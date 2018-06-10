package com.github.connteam.conn.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.Sanitization;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.AuthenticationException;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus;
import com.github.connteam.conn.core.net.proto.NetProtos.KeepAlive;
import com.github.connteam.conn.core.net.proto.NetProtos.ObserveUsers;
import com.github.connteam.conn.core.net.proto.NetProtos.SetStatus;
import com.github.connteam.conn.core.net.proto.NetProtos.TransmissionRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.UserStatus;
import com.github.connteam.conn.core.net.proto.PeerProtos.TextMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class ConnClient implements Closeable {
    private final NetChannel channel;
    private final DataProvider database;
    private ScheduledExecutorService scheduler;

    private final SettingsEntry settings;
    private final KeyPair keyPair;

    private volatile ConnClientListener listener;
    private volatile State state = State.CREATED;

    private final AtomicInteger requestCounter = new AtomicInteger();
    private final Map<Integer, Consumer<UserEntry>> userInfoRequests = new ConcurrentHashMap<>();
    private final Map<Integer, Transmission> transmissions = new ConcurrentHashMap<>();

    private volatile UserStatus userStatus = UserStatus.DISCONNECTED;

    private static enum State {
        CREATED, AUTH_REQUEST, AUTH_RESPONSE, ESTABLISHED, CLOSED
    }

    protected static class Transmission {
        final UserEntry user;
        final byte[] message;
        Consumer<Exception> callback;
        boolean waitingForAck = false;

        public Transmission(UserEntry user, byte[] msg, Consumer<Exception> callback) {
            this.user = user;
            this.callback = (callback != null ? callback : x -> {
            });
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

        keyPair = new KeyPair(settings.getPublicKey(), settings.getPrivateKey());

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

    public DataProvider getDataProvider() {
        return database;
    }

    public NetChannel getNetChannel() {
        return channel;
    }

    public KeyPair getKeyPair() {
        return keyPair;
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

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public synchronized void setUserStatus(UserStatus status) {
        if (userStatus == status) {
            return;
        }

        userStatus = status;
        channel.sendMessage(SetStatus.newBuilder().setStatus(status).build());
    }

    protected Map<Integer, Consumer<UserEntry>> getUserInfoRequests() {
        return userInfoRequests;
    }

    protected Map<Integer, Transmission> getTransmissions() {
        return transmissions;
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

    private synchronized void onLogin(boolean hasBeenRegistered) {
        state = State.ESTABLISHED;
        channel.setMessageHandler(new ClientMessageHandler(this));

        try {
            observeEveryone();
        } catch (DatabaseException e) {
            close(e);
            return;
        }

        listener.onLogin(hasBeenRegistered);

        final KeepAlive keepAlive = KeepAlive.newBuilder().build();

        if (!scheduler.isShutdown()) {
            scheduler.scheduleWithFixedDelay(() -> {
                channel.sendMessage(keepAlive);
            }, 20, 20, TimeUnit.SECONDS);
        }
    }

    private synchronized void login(byte[] toSign) throws InvalidKeyException, SignatureException {
        String name = settings.getUsername();

        AuthResponse.Builder response = AuthResponse.newBuilder();
        response.setUsername(name);
        response.setSignature(ByteString.copyFrom(ClientUtil.makeLoginSignature(keyPair, name, toSign)));
        response.setPublicKey(ByteString.copyFrom(keyPair.getPublic().getEncoded()));

        channel.sendMessage(response.build());

        state = State.AUTH_RESPONSE;
        channel.setMessageHandler(this::onAuthStatus);
    }

    protected void sendPeerMessage(UserEntry to, Message msg, Consumer<Exception> callback) {
        Transmission trans = new Transmission(to, ClientUtil.encodePeerMessage(msg), callback);
        int id = requestCounter.incrementAndGet();

        transmissions.put(id, trans);

        TransmissionRequest.Builder req = TransmissionRequest.newBuilder();
        req.setTransmissionID(id);
        req.setUsername(to.getUsername());
        channel.sendMessage(req.build());
    }

    public void sendTextMessage(UserEntry to, String message, BiConsumer<MessageEntry, Exception> callback) {
        sendPeerMessage(to, TextMessage.newBuilder().setMessage(message).build(), err -> {
            MessageEntry entry = null;

            if (err == null) {
                // Save message in archive
                entry = new MessageEntry();
                entry.setIdUser(to.getId());
                entry.setOutgoing(true);
                entry.setMessage(message);

                try {
                    entry.setIdMessage(database.insertMessage(entry));
                } catch (DatabaseException e) {
                    close(e);
                }
            }

            if (callback != null) {
                callback.accept(entry, err);
            }
        });
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

    public void observe(UserEntry user) {
        ObserveUsers.Builder msg = ObserveUsers.newBuilder();
        msg.addAdded(user.getUsername());
        channel.sendMessage(msg.build());
    }

    private void observeEveryone() throws DatabaseException {
        ObserveUsers.Builder msg = ObserveUsers.newBuilder();
        for (UserEntry user : database.getUsers()) {
            msg.addAdded(user.getUsername());
        }
        channel.sendMessage(msg.build());
    }
}
