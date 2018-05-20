package com.github.connteam.conn.server;

import java.net.ProtocolException;
import java.security.PublicKey;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.KeepAlive;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecv;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSend;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.github.connteam.conn.server.ConnServerClient.Transmission;
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

        ConnServerClient client = getServer().getClientByName(trans.user.getUsername());

        if (client == null) {
            return; // TODO: Offline messaging
        }

        PeerRecv.Builder out = PeerRecv.newBuilder();

        out.setTransmissionID(0);
        out.setUsername(getUser().getUsername());
        out.setEncryptedMessage(msg.getEncryptedMessage());
        out.setPartialKey1(trans.partialKey1.getPublicKey());
        out.setPartialKey2(msg.getPartialKey2());
        out.setSignature(msg.getSignature());

        client.getNetChannel().sendMessage(out.build());
    }
}