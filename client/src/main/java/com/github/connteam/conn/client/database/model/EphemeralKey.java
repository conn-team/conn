package com.github.connteam.conn.client.database.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

public class EphemeralKey {
    private int idKey;
    private byte[] publicKey;
    private byte[] privateKey;

    public int getId() {
        return idKey;
    }

    public byte[] getRawPublicKey() {
        return publicKey;
    }

    public byte[] getRawPrivateKey() {
        return privateKey;
    }

    public void setId(int idKey) {
        this.idKey = idKey;
    }

    public void setPublicKey(@NotNull byte[] publicKey) {
        if (publicKey == null) {
            throw new NullPointerException();
        }
        this.publicKey = publicKey;
    }

    public void setPrivateKey(@NotNull byte[] privateKey) {
        if (privateKey == null) {
            throw new NullPointerException();
        }
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawPublicKey());
    }

    public PrivateKey getPrivateKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePrivateKey(getRawPrivateKey());
    }

    public void setPublicKey(@NotNull PublicKey publicKey) {
        setPublicKey(publicKey.getEncoded());
    }

    public void setPrivateKey(@NotNull PrivateKey privateKey) {
        setPrivateKey(privateKey.getEncoded());
    }
}