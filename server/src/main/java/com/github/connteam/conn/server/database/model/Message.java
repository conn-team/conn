package com.github.connteam.conn.server.database.model;

import javax.validation.constraints.NotNull;

public class Message {
    private int idMessage;
    private int idFrom;
    private int idTo;
    private byte[] message;
    private byte[] key;
    private byte[] signature;

    public int getIdMessage() {
        return idMessage;
    }

    public int getIdFrom() {
        return idFrom;
    }

    public int getIdTo() {
        return idTo;
    }

    public byte[] getMessage() {
        return message;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public void setIdFrom(int idFrom) {
        this.idFrom = idFrom;
    }

    public void setIdTo(int idTo) {
        this.idTo = idTo;
    }

    public void setMessage(@NotNull byte[] message) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.message = message;
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