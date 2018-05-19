package com.github.connteam.conn.server;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerUtil {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServerClient.class);

    private ServerUtil() {
    }

    public static boolean verifyLoginSignature(String username, byte[] pubKey, byte[] toSign, byte[] signature) {
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

    public static boolean verifyEphemeralKey(SignedKey key, PublicKey masterKey) {
        try {
            // Verify if key is valid
            CryptoUtil.decodePublicKey(key.getPublicKey().toByteArray());

            // Verify signature
            Signature sign = CryptoUtil.newSignature(masterKey);
            sign.update(key.getPublicKey().toByteArray());
            return sign.verify(key.getSignature().toByteArray());
        } catch (SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            LOG.error("Error verifying ephemeral key", e);
            return false;
        }
    }
}
