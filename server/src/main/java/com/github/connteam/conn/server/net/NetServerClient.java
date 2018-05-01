package com.github.connteam.conn.server.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;

import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.NetProtos.AuthSuccess;
import com.google.protobuf.Message;

public class NetServerClient implements Closeable {
    private static enum State {
        CREATED, AUTHENTICATION, ESTABLISHED, CLOSED
    }

    private final NetChannel channel;
    private volatile NetServerClientHandler listener;
    private volatile State state = State.CREATED;

    public NetServerClient(NetChannel.Provider channelProvider) throws IOException {
        channel = channelProvider.create(NetMessages.SERVERBOUND, NetMessages.CLIENTBOUND);
        channel.setCloseHandler(this::onClose);
    }

    public NetServerClientHandler getHandler() {
        return listener;
    }

    public void setHandler(NetServerClientHandler listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
        close(null);
    }

    protected void close(IOException err) {
        channel.close(err);
    }

    public synchronized void handle() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse NetServerClient");
        }

        state = State.AUTHENTICATION;
        channel.setMessageHandler(this::onAuthResponse);
        channel.open();
        channel.sendMessage(AuthRequest.newBuilder().build());
    }

    private synchronized void onClose(IOException err) {
        state = State.CLOSED;
        listener.onDisconnect(err);
    }

    private synchronized void onAuthResponse(Message msg) {
        if (msg instanceof AuthResponse) {
            AuthResponse response = (AuthResponse)msg;

            if (listener.onLogin(response.getUsername())) {
                state = State.ESTABLISHED;
                channel.setMessageHandler(new MessageHandler());
                channel.sendMessage(AuthSuccess.newBuilder().build());
            } else {
                close(new IOException("Authentication failed"));
            }
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
