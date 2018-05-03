package com.github.connteam.conn.server;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServerClient implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServerClient.class);

    private final ConnServer server;
    private final NetChannel channel;
    private volatile State state = State.CREATED;

    private static enum State {
        CREATED, AUTHENTICATION, ESTABLISHED, CLOSED
    }

    public ConnServerClient(ConnServer server, NetChannel.Provider channelProvider) throws IOException {
        this.server = server;
        channel = channelProvider.create(NetMessages.SERVERBOUND, NetMessages.CLIENTBOUND);
        channel.setCloseHandler(this::onClose);
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
            throw new IllegalStateException("Cannot reuse ConnServerClient");
        }

        state = State.AUTHENTICATION;
        channel.setMessageHandler(this::onAuthResponse);
        channel.open();
        channel.sendMessage(AuthRequest.newBuilder().build());
    }

    private synchronized void onClose(IOException err) {
        state = State.CLOSED;
        server.removeClient(this);
    }

    private synchronized void onAuthResponse(Message msg) {
        if (msg instanceof AuthResponse) {
            AuthResponse response = (AuthResponse)msg;

            LOG.info("Username: {}", response.getUsername());

            state = State.ESTABLISHED;
            channel.setMessageHandler(new MessageHandler());
            channel.sendMessage(AuthSuccess.newBuilder().build());
            server.addClient(this);
        } else {
            close(new ProtocolException("Unexpected message on authentication stage"));
        }
    }

    private class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void logMessages(Message msg) {
        }
    }
}
