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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.client.database.provider.DataProvider;
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
        scheduler.shutdownNow();
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
            Signature sign = CryptoUtil.newSignature(privateKey);
            sign.update(settings.getUsername().getBytes());
            sign.update(publicKey.getEncoded());
            sign.update(payload);
            byte[] signature = sign.sign();

            channel.sendMessage(AuthResponse.newBuilder().setUsername(settings.getUsername())
                    .setSignature(ByteString.copyFrom(signature)).build());
            
            state = State.AUTH_RESPONSE;
            channel.setMessageHandler(this::onAuthSuccess);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            close(e);
        }
    }

    private synchronized void onAuthSuccess(Message msg) {
        if (!(msg instanceof AuthStatus)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthStatus resp = (AuthStatus)msg;

        switch (resp.getStatus()) {
        case SUCCESS:
            state = State.ESTABLISHED;
            channel.setMessageHandler(new MessageHandler());
            startKeepAlive();
            listener.onLogin(true);
            break;
        case FAILED:
            close(new AuthenticationException("Authentication failed"));
            listener.onLogin(false);
            break;
        default:
            close(new ProtocolException("Internal server error"));
            break;
        }
    }

    private void startKeepAlive() {
        final KeepAlive keepAlive = KeepAlive.newBuilder().build();

        scheduler.scheduleWithFixedDelay(() -> {
            channel.sendMessage(keepAlive);
        }, 20, 20, TimeUnit.SECONDS);
    }

    public class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void logMessages(Message msg) {
        }
    }
}
