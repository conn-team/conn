package com.github.connteam.conn.client.database.model;

import java.sql.Timestamp;
import java.util.Date;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MessageEntry {
    private int idMessage;
    private int idUser;
    private boolean isOutgoing;
    private String message;
    private Timestamp time = new Timestamp(new Date().getTime());

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MessageEntry) {
            MessageEntry x = (MessageEntry) obj;
            return new EqualsBuilder().append(idMessage, x.idMessage).append(idUser, x.idUser)
                    .append(isOutgoing, x.isOutgoing).append(message, x.message).append(time, x.time).isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(idMessage).append(idUser).append(isOutgoing).append(message).append(time)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

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
}