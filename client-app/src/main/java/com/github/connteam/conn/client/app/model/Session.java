package com.github.connteam.conn.client.app.model;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.function.Consumer;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
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
    private static final Transport TRANSPORT = Transport.SSL;
    private static String host = "localhost";
    private static int port = 7312;

    private final App app;
    private final DataProvider database;

    private ConnClient client;
    private volatile boolean closed = false;

    private final ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private final Property<Conversation> currentConversation = new SimpleObjectProperty<>();

    public static ConnClient createClient(DataProvider db)
            throws IOException, DatabaseException, InvalidKeySpecException {
        return ConnClient.builder().setHost(host).setPort(port).setTransport(TRANSPORT).setIdentity(db).build();
    }

    public static void setHost(String h) {
        host = h;
    }

    public static void setPort(int p) {
        port = p;
    }

    public Session(App app, DataProvider db) {
        this.app = app;
        this.database = db;
    }

    public App getApp() {
        return app;
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

        app.asyncTask(() -> {
            try {
                List<UserEntry> users = database.getUsers();
                Platform.runLater(() -> {
                    conversations.clear();
                    for (UserEntry user : users) {
                        conversations.add(new Conversation(this, user));
                    }
                });
            } catch (DatabaseException e) {
                app.reportError(e);
                return;
            }

            connect();
        });
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
            if (conv.getUser().getUsername().equalsIgnoreCase(username)) {
                callback.accept(conv);
                return;
            }
        }

        try {
            client.getUserInfo(username, info -> Platform.runLater(() -> {
                if (info != null) {
                    Conversation conv = new Conversation(this, info);
                    conversations.add(conv);
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
            Platform.runLater(() -> app.getSessionManager().setConnecting(false));
        }

        @Override
        public void onDisconnect(Exception err) {
            Platform.runLater(() -> onConnectionClose(err));
        }

        @Override
        public void onTextMessage(UserEntry from, String message) {
            Platform.runLater(() -> openConversation(from.getUsername(), conv -> {
                MessageEntry msg = new MessageEntry();
                msg.setMessage(message);
                msg.setOutgoing(false);
                conv.getMessages().add(msg);
            }));
        }
    }
}
