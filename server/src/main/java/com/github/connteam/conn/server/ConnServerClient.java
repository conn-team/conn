package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
import com.github.connteam.conn.core.net.proto.NetProtos.KeepAlive;
import com.github.connteam.conn.core.net.proto.NetProtos.TextMessage;
import com.github.connteam.conn.server.database.model.User;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServerClient implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServerClient.class);

    private final ConnServer server;
    private final NetChannel channel;
    private volatile State state = State.CREATED;

    private volatile User user;
    private volatile PublicKey publicKey;
    private byte[] authPayload;

    private static enum State {
        CREATED, AUTHENTICATION, ESTABLISHED, CLOSED
    }

    public ConnServerClient(ConnServer server, NetChannel.Provider channelProvider) throws IOException {
        this.server = server;
        channel = channelProvider.create(NetMessages.SERVERBOUND, NetMessages.CLIENTBOUND);
        channel.setCloseHandler(this::onClose);
        channel.setTimeout(30000);
    }

    @Override
    public void close() {
        close(null);
    }

    protected void close(Exception err) {
        channel.close(err);
    }

    public User getUser() {
        return user;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public NetChannel getNetChannel() {
        return channel;
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
                success = verifyLogin(user, authPayload, response.getSignature().toByteArray());
            }
            
            if (success) {
                publicKey = user.getPublicKey();
                this.user = user;
            }
        } catch (DatabaseException | NoSuchAlgorithmException e) {
            LOG.error("Error verifying login: {}", e.toString());
            channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.INTERNAL_ERROR).build());
            close(e);
            return;
        } catch (InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            success = false;
        }

        if (!success) {
            channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.FAILED).build());
            close(new AuthenticationException("Authentication failed"));
            return;
        }

        if (!server.addClient(this)) {
            channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.ALREADY_ONLINE).build());
            close(new AuthenticationException("User connected from another location"));
            return;
        }

        state = State.ESTABLISHED;
        channel.setMessageHandler(new MessageHandler());
        channel.sendMessage(AuthStatus.newBuilder().setStatus(AuthStatus.Status.SUCCESS).build());
    }

    private boolean verifyLogin(User user, byte[] toSign, byte[] signature)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException {
        Signature sign = CryptoUtil.newSignature(user.getPublicKey());
        sign.update(user.getUsername().getBytes());
        sign.update(user.getRawPublicKey());
        sign.update(authPayload);
        return sign.verify(signature);
    }

    public class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void onKeepAlive(KeepAlive msg) {
            channel.sendMessage(msg);
        }

        @HandleEvent
        public void onTextMessage(TextMessage msg) {
            ConnServerClient client = server.getClientByName(msg.getUsername());

            if (client != null) {
                client.getNetChannel().sendMessage(TextMessage.newBuilder().setUsername(getUser().getUsername())
                        .setMessage(msg.getMessage()).build());
            }
        }
    }
}
