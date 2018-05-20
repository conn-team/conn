package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.connteam.conn.core.Sanitization;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.AuthenticationException;
import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.NetMessages;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthResponse;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;
import com.github.connteam.conn.core.net.proto.NetProtos.EphemeralKeysDemand;
import com.github.connteam.conn.server.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.server.database.model.UserEntry;
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
    private final byte[] authPayload = CryptoUtil.randomBytes(64);

    private volatile State state = State.CREATED;
    private volatile UserEntry user;
    private volatile PublicKey publicKey;

    private final Map<Integer, Transmission> transmissions = new ConcurrentHashMap<>();
    private int ephemeralKeysCount, requestedEphemeralKeys; // Just estimated

    private static enum State {
        CREATED, AUTHENTICATION, ESTABLISHED, CLOSED
    }

    protected static class Transmission {
        final UserEntry user;
        final SignedKey partialKey1;

        public Transmission(UserEntry user, SignedKey partial) {
            this.user = user;
            partialKey1 = partial;
        }
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

    public boolean isEstablished() {
        return state == State.ESTABLISHED;
    }

    public ConnServer getServer() {
        return server;
    }

    public UserEntry getUser() {
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

    protected Map<Integer, Transmission> getTransmissions() {
        return transmissions;
    }

    public synchronized void handle() {
        if (state != State.CREATED) {
            throw new IllegalStateException("Cannot reuse ConnServerClient");
        }

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

    private synchronized AuthStatus.Status attemptLogin(AuthResponse msg)
            throws DatabaseException, InvalidKeySpecException {
        String username = msg.getUsername();
        byte[] receivedPublicKey = msg.getPublicKey().toByteArray();
        byte[] sign = msg.getSignature().toByteArray();

        if (!Sanitization.isValidUsername(username)) {
            return AuthStatus.Status.INVALID_INPUT;
        }

        // Verify if user is owner of private key by checking signature

        if (!ServerUtil.verifyLoginSignature(username, receivedPublicKey, authPayload, sign)) {
            return AuthStatus.Status.INVALID_SIGNATURE;
        }

        // Get user from database, if not present - register him

        DataProvider db = getDataProvider();
        UserEntry user = db.getUserByUsername(username).orElse(null);
        AuthStatus.Status mode = AuthStatus.Status.LOGGED_IN;

        if (user == null) {
            user = new UserEntry();
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

        ephemeralKeysCount = getDataProvider().countEphemeralKeysByUserId(user.getIdUser());
        requestedEphemeralKeys = 0;

        state = State.ESTABLISHED;
        channel.setMessageHandler(new ServerClientMessageHandler(this));
        return mode;
    }

    private synchronized void checkEphemeralKeysCount() {
        int current = ephemeralKeysCount + requestedEphemeralKeys;
        if (current >= MIN_EPHEMERAL_KEYS) {
            return;
        }

        // Ask client to refill ephemeral keys
        int n = MAX_EPHEMERAL_KEYS - current;
        requestedEphemeralKeys += n;
        channel.sendMessage(EphemeralKeysDemand.newBuilder().setCount(n).build());
    }

    protected synchronized void onEphemeralKeyDelete() {
        ephemeralKeysCount--;
        checkEphemeralKeysCount();
    }

    protected synchronized boolean addEphemeralKey(SignedKey key) {
        if (requestedEphemeralKeys <= 0) {
            LOG.warn("Received more ephemeral keys than requested");
            return false;
        }
        if (CryptoUtil.verifyEphemeralKey(key, publicKey) == null) {
            close(new ProtocolException("Invalid ephemeral key"));
            return false;
        }

        EphemeralKeyEntry entry = new EphemeralKeyEntry();
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
}
