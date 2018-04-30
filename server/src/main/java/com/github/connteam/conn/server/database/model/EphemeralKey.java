package com.github.connteam.conn.server.database.model;

import javax.validation.constraints.NotNull;

public class EphemeralKey {
    private int idKey;
    private int idUser;
    private byte[] key;
    private byte[] signature;

    public int getIdKey() {
        return idKey;
    }

    public int getIdUser() {
        return idUser;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setIdKey(int idKey) {
        this.idKey = idKey;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public void setKey(@NotNull byte[] key) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.key = key;
    }

    public void setSignature(@NotNull byte[] signature) {
        if (signature == null) {
            throw new NullPointerException();
        }
        this.signature = signature;
    }
}