package com.github.connteam.conn.server.database.model;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Message {
    private int idMessage;
    private int idFrom;
    private int idTo;
    private byte[] message;
    private byte[] key;
    private byte[] signature;
    private Timestamp time;

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

    public byte[] getRawKey() {
        return key;
    }

    public byte[] getSignature() {
        return signature;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public void setTime(@NotNull Timestamp time) {
        if (time == null) {
            throw new NullPointerException();
        }
        this.time = time;
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

    public PublicKey getKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawKey());
    }

    public void setKey(@NotNull PublicKey publicKey) {
        setKey(publicKey.getEncoded());
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}