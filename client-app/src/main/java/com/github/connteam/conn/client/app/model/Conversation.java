package com.github.connteam.conn.client.app.model;

import java.util.List;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.database.DatabaseException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Conversation {
    private static final int MESSAGES_PER_FETCH = 50;
    // Special message IDs
    public static final int SENDING_MESSAGE = -1;
    public static final int SENDING_ERROR = -2;

    private final Session session;
    private final UserEntry user;

    private final ObservableList<MessageEntry> messages = FXCollections.observableArrayList();
    private final StringProperty currentMessage = new SimpleStringProperty();

    private int nextFetchMaxID = Integer.MAX_VALUE;
    private final Property<Runnable> onFetch = new SimpleObjectProperty<>();

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

    public Runnable getOnFetch() {
        return onFetch.getValue();
    }

    public void setOnFetch(Runnable run) {
        onFetch.setValue(run);
    }

    public Property<Runnable> onFetchProperty() {
        return onFetch;
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
                    if (fetched.isEmpty()) {
                        nextFetchMaxID = -1;
                        return;
                    }

                    for (MessageEntry msg : fetched) {
                        messages.add(0, msg);
                        nextFetchMaxID = Integer.min(nextFetchMaxID, msg.getIdMessage() - 1);
                    }

                    if (onFetch != null && getOnFetch() != null) {
                        getOnFetch().run();
                    }
                });
            } catch (DatabaseException e) {
                Platform.runLater(() -> session.getApp().reportError(e));
            }
        });
    }

    public void sendMessage(String text) {
        MessageEntry msg = new MessageEntry();
        msg.setIdMessage(SENDING_MESSAGE);
        msg.setIdUser(user.getId());
        msg.setOutgoing(true);
        msg.setMessage(text);

        if (session.getClient() == null) {
            msg.setIdMessage(SENDING_ERROR);
            messages.add(msg);
            return;
        }

        session.getClient().sendTextMessage(user, text, (saved, err) -> Platform.runLater(() -> {
            MessageEntry updated = msg;

            if (err == null) {
                updated = saved;
            } else {
                session.getApp().reportError(err);
                msg.setIdMessage(SENDING_ERROR);
            }

            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i) == msg) {
                    messages.set(i, updated);
                    break;
                }
            }
        }));

        messages.add(msg);
    }
}
