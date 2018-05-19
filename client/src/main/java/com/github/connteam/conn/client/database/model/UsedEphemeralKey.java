package com.github.connteam.conn.client.database.model;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class UsedEphemeralKey {
    private byte[] key;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UsedEphemeralKey) {
            UsedEphemeralKey x = (UsedEphemeralKey) obj;
            return new EqualsBuilder().append(key, x.key).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(key).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public byte[] getRawKey() {
        return key;
    }

    public void setKey(@NotNull byte[] key) {
        if (key == null) {
            throw new NullPointerException();
        }
        this.key = key;
    }

    public PublicKey getKey() throws InvalidKeySpecException {
        return CryptoUtil.decodePublicKey(getRawKey());
    }
}