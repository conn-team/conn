package com.github.connteam.conn.client.app.model;

import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.client.database.model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Conversation {
    private final Session session;
    private final User user;

    private final ObservableList<Message> messages = FXCollections.observableArrayList();
    private final StringProperty currentMessage = new SimpleStringProperty();

    public Conversation(Session session, User user) {
        this.session = session;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public ObservableList<Message> getMessages() {
        return messages;
    }

    public String getCurrentMessage() {
        return currentMessage.get();
    }

    public void setCurrentMessage(String msg) {
        currentMessage.set(msg);
    }

    public StringProperty currentMessageProperty() {
        return currentMessage;
    }

    @Override
    public String toString() {
        return user.getUsername();
    }
}
