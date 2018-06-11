package com.github.connteam.conn.client;

import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Consumer;

import javax.crypto.SecretKey;

import com.github.connteam.conn.client.ConnClient.Transmission;
import com.github.connteam.conn.client.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UsedEphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.crypto.SharedSecretGenerator;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysDemand;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecv;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecvAck;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSend;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSendAck;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserNotification;
import com.github.connteam.conn.core.net.proto.PeerProtos.TextMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMessageHandler extends MultiEventListener<Message> {
    private final static Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private final ConnClient client;

    protected ClientMessageHandler(ConnClient client) {
        this.client = client;
    }

    public ConnClientListener getHandler() {
        return client.getHandler();
    }

    public DataProvider getDataProvider() {
        return client.getDataProvider();
    }

    public NetChannel getNetChannel() {
        return client.getNetChannel();
    }

    public KeyPair getKeyPair() {
        return client.getKeyPair();
    }

    @HandleEvent
    public void onUserInfo(UserInfo msg) {
        Consumer<UserEntry> callback = client.getUserInfoRequests().remove(msg.getRequestID());
        if (callback == null) {
            client.close(new ProtocolException("UserInfo received with invalid requestID"));
            return;
        }

        if (!msg.getExists()) {
            callback.accept(null);
            return;
        }

        UserEntry user = new UserEntry();
        user.setUsername(msg.getUsername());
        user.setPublicKey(msg.getPublicKey().toByteArray());

        try {
            user.setId(getDataProvider().insertUser(user));
        } catch (DatabaseException e) {
            try {
                // Check if not fetched in the meantime
                user = getDataProvider().getUserByUsername(user.getUsername()).orElse(null);
                if (user != null) {
                    callback.accept(user);
                    return;
                }
            } catch (DatabaseException e2) {
                // Prioritize original error
            }

            client.close(e);
            return;
        }

        client.observe(user);
        callback.accept(user);
    }

    @HandleEvent
    public void onEphemeralKeysDemand(EphemeralKeysDemand msg) {
        try {
            EphemeralKeysUpload.Builder out = EphemeralKeysUpload.newBuilder();

            for (int i = 0; i < msg.getCount(); i++) {
                EphemeralKeyEntry key = ClientUtil.generateEphemeralKey();
                key.setId(getDataProvider().insertEphemeralKey(key));

                Signature sign = CryptoUtil.newSignature(getKeyPair().getPrivate());
                sign.update(key.getRawPublicKey());

                SignedKey.Builder signed = SignedKey.newBuilder();
                signed.setPublicKey(ByteString.copyFrom(key.getRawPublicKey()));
                signed.setSignature(ByteString.copyFrom(sign.sign()));
                out.addKeys(signed);
            }

            getNetChannel().sendMessage(out.build());
        } catch (DatabaseException | InvalidKeyException | SignatureException e) {
            client.close(e);
        }
    }

    private boolean checkEphemeralKeyUsage(byte[] key) throws DatabaseException {
        UsedEphemeralKeyEntry entry = new UsedEphemeralKeyEntry(key);

        if (!getDataProvider().isUsedEphemeralKey(entry)) {
            getDataProvider().insertUsedEphemeralKey(entry);
            return true;
        }
        return false;
    }

    @HandleEvent
    public void onTransmissionResponse(TransmissionResponse msg) {
        int id = msg.getTransmissionID();
        Transmission trans = client.getTransmissions().get(id);
        Exception err = new ProtocolException("Unknown delivery error");

        try {
            if (trans == null || trans.waitingForAck) {
                err = new ProtocolException("TransmissionResponse received with invalid ID");
                client.close(err);
                return;
            }

            if (!msg.getSuccess()) {
                err = new ProtocolException("Prekeys exhausted");
                return;
            }

            // Verify prekey signature

            PublicKey remoteKey = CryptoUtil.verifyEphemeralKey(msg.getPartialKey1(), trans.user.getPublicKey());
            if (remoteKey == null) {
                err = new ProtocolException("Invalid ephemeral key received");
                return;
            }

            // Check if key wasn't used before

            if (!checkEphemeralKeyUsage(remoteKey.getEncoded())) {
                err = new ProtocolException("Received already used ephemeral key");
                return;
            }

            // Generate local ECDH part

            KeyPair localKey = CryptoUtil.generateKeyPair();
            SecretKey sharedKey = new SharedSecretGenerator().setPublic(remoteKey).setPrivate(localKey.getPrivate())
                    .add(remoteKey.getEncoded()).add(localKey.getPublic().getEncoded()).build();

            // Encrypt and sign message

            byte[] encrypted = CryptoUtil.encryptSymmetric(sharedKey, trans.message);
            byte[] partialKey2 = localKey.getPublic().getEncoded();

            Signature sign = CryptoUtil.newSignature(getKeyPair().getPrivate());
            sign.update(encrypted);
            sign.update(remoteKey.getEncoded());
            sign.update(partialKey2);

            // Send

            PeerSend.Builder out = PeerSend.newBuilder();
            out.setTransmissionID(id);
            out.setEncryptedMessage(ByteString.copyFrom(encrypted));
            out.setPartialKey2(ByteString.copyFrom(partialKey2));
            out.setSignature(ByteString.copyFrom(sign.sign()));

            getNetChannel().sendMessage(out.build());
            err = null;
            trans.waitingForAck = true;
        } catch (InvalidKeySpecException | InvalidKeyException | SignatureException | DatabaseException e) {
            client.close(e);
            err = e;
        } finally {
            if (err != null && trans != null) {
                LOG.warn(err.getMessage());
                client.getTransmissions().remove(id);
                trans.callback.accept(err);
            }
        }
    }

    @HandleEvent
    public void onPeerSendAck(PeerSendAck msg) {
        Transmission trans = client.getTransmissions().remove(msg.getTransmissionID());

        if (trans == null) {
            client.close(new ProtocolException("PeerSendAck received with invalid ID"));
        } else {
            trans.callback.accept(null);
        }
    }

    @HandleEvent
    public void onPeerRecv(PeerRecv msg) {
        try {
            client.getUserInfo(msg.getUsername(), other -> {
                if (other == null) {
                    client.close(new ProtocolException("Received peer message from unknown user"));
                    return;
                }

                byte[] encrypted = msg.getEncryptedMessage().toByteArray();
                byte[] partialKey1 = msg.getPartialKey1().toByteArray();
                byte[] partialKey2 = msg.getPartialKey2().toByteArray();
                byte[] signature = msg.getSignature().toByteArray();

                try {
                    // Verify signature

                    Signature sign = CryptoUtil.newSignature(other.getPublicKey());
                    sign.update(encrypted);
                    sign.update(partialKey1);
                    sign.update(partialKey2);

                    if (!sign.verify(signature)) {
                        LOG.warn("Invalid peer message signature");
                        return;
                    }

                    // Check if keys weren't used before

                    if (!checkEphemeralKeyUsage(partialKey1) || !checkEphemeralKeyUsage(partialKey2)) {
                        LOG.warn("Received already used ephemeral key");
                        return;
                    }

                    // Lookup for local part of ephemeral key

                    EphemeralKeyEntry localKey = getDataProvider().getEphemeralKeyByPublicKey(partialKey1).orElse(null);
                    if (localKey == null) {
                        LOG.warn("Peer message using unknown ephemeral key");
                        return;
                    }

                    getDataProvider().deleteEphemeralKey(localKey.getId());

                    // Decrypt and handle message

                    PublicKey remoteKey = CryptoUtil.decodePublicKey(partialKey2);
                    SecretKey sharedKey = new SharedSecretGenerator().setPublic(remoteKey)
                            .setPrivate(localKey.getPrivateKey()).add(partialKey1).add(partialKey2).build();

                    byte[] data = CryptoUtil.decryptSymmetric(sharedKey, encrypted);
                    Message decoded = ClientUtil.decodePeerMessage(data);

                    if (decoded == null) {
                        LOG.warn("Failed to decode peer message");
                        return;
                    }

                    onPeerMessage(other, decoded);

                    // Send acknowledge

                    PeerRecvAck.Builder ack = PeerRecvAck.newBuilder();
                    ack.setTransmissionID(msg.getTransmissionID());
                    getNetChannel().sendMessage(ack.build());
                } catch (SignatureException | InvalidKeyException | InvalidKeySpecException e) {
                    LOG.warn("Invalid peer message", e);
                } catch (DatabaseException e) {
                    client.close(e);
                }
            });
        } catch (DatabaseException e) {
            client.close(e);
        }
    }

    private void onPeerMessage(UserEntry from, Message msg) {
        if (!(msg instanceof TextMessage)) {
            return;
        }

        TextMessage txt = (TextMessage) msg;

        // Save message in archive

        MessageEntry entry = new MessageEntry();
        entry.setIdUser(from.getId());
        entry.setOutgoing(false);
        entry.setMessage(txt.getMessage());

        try {
            entry.setIdMessage(getDataProvider().insertMessage(entry));
            getHandler().onTextMessage(from, entry);
        } catch (DatabaseException e) {
            client.close(e);
        }
    }

    @HandleEvent
    public void onUserNotification(UserNotification msg) {
        try {
            client.getUserInfo(msg.getUsername(), user -> {
                client.getHandler().onStatusChange(user, msg.getStatus());
            });
        } catch (DatabaseException e) {
            client.close(e);
        }
    }
}