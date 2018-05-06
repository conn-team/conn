package com.github.connteam.conn.client.database.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class Friend {
    private int idUser;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Friend) {
            Friend x = (Friend)obj;
            return idUser == x.idUser;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return idUser;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int getId() {
        return idUser;
    }

    public void setId(int idUser) {
        this.idUser = idUser;
    }
};