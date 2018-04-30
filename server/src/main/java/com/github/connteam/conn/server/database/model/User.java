package com.github.connteam.conn.server.database.model;

import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

public class User {
    private int id;
    private String username;
    private byte[] publicKey;
    private Timestamp signupTime;

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Timestamp getSignupTime() {
        return signupTime;
    }

    public void setId(int id) {
        this.id = id;
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

    public void setSignupTime(@NotNull Timestamp signupTime) {
        if (signupTime == null) {
            throw new NullPointerException();
        }
        this.signupTime = signupTime;
    }
}