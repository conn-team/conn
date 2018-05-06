package com.github.connteam.conn.client.database.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Settings {
    private String username;
    private byte[] publicKey;
    private byte[] privateKey;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Settings) {
            Settings x = (Settings)obj;
            return new EqualsBuilder().append(username, x.username).append(publicKey, x.publicKey)
                    .append(privateKey, x.privateKey).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(username).append(publicKey).append(privateKey).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getUsername() {
        return username;
    }

    public byte[] getRawPublicKey() {
        return publicKey;
    }

    public byte[] getRawPrivateKey() {
        return privateKey;
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