package com.github.connteam.conn.client.database.model;

import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Message {
    private int idMessage;
    private int idUser;
    private boolean isOutgoing;
    private String message;
    private Timestamp time;

    public int getIdMessage() {
        return idMessage;
    }

    public int getIdUser() {
        return idUser;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public void setOutgoing(boolean isOutgoing) {
        this.isOutgoing = isOutgoing;
    }

    public void setTime(@NotNull Timestamp time) {
        if (time == null) {
            throw new NullPointerException();
        }
        this.time = time;
    }

    public void setMessage(@NotNull String message) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.message = message;
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