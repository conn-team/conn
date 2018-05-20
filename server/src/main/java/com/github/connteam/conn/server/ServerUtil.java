package com.github.connteam.conn.server;

import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.core.crypto.CryptoUtil;

public final class ServerUtil {
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
}
