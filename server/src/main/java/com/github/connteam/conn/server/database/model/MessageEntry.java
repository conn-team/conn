package com.github.connteam.conn.server.database.model;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.Date;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MessageEntry {
    private int idMessage;
    private int idFrom;
    private int idTo;
    private byte[] message;
    private byte[] partialKey1;
    private byte[] partialKey2;
    private byte[] signature;
    private Timestamp time = new Timestamp(new Date().getTime());

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MessageEntry) {
            MessageEntry x = (MessageEntry) obj;
            return new EqualsBuilder().append(idMessage, x.idMessage).append(idFrom, x.idFrom).append(idTo, x.idTo)
                    .append(message, x.message).append(partialKey1, x.partialKey1).append(partialKey2, x.partialKey2)
                    .append(signature, x.signature).append(time, x.time).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idMessage).append(idFrom).append(idTo).append(message).append(partialKey1)
                .append(partialKey2).append(signature).append(time).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

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

    public byte[] getRawPartialKey1() {
        return partialKey1;
    }

    public byte[] getRawPartialKey2() {
        return partialKey2;
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

    public void setPartialKey1(@NotNull byte[] key) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.partialKey1 = key;
    }

    public void setPartialKey2(@NotNull byte[] key) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.partialKey2 = key;
    }

    public void setSignature(@NotNull byte[] signature) {
        if (signature == null) {
            throw new NullPointerException();
        }
        this.signature = signature;
    }

    public PublicKey getPartialKey1() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawPartialKey1());
    }

    public PublicKey getPartialKey2() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawPartialKey2());
    }

    public void setPartialKey1(@NotNull PublicKey publicKey) {
        setPartialKey1(publicKey.getEncoded());
    }

    public void setPartialKey2(@NotNull PublicKey publicKey) {
        setPartialKey2(publicKey.getEncoded());
    }
}