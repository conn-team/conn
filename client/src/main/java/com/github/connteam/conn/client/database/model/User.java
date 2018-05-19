package com.github.connteam.conn.client.database.model;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class User {
    private int idUser;
    private String username;
    private byte[] publicKey;
    private boolean isVerified;
    private int outSequence;
    private int inSequence;
    private boolean isFriend;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User x = (User)obj;
            return new EqualsBuilder().append(idUser, x.idUser).append(username, x.username)
                    .append(publicKey, x.publicKey).append(isVerified, x.isVerified).append(outSequence, x.outSequence)
                    .append(inSequence, x.inSequence).append(isFriend, x.isFriend).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idUser).append(username).append(publicKey).append(isVerified)
                .append(outSequence).append(inSequence).append(isFriend).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

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

    public boolean isFriend() {
        return isFriend;
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

    public void isFriend(boolean isFriend) {
        this.isFriend = isFriend;
    }
}