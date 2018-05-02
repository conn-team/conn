package com.github.connteam.conn.core.crypto;

import static org.junit.Assert.*;

import java.security.KeyPair;
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
}
