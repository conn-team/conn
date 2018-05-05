package com.github.connteam.conn.client.database.model;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    public byte[] getRawPublicKey() {
        return publicKey;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public int getOutSequence() {
        return outSequence;
    }

    public int getInSequence() {
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

    public void setInSequence(int inSequence) {
        this.inSequence = inSequence;
    }

    public PublicKey getPublicKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawPublicKey());
    }

    public void setPublicKey(@NotNull PublicKey publicKey) {
        setPublicKey(publicKey.getEncoded());
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