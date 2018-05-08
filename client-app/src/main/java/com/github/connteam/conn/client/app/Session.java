package com.github.connteam.conn.client.app;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
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
    private final DataProvider db;
    private ConnClient client;

    private final ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private final Property<Conversation> currentConversation = new SimpleObjectProperty<>();

    public Session(App app, DataProvider db) {
        this.app = app;
        this.db = db;
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

    public Property<Conversation> conversationProperty() {
        return currentConversation;
    }

    public void start() {
        app.getSessionManager().setConnecting(true);

        app.asyncTask(() -> {
            try {
				client = Session.createClient(db);
                client.setHandler(new SessionHandler());
                client.start();
			} catch (InvalidKeySpecException | IOException | DatabaseException e) {
                Platform.runLater(() -> {
                    app.getSessionManager().setConnecting(false);
                    app.reportError(e);
                });
			}
        });
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
        IOUtils.closeQuietly(db); // TODO: close it properly (when connection is closed)
    }

    public static ConnClient createClient(DataProvider db)
            throws IOException, DatabaseException, InvalidKeySpecException {
        return ConnClient.builder().setHost(HOST).setPort(PORT).setTransport(TRANSPORT).setIdentity(db).build();
    }

    public void openConversation(String username) {
        for (Conversation conv : conversations) {
            if (conv.getUser().getUsername().equals(username)) {
                setCurrentConversation(conv);
                return;
            }
        }

        try {
            client.getUserInfo(username, info -> Platform.runLater(() -> {
                if (info != null) {
                    Conversation conv = new Conversation(this, info);
                    conversations.add(conv);
                    setCurrentConversation(conv);
                } else {
                    app.reportError("Nie ma takiego użytkownika!");
                }
            }));
        } catch (DatabaseException e) {
            app.reportError(e);
        }
    }

    private class SessionHandler implements ConnClientListener {
        @Override
        public void onLogin(boolean hasBeenRegistered) {
            Platform.runLater(() -> app.getSessionManager().setConnecting(false));
        }

        @Override
        public void onDisconnect(Exception err) {
            client.close();

            if (err != null) {
                Platform.runLater(() -> {
                    app.getSessionManager().setConnecting(false);
                    app.reportError(err);
                });
            }
        }
    }
}
