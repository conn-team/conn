package com.github.connteam.conn.client.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;

import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.NetProtos.AuthSuccess;
import com.google.protobuf.Message;

public class NetClient implements Closeable {
    private static enum State {
        CREATED, AUTH_REQUEST, AUTH_RESPONSE, ESTABLISHED, CLOSED
    }

    private final NetChannel channel;
    private volatile NetClientHandler listener;
    private volatile State state = State.CREATED;

    private String username;

    public NetClient(NetChannel.Provider channelProvider) throws IOException {
        channel = channelProvider.create(NetMessages.CLIENTBOUND, NetMessages.SERVERBOUND);
        channel.setCloseHandler(this::onClose);
    }

    public static NetClient connect(String host, int port) throws IOException {
        return new NetClient(StandardNetChannel.connectSSL(host, port));
    }

    public NetClientHandler getHandler() {
        return listener;
    }

    public void setHandler(NetClientHandler listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        close(null);
    }

    protected void close(IOException err) {
        channel.close(err);
    }

    public synchronized void login(String username) {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse NetClient");
        }

        this.username = username;
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
            channel.sendMessage(AuthResponse.newBuilder().setUsername(username).build());
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
