package com.github.connteam.conn.client.app.model;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UserEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Conversation {
    private final Session session;
    private final UserEntry user;

    private final ObservableList<MessageEntry> messages = FXCollections.observableArrayList();
    private final StringProperty currentMessage = new SimpleStringProperty();

    public Conversation(Session session, UserEntry user) {
        this.session = session;
        this.user = user;
    }

    public UserEntry getUser() {
        return user;
    }

    public ObservableList<MessageEntry> getMessages() {
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

    public void sendMessage(String text) {
        if (session.getClient() == null) {
            return;
        }

        MessageEntry msg = new MessageEntry();
        msg.setMessage(text);
        msg.setOutgoing(true);

        session.getClient().sendTextMessage(user, text);
        messages.add(msg);
    }
}
