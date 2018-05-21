package com.github.connteam.conn.core.crypto;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.github.connteam.conn.core.ByteArrayComparator;

public class SharedSecretGenerator {
    private final List<byte[]> elems = new ArrayList<byte[]>();
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public SharedSecretGenerator setPublic(PublicKey key) {
        publicKey = key;
        return this;
    }

    public SharedSecretGenerator setPrivate(PrivateKey key) {
        privateKey = key;
        return this;
    }

    public SharedSecretGenerator add(byte[] data) {
        elems.add(data);
        return this;
    }

    public SecretKey build() throws InvalidKeyException {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance(CryptoUtil.KEYAGREEMENT_ALGORITHM);
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);

            MessageDigest hash = MessageDigest.getInstance(CryptoUtil.CIPHER_DERIVE_HASH);
            hash.update(agreement.generateSecret());

            elems.sort(new ByteArrayComparator());
            for (byte[] elem : elems) {
                hash.update(elem);
            }

            byte[] digest = Arrays.copyOf(hash.digest(), 16);
            return new SecretKeySpec(digest, CryptoUtil.CIPHER_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
