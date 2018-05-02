package com.github.connteam.conn.core.crypto;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class CryptoUtil {
    private static final KeyFactory keyFactory;
    private static final KeyPairGenerator keyGen;

    private CryptoUtil() {}

    static {
        try {
            keyFactory = KeyFactory.getInstance("EC");
            keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateKeyPair() {
        synchronized (keyGen) {
            return keyGen.generateKeyPair();
        }
    }

    public static PublicKey decodePublicKey(byte[] data) throws InvalidKeySpecException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        synchronized (keyFactory) {
            return keyFactory.generatePublic(spec);
        }
    }

    public static PrivateKey decodePrivateKey(byte[] data) throws InvalidKeySpecException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
        synchronized (keyFactory) {
            return keyFactory.generatePrivate(spec);
        }
    }

    public static KeyPair decodeKeyPair(byte[] publicKey, byte[] privateKey) throws InvalidKeySpecException {
        return new KeyPair(decodePublicKey(publicKey), decodePrivateKey(privateKey));
    }
}
