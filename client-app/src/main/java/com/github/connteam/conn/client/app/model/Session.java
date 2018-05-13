package com.github.connteam.conn.client.app.model;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Consumer;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.io.IOUtils;
import com.github.connteam.conn.core.net.Transport;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Session implements AutoCloseable {
    public static final String HOST = "localhost";
    public static final int PORT = 9090;
    public static final Transport TRANSPORT = Transport.SSL;

    private final App app;
    private final DataProvider database;

    private ConnClient client;
    private volatile boolean closed = false;

    private final ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private final Property<Conversation> currentConversation = new SimpleObjectProperty<>();

    public static ConnClient createClient(DataProvider db)
            throws IOException, DatabaseException, InvalidKeySpecException {
        return ConnClient.builder().setHost(HOST).setPort(PORT).setTransport(TRANSPORT).setIdentity(db).build();
    }

    public Session(App app, DataProvider db) {
        this.app = app;
        this.database = db;
    }

    public ConnClient getClient() {
        return client;
    }

    public DataProvider getDataProvider() {
        return database;
    }

    public ObservableList<Conversation> getConversations() {
        return conversations;
    }

    public Conversation getCurrentConversation() {
        return currentConversation.getValue();
    }

    public void setCurrentConversation(Conversation val) {
        currentConversation.setValue(val);
    }

    public Property<Conversation> currentConversationProperty() {
        return currentConversation;
    }

    public void start() {
        app.getSessionManager().setConnecting(true);
        app.asyncTask(() -> connect());
    }

    private void connect() {
        if (closed) {
            return;
        }

        try {
            ConnClient tmp = Session.createClient(database);
            Platform.runLater(() -> {
                client = tmp;
                client.setHandler(new SessionHandler());
                client.start();
            });
        } catch (Exception e) {
            Platform.runLater(() -> onConnectionClose(e));
        }
    }

    private void onConnectionClose(Exception err) {
        app.getSessionManager().setConnecting(false);
        client = null;

        if (closed) {
            IOUtils.closeQuietly(database);
        }

        if (err != null) {
            app.reportError(err);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        if (client != null) {
            client.close();
        } else {
            IOUtils.closeQuietly(database);
        }
    }

    public void openConversation(String username, Consumer<Conversation> callback) {
        if (client == null) {
            return;
        }

        for (Conversation conv : conversations) {
            if (conv.getUser().getUsername().equals(username)) {
                setCurrentConversation(conv);
                callback.accept(conv);
                return;
            }
        }

        try {
            client.getUserInfo(username, info -> Platform.runLater(() -> {
                if (info != null) {
                    Conversation conv = new Conversation(this, info);
                    conversations.add(conv);
                    setCurrentConversation(conv);
                    callback.accept(conv);
                } else {
                    app.reportError("Nie ma takiego użytkownika!");
                }
            }));
        } catch (DatabaseException e) {
            app.reportError(e);
        }
    }

    public void openConversation(String username) {
        openConversation(username, x -> {
        });
    }

    private class SessionHandler implements ConnClientListener {
        @Override
        public void onLogin(boolean hasBeenRegistered) {
            Platform.runLater(() -> {
                app.getSessionManager().setConnecting(false);

                try {
                    for (User user : database.getUsers()) {
                        openConversation(user.getUsername());
                    }
                } catch (DatabaseException e) {
                    app.reportError(e);
                }
            });
        }

        @Override
        public void onDisconnect(Exception err) {
            Platform.runLater(() -> onConnectionClose(err));
        }

        @Override
        public void onTextMessage(String from, String message) {
            Platform.runLater(() -> openConversation(from, conv -> {
                Message msg = new Message();
                msg.setMessage(message);
                msg.setOutgoing(false);
                conv.getMessages().add(msg);
            }));
        }
    }
}
