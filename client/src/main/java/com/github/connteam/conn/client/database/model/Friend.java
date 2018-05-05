package com.github.connteam.conn.client.database.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Friend {
    private int idUser;

    public int getId() {
        return idUser;
    }

    public void setId(int idUser) {
        this.idUser = idUser;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
};