package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.events.HandleEvent;
import com.github.connteam.conn.core.events.MultiEventListener;
import com.github.connteam.conn.core.net.AuthenticationException;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus;
import com.github.connteam.conn.server.database.model.User;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class ConnServerClient implements Closeable {
    private final ConnServer server;
    private final NetChannel channel;

    private volatile byte[] authPayload;
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

    protected void close(Exception err) {
        channel.close(err);
    }

    public synchronized void handle() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse ConnServerClient");
        }

        authPayload = CryptoUtil.randomBytes(64);

        state = State.AUTHENTICATION;
        channel.setMessageHandler(this::onAuthResponse);
        channel.open();
        channel.sendMessage(AuthRequest.newBuilder().setPayload(ByteString.copyFrom(authPayload)).build());
    }

    private synchronized void onClose(Exception err) {
        state = State.CLOSED;
        server.removeClient(this);
    }

    private synchronized void onAuthResponse(Message msg) {
        if (!(msg instanceof AuthResponse)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthResponse response = (AuthResponse)msg;
        User user = null;
        boolean success = false;

        try {
            user = server.getDataProvider().getUserByUsername(response.getUsername()).get();
            if (user != null) {
                Signature sign = CryptoUtil.newSignature(user.getPublicKey());
                sign.update(authPayload);
                success = sign.verify(response.getSignature().toByteArray());
            }
        } catch (DatabaseException | InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
                | SignatureException e) {
            channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.INTERNAL_ERROR).build());
            close(e);
            return;
        }

        if (!success) {
            channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.FAILED).build());
            close(new AuthenticationException("Authentication failed"));
            return;
        }

        server.addClient(this);

        state = State.ESTABLISHED;
        channel.setMessageHandler(new MessageHandler());
        channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.SUCCESS).build());
    }

    private class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void logMessages(Message msg) {
        }
    }
}
