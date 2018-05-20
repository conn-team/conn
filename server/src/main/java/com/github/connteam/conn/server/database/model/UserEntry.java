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

public class UserEntry {
    private int idUser;
    private String username;
    private byte[] publicKey;
    private Timestamp signupTime = new Timestamp(new Date().getTime());

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserEntry) {
            UserEntry x = (UserEntry) obj;
            return new EqualsBuilder().append(idUser, x.idUser).append(username, x.username)
                    .append(publicKey, x.publicKey).append(signupTime, x.signupTime).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idUser).append(username).append(publicKey).append(signupTime).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int getIdUser() {
        return idUser;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getRawPublicKey() {
        return publicKey;
    }

    public Timestamp getSignupTime() {
        return signupTime;
    }

    public void setIdUser(int id) {
        this.idUser = id;
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

    public PublicKey getPublicKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawPublicKey());
    }

    public void setPublicKey(@NotNull PublicKey publicKey) {
        setPublicKey(publicKey.getEncoded());
    }
}