package com.github.connteam.conn.client.database.model;

import javax.validation.constraints.NotNull;

public class EphemeralKey {
    private int idKey;
    private byte[] publicKey;
    private byte[] privateKey;

    public int getId() {
        return idKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getPrivateKey() {
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
}