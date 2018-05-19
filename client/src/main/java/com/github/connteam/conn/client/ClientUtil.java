package com.github.connteam.conn.client;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;

import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.core.crypto.CryptoUtil;

public final class ClientUtil {
    private ClientUtil() {
    }

    public static byte[] makeLoginSignature(KeyPair keyPair, String username, byte[] toSign)
            throws InvalidKeyException, SignatureException {
        Signature sign = CryptoUtil.newSignature(keyPair.getPrivate());
        sign.update(username.getBytes());
        sign.update(keyPair.getPublic().getEncoded());
        sign.update(toSign);
        return sign.sign();
    }

    public static EphemeralKey generateEphemeralKey() {
        KeyPair pair = CryptoUtil.generateKeyPair();
        EphemeralKey key = new EphemeralKey();
        key.setPrivateKey(pair.getPrivate());
        key.setPublicKey(pair.getPublic());
        return key;
    }
}
