package com.github.connteam.conn.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.crypto.SecretKey;

import com.github.connteam.conn.client.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.Sanitization;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.AuthenticationException;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus;
import com.github.connteam.conn.core.net.proto.NetProtos.KeepAlive;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerRecv;
import com.github.connteam.conn.core.net.proto.NetProtos.PeerSend;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysDemand;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.github.connteam.conn.core.net.proto.PeerProtos.TextMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnClient implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnClient.class);

    private final NetChannel channel;
    private final DataProvider database;
    private ScheduledExecutorService scheduler;

    private final SettingsEntry settings;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final KeyPair keyPair;

    private volatile ConnClientListener listener;
    private volatile State state = State.CREATED;

    private final AtomicInteger requestCounter = new AtomicInteger();
    private final Map<Integer, Consumer<UserEntry>> userInfoRequests = new ConcurrentHashMap<>();
    private final Map<Integer, Transmission> transmissions = new ConcurrentHashMap<>();

    private static enum State {
        CREATED, AUTH_REQUEST, AUTH_RESPONSE, ESTABLISHED, CLOSED
    }

    private static class Transmission {
        final UserEntry user;
        final byte[] message;
        boolean waitingForAck = false;

        public Transmission(UserEntry user, byte[] msg) {
            this.user = user;
            message = msg;
        }
    }

    public static class Builder {
        private String host;
        private Integer port;
        private Transport transport;
        private DataProvider database;

        private Builder() {
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setTransport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder setIdentity(DataProvider database) {
            this.database = database;
            return this;
        }

        public ConnClient build() throws IOException, DatabaseException, InvalidKeySpecException {
            if (host == null || port == null || transport == null || database == null) {
                throw new IllegalStateException("Missing builder parameters");
            }
            return new ConnClient(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private ConnClient(Builder builder) throws IOException, DatabaseException, InvalidKeySpecException {
        NetChannel.Provider provider;

        database = builder.database;
        settings = database.getSettings()
                .orElseThrow(() -> new IllegalArgumentException("Settings missing from identity file"));

        publicKey = settings.getPublicKey();
        privateKey = settings.getPrivateKey();
        keyPair = new KeyPair(publicKey, privateKey);

        switch (builder.transport) {
        case TCP:
            provider = StandardNetChannel.connectTCP(builder.host, builder.port);
            break;
        case SSL:
            provider = StandardNetChannel.connectSSL(builder.host, builder.port);
            break;
        default:
            throw new IllegalArgumentException("Unsupported transport layer");
        }

        channel = provider.create(NetMessages.CLIENTBOUND, NetMessages.SERVERBOUND);
        channel.setCloseHandler(this::onClose);
        channel.setTimeout(30000);
    }

    public ConnClientListener getHandler() {
        return listener;
    }

    public void setHandler(ConnClientListener listener) {
        this.listener = listener;
    }

    public SettingsEntry getSettings() {
        return settings;
    }

    @Override
    public void close() {
        close(null);
    }

    protected void close(Exception err) {
        channel.close(err);
    }

    public synchronized void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse NetClient");
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        state = State.AUTH_REQUEST;
        channel.setMessageHandler(this::onAuthRequest);
        channel.open();
    }

    private synchronized void onClose(Exception err) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        state = State.CLOSED;
        listener.onDisconnect(err);
    }

    private synchronized void onAuthRequest(Message msg) {
        if (!(msg instanceof AuthRequest)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthRequest request = (AuthRequest) msg;
        byte[] payload = request.getPayload().toByteArray();

        if (payload.length == 0) {
            close(new ProtocolException("Server didn't provide payload to sign"));
            return;
        }

        try {
            login(payload);
        } catch (InvalidKeyException | SignatureException e) {
            close(e);
        }
    }

    private synchronized void onAuthStatus(Message msg) {
        if (!(msg instanceof AuthStatus)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthStatus resp = (AuthStatus) msg;

        switch (resp.getStatus()) {
        case LOGGED_IN:
            onLogin(false);
            break;
        case REGISTERED:
            onLogin(true);
            break;
        default:
            close(new AuthenticationException(resp.getStatus()));
        }
    }

    private synchronized void login(byte[] toSign) throws InvalidKeyException, SignatureException {
        String name = settings.getUsername();

        AuthResponse.Builder response = AuthResponse.newBuilder();
        response.setUsername(name);
        response.setSignature(ByteString.copyFrom(ClientUtil.makeLoginSignature(keyPair, name, toSign)));
        response.setPublicKey(ByteString.copyFrom(publicKey.getEncoded()));

        channel.sendMessage(response.build());

        state = State.AUTH_RESPONSE;
        channel.setMessageHandler(this::onAuthStatus);
    }

    private synchronized void onLogin(boolean hasBeenRegistered) {
        state = State.ESTABLISHED;
        channel.setMessageHandler(new MessageHandler());
        listener.onLogin(hasBeenRegistered);

        final KeepAlive keepAlive = KeepAlive.newBuilder().build();

        if (!scheduler.isShutdown()) {
            scheduler.scheduleWithFixedDelay(() -> {
                channel.sendMessage(keepAlive);
            }, 20, 20, TimeUnit.SECONDS);
        }
    }

    private void onPeerMessage(UserEntry from, Message msg) {
        if (msg instanceof TextMessage) {
            TextMessage txt = (TextMessage) msg;
            listener.onTextMessage(from, txt.getMessage());
        }
    }

    private void sendPeerMessage(UserEntry to, Message msg) {
        Transmission trans = new Transmission(to, ClientUtil.encodePeerMessage(msg));
        int id = requestCounter.incrementAndGet();

        transmissions.put(id, trans);

        TransmissionRequest.Builder req = TransmissionRequest.newBuilder();
        req.setTransmissionID(id);
        req.setUsername(to.getUsername());
        channel.sendMessage(req.build());
    }

    public void sendTextMessage(UserEntry to, String message) {
        sendPeerMessage(to, TextMessage.newBuilder().setMessage(message).build());
    }

    public void getUserInfo(String username, Consumer<UserEntry> callback) throws DatabaseException {
        UserEntry user = database.getUserByUsername(username).orElse(null);

        if (user != null || !Sanitization.isValidUsername(username)) {
            callback.accept(user);
            return;
        }

        int i = requestCounter.incrementAndGet();
        userInfoRequests.put(i, callback);

        UserInfoRequest.Builder request = UserInfoRequest.newBuilder();
        request.setRequestID(i);
        request.setUsername(username);
        channel.sendMessage(request.build());
    }

    public class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void onUserInfo(UserInfo msg) {
            Consumer<UserEntry> callback = userInfoRequests.remove(msg.getRequestID());
            if (callback == null) {
                close(new ProtocolException("UserInfo received with invalid requestID"));
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
                database.insertUser(user);
            } catch (DatabaseException e) {
                close(e);
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
                    key.setId(database.insertEphemeralKey(key));

                    Signature sign = CryptoUtil.newSignature(privateKey);
                    sign.update(key.getRawPublicKey());

                    SignedKey.Builder signed = SignedKey.newBuilder();
                    signed.setPublicKey(ByteString.copyFrom(key.getRawPublicKey()));
                    signed.setSignature(ByteString.copyFrom(sign.sign()));
                    out.addKeys(signed);
                }

                channel.sendMessage(out.build());
            } catch (DatabaseException | InvalidKeyException | SignatureException e) {
                close(e);
            }
        }

        @HandleEvent
        public void onTransmissionResponse(TransmissionResponse msg) {
            int id = msg.getTransmissionID();
            Transmission trans = transmissions.remove(id);

            if (trans == null || trans.waitingForAck) {
                close(new ProtocolException("TransmissionResponse received with invalid ID"));
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
                    close(new ProtocolException("Invalid ephemeral key received"));
                    return;
                }

                // Generate local ECDH part

                KeyPair localKey = CryptoUtil.generateKeyPair();
                SecretKey sharedKey = CryptoUtil.getSharedSecret(localKey.getPrivate(), remoteKey);

                // Encrypt and sign message

                byte[] encrypted = CryptoUtil.encryptSymmetric(sharedKey, trans.message);
                byte[] partialKey2 = localKey.getPublic().getEncoded();

                Signature sign = CryptoUtil.newSignature(privateKey);
                sign.update(encrypted);
                sign.update(remoteKey.getEncoded());
                sign.update(partialKey2);

                // Send

                PeerSend.Builder out = PeerSend.newBuilder();
                out.setTransmissionID(id);
                out.setEncryptedMessage(ByteString.copyFrom(encrypted));
                out.setPartialKey2(ByteString.copyFrom(partialKey2));
                out.setSignature(ByteString.copyFrom(sign.sign()));

                channel.sendMessage(out.build());
            } catch (InvalidKeySpecException | InvalidKeyException | SignatureException e) {
                close(e);
            }
        }

        @HandleEvent
        public void onPeerRecv(PeerRecv msg) {
            try {
                getUserInfo(msg.getUsername(), other -> {
                    if (other == null) {
                        close(new ProtocolException("Received peer message from unknown user"));
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

                        EphemeralKeyEntry localKey = database.getEphemeralKeyByPublicKey(partialKey1).orElse(null);
                        if (localKey == null) {
                            LOG.warn("Peer message using unknown ephemeral key");
                            return;
                        }

                        database.deleteEphemeralKey(localKey.getId());

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
                        close(e);
                    }
                });
            } catch (DatabaseException e) {
                close(e);
            }
        }
    }
}
