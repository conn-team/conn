package com.github.connteam.conn.client.database.model;

import javax.validation.constraints.NotNull;

public class User {
    private int idUser;
    private String username;
    private byte[] publicKey;
    private boolean isVerified;
    private int outSequence;
    private int inSequence;

    public int getId() {
        return idUser;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public int getOutSequence() {
        return outSequence;
    }

    public int getinSequence() {
        return inSequence;
    }

    public void setId(int idUser) {
        this.idUser = idUser;
    }

    public void setUsername(@NotNull String username) {
        if (username == null) {
            throw new NullPointerException();
        }
        this.username = username;
    }

    public void setPublicKey(@NotNull byte[] publicKey) {
        if (publicKey == null) {
            throw new NullPointerException();
        }
        this.publicKey = publicKey;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public void setOutSequence(int outSequence) {
        this.outSequence = outSequence;
    }

    public void setinSequence(int inSequence) {
        this.inSequence = inSequence;
    }
}