package com.github.connteam.conn.core.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public final class CryptoUtil {
    public static final String KEYPAIR_ALGORITHM = "EC";
    public static final String SIGNATURE_ALGORITHM = "SHA512withECDSA";
    public static final int KEY_SIZE = 256;

    private static final KeyFactory keyFactory;
    private static final KeyPairGenerator keyGen;
    private static final SecureRandom random;

    private CryptoUtil() {
    }

    static {
        try {
            keyFactory = KeyFactory.getInstance(KEYPAIR_ALGORITHM);
            keyGen = KeyPairGenerator.getInstance(KEYPAIR_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            random = new SecureRandom();
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

    public static Signature newSignature(PublicKey key) throws InvalidKeyException {
        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initVerify(key);
            return sign;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Signature newSignature(PrivateKey key) throws InvalidKeyException {
        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            sign.initSign(key);
            return sign;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] randomBytes(int len) {
        byte[] out = new byte[len];
        synchronized (random) {
            random.nextBytes(out);
        }
        return out;
    }
}
