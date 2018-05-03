package com.github.connteam.conn.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;

import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.core.net.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.NetProtos.AuthSuccess;
import com.google.protobuf.Message;

public class ConnClient implements Closeable {
    private final NetChannel channel;
    private volatile ConnClientListener listener;
    private volatile State state = State.CREATED;

    private static enum State {
        CREATED, AUTH_REQUEST, AUTH_RESPONSE, ESTABLISHED, CLOSED
    }

    public static class Builder {
        private String host;
        private Integer port;
        private Transport transport;

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

        public ConnClient build() throws IOException {
            if (host == null || port == null || transport == null) {
                throw new IllegalStateException();
            }
            return new ConnClient(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private ConnClient(Builder builder) throws IOException {
        NetChannel.Provider provider;

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

    protected void close(IOException err) {
        channel.close(err);
    }

    public synchronized void start() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse NetClient");
        }

        state = State.AUTH_REQUEST;
        channel.setMessageHandler(this::onAuthRequest);
        channel.open();
    }

    private synchronized void onClose(IOException err) {
        state = State.CLOSED;
        listener.onDisconnect(err);
    }

    private synchronized void onAuthRequest(Message msg) {
        if (msg instanceof AuthRequest) {
            state = State.AUTH_RESPONSE;
            channel.setMessageHandler(this::onAuthSuccess);
            channel.sendMessage(AuthResponse.newBuilder().setUsername("admin123").build());
        } else {
            close(new ProtocolException("Unexpected message on authentication stage"));
        }
    }

    private synchronized void onAuthSuccess(Message msg) {
        if (msg instanceof AuthSuccess) {
            state = State.ESTABLISHED;
            channel.setMessageHandler(new MessageHandler());
            listener.onLogin();
        } else {
            close(new ProtocolException("Unexpected message on authentication stage"));
        }
    }

    private class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void logMessages(Message msg) {
            System.out.println(msg.getClass().getSimpleName());
            System.out.println(msg.toString());
        }
    }
}
