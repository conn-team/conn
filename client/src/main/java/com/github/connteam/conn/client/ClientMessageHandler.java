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
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysDemand;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecv;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSend;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.PeerProtos.TextMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientMessageHandler extends MultiEventListener<Message> {
    private final static Logger LOG = LoggerFactory.getLogger(ClientMessageHandler.class);

    private ConnClient client;

    public ClientMessageHandler(ConnClient client) {
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
            client.getDataProvider().insertUser(user);
        } catch (DatabaseException e) {
            client.close(e);
            return;
        }

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

    @HandleEvent
    public void onTransmissionResponse(TransmissionResponse msg) {
        int id = msg.getTransmissionID();
        Transmission trans = client.getTransmissions().remove(id);

        if (trans == null || trans.waitingForAck) {
            client.close(new ProtocolException("TransmissionResponse received with invalid ID"));
            return;
        }

        if (!msg.getSuccess()) {
            // transmissions.remove(id);
            return;
        }

        try {
            // Verify prekey signature

            PublicKey remoteKey = CryptoUtil.verifyEphemeralKey(msg.getPartialKey1(), trans.user.getPublicKey());
            if (remoteKey == null) {
                client.close(new ProtocolException("Invalid ephemeral key received"));
                return;
            }

            // Generate local ECDH part

            KeyPair localKey = CryptoUtil.generateKeyPair();
            SecretKey sharedKey = CryptoUtil.getSharedSecret(localKey.getPrivate(), remoteKey);

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
        } catch (InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            client.close(e);
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

                    // Lookup for local part of ephemeral key

                    EphemeralKeyEntry localKey = getDataProvider().getEphemeralKeyByPublicKey(partialKey1).orElse(null);
                    if (localKey == null) {
                        LOG.warn("Peer message using unknown ephemeral key");
                        return;
                    }

                    getDataProvider().deleteEphemeralKey(localKey.getId());

                    // Decrypt and handle message

                    PublicKey remoteKey = CryptoUtil.decodePublicKey(partialKey2);
                    SecretKey sharedKey = CryptoUtil.getSharedSecret(localKey.getPrivateKey(), remoteKey);

                    byte[] data = CryptoUtil.decryptSymmetric(sharedKey, encrypted);
                    Message decoded = ClientUtil.decodePeerMessage(data);

                    if (decoded != null) {
                        onPeerMessage(other, decoded);
                    }
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
        if (msg instanceof TextMessage) {
            TextMessage txt = (TextMessage) msg;
            getHandler().onTextMessage(from, txt.getMessage());
        }
    }
}