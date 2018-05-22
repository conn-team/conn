package com.github.connteam.conn.client.app.model;

import java.util.List;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.database.DatabaseException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Conversation {
    private static final int MESSAGES_PER_FETCH = 100;

    private final Session session;
    private final UserEntry user;

    private final ObservableList<MessageEntry> messages = FXCollections.observableArrayList();
    private final StringProperty currentMessage = new SimpleStringProperty();

    private int nextFetchMaxID = Integer.MAX_VALUE;

    public Conversation(Session session, UserEntry user) {
        this.session = session;
        this.user = user;
        loadMoreMessages();
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

    public void loadMoreMessages() {
        if (nextFetchMaxID < 0) {
            return;
        }

        session.getApp().asyncTask(() -> {
            try {
                List<MessageEntry> fetched = session.getDataProvider().getMessagesPage(user.getId(), MESSAGES_PER_FETCH,
                        nextFetchMaxID);

                Platform.runLater(() -> {
                    messages.addAll(fetched);
                    nextFetchMaxID = (fetched.isEmpty() ? -1 : fetched.get(fetched.size() - 1).getIdMessage() - 1);
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> session.getApp().reportError(e));
            }
        });
    }

    public void sendMessage(String text) {
        if (session.getClient() == null) {
            return;
        }

        MessageEntry msg = new MessageEntry();
        msg.setMessage(text);
        msg.setOutgoing(true);

        session.getClient().sendTextMessage(user, text, err -> Platform.runLater(() -> {
            if (err != null) {
                session.getApp().reportError(err);
            }
        }));

        messages.add(msg);
    }
}
