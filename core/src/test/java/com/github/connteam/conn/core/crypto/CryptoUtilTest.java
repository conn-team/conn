package com.github.connteam.conn.core.crypto;

import static org.junit.Assert.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Test;

public class CryptoUtilTest {
    @Test
    public void testDecoding() throws InvalidKeySpecException {
        KeyPair pair = CryptoUtil.generateKeyPair();

        byte[] priv = pair.getPrivate().getEncoded();
        byte[] pub = pair.getPublic().getEncoded();

        KeyPair decoded = CryptoUtil.decodeKeyPair(pub, priv);

        assertArrayEquals(priv, decoded.getPrivate().getEncoded());
        assertArrayEquals(pub, decoded.getPublic().getEncoded());
    }

    @Test
    public void testSignatures() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        KeyPair pair1 = CryptoUtil.generateKeyPair();
        KeyPair pair2 = CryptoUtil.generateKeyPair();

        byte[] data = "one two three".getBytes();

        Signature sign1 = CryptoUtil.newSignature(pair1.getPrivate());
        sign1.update(data);
        byte[] signature1 = sign1.sign();

        Signature verify1 = CryptoUtil.newSignature(pair1.getPublic());
        verify1.update(data);
        assertTrue(verify1.verify(signature1));

        Signature verify2 = CryptoUtil.newSignature(pair2.getPublic());
        verify2.update(data);
        assertFalse(verify2.verify(signature1));
    }
}
