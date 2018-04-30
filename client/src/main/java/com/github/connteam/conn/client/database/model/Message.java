package com.github.connteam.conn.client.database.model;

import javax.validation.constraints.NotNull;

public class Message {
    private int idMessage;
    private int idFrom;
    private String message;

    public int getIdMessage() {
        return idMessage;
    }

    public int getIdFrom() {
        return idFrom;
    }

    public String getMessage() {
        return message;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public void setIdFrom(int idFrom) {
        this.idFrom = idFrom;
    }

    public void setMessage(@NotNull String message) {
        if (message == null) {
            throw new NullPointerException();
        }
        this.message = message;
    }
}