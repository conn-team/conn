package com.github.connteam.conn.server.database.model;

import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.crypto.CryptoUtil;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EphemeralKeyEntry {
    private int idKey;
    private int idUser;
    private byte[] key;
    private byte[] signature;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EphemeralKeyEntry) {
            EphemeralKeyEntry x = (EphemeralKeyEntry) obj;
            return new EqualsBuilder().append(idKey, x.idKey).append(idUser, x.idUser).append(key, x.key)
                    .append(signature, x.signature).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idKey).append(idUser).append(key).append(signature).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int getIdKey() {
        return idKey;
    }

    public int getIdUser() {
        return idUser;
    }

    public byte[] getRawKey() {
        return key;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setIdKey(int idKey) {
        this.idKey = idKey;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
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
}