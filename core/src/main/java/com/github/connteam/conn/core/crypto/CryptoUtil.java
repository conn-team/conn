package com.github.connteam.conn.core.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.github.connteam.conn.core.net.proto.NetProtos.SignedKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CryptoUtil {
    private final static Logger LOG = LoggerFactory.getLogger(CryptoUtil.class);

    public static final String KEYPAIR_ALGORITHM = "EC";
    public static final String KEYAGREEMENT_ALGORITHM = "ECDH";
    public static final String SIGNATURE_ALGORITHM = "SHA512withECDSA";
    public static final String CIPHER_DERIVE_HASH = "SHA-256";
    public static final String CIPHER_ALGORITHM = "AES";
    public static final int KEY_SIZE = 256;

    private static final KeyPairGenerator keyGen;
    private static final SecureRandom random;

    private CryptoUtil() {
    }

    static {
        try {
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
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            return KeyFactory.getInstance(KEYPAIR_ALGORITHM).generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey decodePrivateKey(byte[] data) throws InvalidKeySpecException {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
            return KeyFactory.getInstance(KEYPAIR_ALGORITHM).generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
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

    private static byte[] performCipher(SecretKey key, byte[] data, int mode) throws InvalidKeyException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(mode, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptSymmetric(SecretKey key, byte[] data) throws InvalidKeyException {
        return performCipher(key, data, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decryptSymmetric(SecretKey key, byte[] data) throws InvalidKeyException {
        return performCipher(key, data, Cipher.DECRYPT_MODE);
    }

    public static SecretKey getSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws InvalidKeyException {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance(KEYAGREEMENT_ALGORITHM);
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);

            MessageDigest hash = MessageDigest.getInstance(CIPHER_DERIVE_HASH);
            hash.update(agreement.generateSecret());
            return new SecretKeySpec(Arrays.copyOf(hash.digest(), 16), CIPHER_ALGORITHM);
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

    public static int randomInt(int bound) {
        synchronized (random) {
            return random.nextInt(bound);
        }
    }

    public static PublicKey verifyEphemeralKey(SignedKey key, PublicKey masterKey) {
        try {
            // Verify if key is valid
            byte[] data = key.getPublicKey().toByteArray();
            PublicKey decoded = decodePublicKey(data);

            // Verify signature
            Signature sign = CryptoUtil.newSignature(masterKey);
            sign.update(data);

            if (sign.verify(key.getSignature().toByteArray())) {
                return decoded;
            }
        } catch (SignatureException | InvalidKeyException | InvalidKeySpecException e) {
            LOG.error("Error verifying ephemeral key", e);
        }
        return null;
    }
}
