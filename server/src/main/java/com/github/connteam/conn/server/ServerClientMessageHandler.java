package com.github.connteam.conn.server;

import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.KeepAlive;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecvAck;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSend;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSendAck;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.github.connteam.conn.server.ConnServerClient.Transmission;
import com.github.connteam.conn.server.database.model.MessageEntry;
import com.github.connteam.conn.server.database.model.UserEntry;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class ServerClientMessageHandler extends MultiEventListener<Message> {
    private final ConnServerClient client;

    protected ServerClientMessageHandler(ConnServerClient client) {
        this.client = client;
    }

    public ConnServer getServer() {
        return client.getServer();
    }

    public DataProvider getDataProvider() {
        return client.getDataProvider();
    }

    public NetChannel getNetChannel() {
        return client.getNetChannel();
    }

    public UserEntry getUser() {
        return client.getUser();
    }

    public PublicKey getPublicKey() {
        return client.getPublicKey();
    }

    @HandleEvent
    public void onKeepAlive(KeepAlive msg) {
        getNetChannel().sendMessage(msg);
    }

    @HandleEvent
    public void onEphemeralKeysUpload(EphemeralKeysUpload msg) {
        synchronized (client) {
            for (SignedKey key : msg.getKeysList()) {
                if (!client.addEphemeralKey(key)) {
                    return;
                }
            }
        }
    }

    @HandleEvent
    public void onUserInfoRequest(UserInfoRequest msg) {
        String username = msg.getUsername();
        UserEntry user;

        try {
            user = getDataProvider().getUserByUsername(username).orElse(null);
        } catch (DatabaseException e) {
            client.close(e);
            return;
        }

        UserInfo.Builder resp = UserInfo.newBuilder();
        resp.setRequestID(msg.getRequestID());
        resp.setExists(user != null);

        if (user != null) {
            resp.setUsername(user.getUsername());
            resp.setPublicKey(ByteString.copyFrom(user.getRawPublicKey()));
        }

        getNetChannel().sendMessage(resp.build());
    }

    @HandleEvent
    public void onTransmissionRequest(TransmissionRequest msg) {
        int id = msg.getTransmissionID();

        if (client.getTransmissions().containsKey(id)) {
            client.close(new ProtocolException("Unexpected transmission ID"));
            return;
        }

        try {
            UserEntry other = getDataProvider().getUserByUsername(msg.getUsername()).orElse(null);

            TransmissionResponse.Builder resp = TransmissionResponse.newBuilder();
            resp.setTransmissionID(msg.getTransmissionID());
            resp.setSuccess(false);

            if (other != null) {
                getDataProvider().popEphemeralKeyByUserId(other.getIdUser()).ifPresent(key -> {
                    SignedKey.Builder elem = SignedKey.newBuilder();
                    elem.setPublicKey(ByteString.copyFrom(key.getRawKey()));
                    elem.setSignature(ByteString.copyFrom(key.getSignature()));

                    SignedKey partial = elem.build();
                    resp.setPartialKey1(partial);
                    resp.setSuccess(true);

                    client.getTransmissions().put(id, new Transmission(other, partial));
                });

                ConnServerClient otherClient = getServer().getClientByName(other.getUsername());
                if (otherClient != null) {
                    otherClient.onEphemeralKeyDelete();
                }
            }

            getNetChannel().sendMessage(resp.build());
        } catch (DatabaseException e) {
            client.close(e);
        }
    }

    @HandleEvent
    public void onPeerSend(PeerSend msg) {
        Transmission trans = client.getTransmissions().remove(msg.getTransmissionID());

        if (trans == null) {
            client.close(new ProtocolException("Unexpected transmission ID"));
            return;
        }

        byte[] encrypted = msg.getEncryptedMessage().toByteArray();
        byte[] partialKey1 = trans.partialKey1.getPublicKey().toByteArray();
        byte[] partialKey2 = msg.getPartialKey2().toByteArray();
        byte[] signature = msg.getSignature().toByteArray();

        // Verify signature

        try {
            Signature sign = CryptoUtil.newSignature(getUser().getPublicKey());
            sign.update(encrypted);
            sign.update(partialKey1);
            sign.update(partialKey2);

            if (!sign.verify(signature)) {
                client.close(new ProtocolException("Invalid peer message signature"));
                return;
            }
        } catch (InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            client.close(e);
            return;
        }

        // Save message to database

        MessageEntry entry = new MessageEntry();

        entry.setIdFrom(getUser().getIdUser());
        entry.setIdTo(trans.user.getIdUser());
        entry.setMessage(encrypted);
        entry.setPartialKey1(partialKey1);
        entry.setPartialKey2(partialKey2);
        entry.setSignature(signature);

        try {
            entry.setIdMessage(getDataProvider().insertMessage(entry));
        } catch (DatabaseException e) {
            client.close(e);
            return;
        }

        // Send acknowledge

        PeerSendAck.Builder ack = PeerSendAck.newBuilder();
        ack.setTransmissionID(msg.getTransmissionID());
        getNetChannel().sendMessage(ack.build());

        // Trigger inbox check

        ConnServerClient other = getServer().getClientByName(trans.user.getUsername());

        if (other != null) {
            other.checkInbox();
        }
    }

    @HandleEvent
    public void onPeerRecvAck(PeerRecvAck msg) {
        MessageEntry entry = client.getReceivedMessages().remove(msg.getTransmissionID());

        if (entry == null) {
            client.close(new ProtocolException("Unexpected transmission ID"));
            return;
        }

        try {
            getDataProvider().deleteMessage(entry.getIdMessage());
        } catch (DatabaseException e) {
            client.close(e);
        }
    }
}
