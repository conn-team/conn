package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import com.github.connteam.conn.core.Sanitization;
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
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.DeprecatedTextMessage;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysDemand;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysUpload;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfo;
import com.github.connteam.conn.core.net.proto.NetProtos.UserInfoRequest;
import com.github.connteam.conn.server.database.model.EphemeralKey;
import com.github.connteam.conn.server.database.model.User;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServerClient implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServerClient.class);
    public final static int MIN_EPHEMERAL_KEYS = 80;
    public final static int MAX_EPHEMERAL_KEYS = 100;

    private final ConnServer server;
    private final NetChannel channel;
    private volatile State state = State.CREATED;

    private volatile User user;
    private volatile PublicKey publicKey;

    private byte[] authPayload;
    private int ephemeralKeysCount, requestedEphemeralKeys;

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

    public DataProvider getDataProvider() {
        return server.getDataProvider();
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
        server.removeClient(this, err);
    }

    private synchronized void onAuthResponse(Message msg) {
        if (!(msg instanceof AuthResponse)) {
            close(new ProtocolException("Unexpected message on authentication stage"));
            return;
        }

        AuthStatus.Status status;
        try {
            status = attemptLogin((AuthResponse) msg);
        } catch (InvalidKeySpecException | DatabaseException e) {
            status = AuthStatus.Status.INTERNAL_ERROR;
            LOG.info("Error while authenticating user", e);
        }

        channel.sendMessage(AuthStatus.newBuilder().setStatus(status).build());

        if (status != AuthStatus.Status.LOGGED_IN && status != AuthStatus.Status.REGISTERED) {
            close(new AuthenticationException(status));
        } else {
            checkEphemeralKeysCount();
        }
    }

    private AuthStatus.Status attemptLogin(AuthResponse msg) throws DatabaseException, InvalidKeySpecException {
        String username = msg.getUsername();
        byte[] receivedPublicKey = msg.getPublicKey().toByteArray();
        byte[] sign = msg.getSignature().toByteArray();

        if (!Sanitization.isValidUsername(username)) {
            return AuthStatus.Status.INVALID_INPUT;
        }

        // Verify if user is owner of private key by checking signature

        if (!verifyLoginSignature(username, receivedPublicKey, authPayload, sign)) {
            return AuthStatus.Status.INVALID_SIGNATURE;
        }

        // Get user from database, if not present - register him

        DataProvider db = getDataProvider();
        User user = db.getUserByUsername(username).orElse(null);
        AuthStatus.Status mode = AuthStatus.Status.LOGGED_IN;

        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setPublicKey(receivedPublicKey);

            db.insertUser(user);
            user = db.getUserByUsername(username).orElse(null);
            mode = AuthStatus.Status.REGISTERED;

            if (user == null) {
                return AuthStatus.Status.MISMATCHED_PUBLICKEY;
            }
        }

        // Check if public key matches with user in database

        if (!Arrays.equals(user.getRawPublicKey(), receivedPublicKey)) {
            return AuthStatus.Status.MISMATCHED_PUBLICKEY;
        }

        // User is verified

        this.user = user;
        publicKey = user.getPublicKey();

        if (!server.addClient(this)) {
            return AuthStatus.Status.ALREADY_ONLINE;
        }

        state = State.ESTABLISHED;
        channel.setMessageHandler(new MessageHandler());
        ephemeralKeysCount = getDataProvider().countEphemeralKeysByUserId(user.getIdUser());
        requestedEphemeralKeys = 0;
        return mode;
    }

    private boolean verifyLoginSignature(String username, byte[] pubKey, byte[] toSign, byte[] signature) {
        try {
            Signature sign = CryptoUtil.newSignature(CryptoUtil.decodePublicKey(pubKey));
            sign.update(username.getBytes());
            sign.update(pubKey);
            sign.update(toSign);
            return sign.verify(signature);
        } catch (SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            return false;
        }
    }

    private void checkEphemeralKeysCount() {
        int current = ephemeralKeysCount + requestedEphemeralKeys;
        if (current >= MIN_EPHEMERAL_KEYS) {
            return;
        }

        // Ask client to refill ephemeral keys

        int n = MAX_EPHEMERAL_KEYS - current;
        requestedEphemeralKeys += n;
        channel.sendMessage(EphemeralKeysDemand.newBuilder().setCount(n).build());
    }

    private boolean verifyEphemeralKey(SignedKey key) {
        try {
            // Verify if key is valid
            CryptoUtil.decodePublicKey(key.getPublicKey().toByteArray());

            // Verify signature
            Signature sign = CryptoUtil.newSignature(publicKey);
            sign.update(key.getPublicKey().toByteArray());
            return sign.verify(key.getSignature().toByteArray());
        } catch (SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean onEphemeralKeyReceived(SignedKey key) {
        if (requestedEphemeralKeys <= 0) {
            LOG.warn("Received more ephemeral keys than requested");
            return false;
        }
        if (!verifyEphemeralKey(key)) {
            close(new ProtocolException("Invalid ephemeral key"));
            return false;
        }

        EphemeralKey entry = new EphemeralKey();
        entry.setIdUser(user.getIdUser());
        entry.setKey(key.getPublicKey().toByteArray());
        entry.setSignature(key.getSignature().toByteArray());

        try {
            getDataProvider().insertEphemeralKey(entry);
        } catch (DatabaseException e) {
            close(e);
            return false;
        }

        requestedEphemeralKeys--;
        ephemeralKeysCount++;
        return true;
    }

    public class MessageHandler extends MultiEventListener<Message> {
        @HandleEvent
        public void onKeepAlive(KeepAlive msg) {
            channel.sendMessage(msg);
        }

        @HandleEvent
        public void onTextMessage(DeprecatedTextMessage msg) {
            ConnServerClient client = server.getClientByName(msg.getUsername());

            if (client != null) {
                client.getNetChannel().sendMessage(DeprecatedTextMessage.newBuilder()
                        .setUsername(getUser().getUsername()).setMessage(msg.getMessage()).build());
            }
        }

        @HandleEvent
        public void onUserInfoRequest(UserInfoRequest msg) {
            String username = msg.getUsername();
            User user;

            try {
                user = getDataProvider().getUserByUsername(username).orElse(null);
            } catch (DatabaseException e) {
                close(e);
                return;
            }

            UserInfo.Builder resp = UserInfo.newBuilder();
            resp.setRequestID(msg.getRequestID());
            resp.setExists(user != null);

            if (user != null) {
                resp.setUsername(user.getUsername());
                resp.setPublicKey(ByteString.copyFrom(user.getRawPublicKey()));
            }

            channel.sendMessage(resp.build());
        }

        @HandleEvent
        public void onEphemeralKeysUpload(EphemeralKeysUpload msg) {
            for (SignedKey key : msg.getKeysList()) {
                if (!onEphemeralKeyReceived(key)) {
                    return;
                }
            }
        }
    }
}
