package com.github.connteam.conn.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.client.database.model.User;
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
import com.github.connteam.conn.core.net.proto.NetProtos.TextMessage;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class ConnClient implements Closeable {
    private final NetChannel channel;
    private final DataProvider database;
    private ScheduledExecutorService scheduler;

    private final Settings settings;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    private volatile ConnClientListener listener;
    private volatile State state = State.CREATED;

    private final AtomicInteger requestCounter = new AtomicInteger();
    private final Map<Integer, Consumer<User>> userInfoRequests = new ConcurrentHashMap<>();

    private static enum State {
        CREATED, AUTH_REQUEST, AUTH_RESPONSE, ESTABLISHED, CLOSED
    }

    public static class Builder {
        private String host;
        private Integer port;
        private Transport transport;
        private DataProvider database;

        private Builder() {}

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

    public Settings getSettings() {
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

        AuthRequest request = (AuthRequest)msg;
        byte[] payload = request.getPayload().toByteArray();

        if (payload.length == 0) {
            close(new ProtocolException("Server didn't provide payload to sign"));
            return;
        }

        try {
            login(payload);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            close(e);
        }
    }

    private synchronized void onAuthStatus(Message msg) {
        if (!(msg instanceof AuthStatus)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthStatus resp = (AuthStatus)msg;

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

    private synchronized void login(byte[] toSign) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        String name = settings.getUsername();
        byte[] pubKey = publicKey.getEncoded();
        
        Signature sign = CryptoUtil.newSignature(privateKey);
        sign.update(name.getBytes());
        sign.update(pubKey);
        sign.update(toSign);

        AuthResponse.Builder response = AuthResponse.newBuilder();
        response.setUsername(name);
        response.setSignature(ByteString.copyFrom(sign.sign()));
        response.setPublicKey(ByteString.copyFrom(pubKey));

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

    public void sendTextMessage(String to, String message) {
        channel.sendMessage(TextMessage.newBuilder().setUsername(to).setMessage(message).build());
    }

    public void getUserInfo(String username, Consumer<User> callback) throws DatabaseException {
        User user = database.getUserByUsername(username).orElse(null);

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
        public void onTextMessage(TextMessage msg) {
            listener.onTextMessage(msg.getUsername(), msg.getMessage());
        }

        @HandleEvent
        public void onUserInfo(UserInfo msg) {
            Consumer<User> callback = userInfoRequests.remove(msg.getRequestID());
            if (callback == null) {
                close(new ProtocolException("UserInfo received with invalid requestID"));
                return;
            }

            if (!msg.getExists()) {
                callback.accept(null);
                return;
            }

            User user = new User();
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
    }
}
