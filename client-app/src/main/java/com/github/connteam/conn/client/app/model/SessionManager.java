package com.github.connteam.conn.client.app.model;

import com.github.connteam.conn.client.IdentityFactory;
import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.IdentityManager.IdentityInfo;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.database.DatabaseException;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

public class SessionManager {
    private final App app;

    private final BooleanProperty connecting = new SimpleBooleanProperty();
    private final Property<Session> session = new SimpleObjectProperty<>();

    public SessionManager(App app) {
        this.app = app;
    }

    public boolean isConnecting() {
        return connecting.get();
    }

    public void setConnecting(boolean state) {
        connecting.set(state);
    }

    public BooleanProperty connectingProperty() {
        return connecting;
    }

    public Session getSession() {
        return session.getValue();
    }

    public void setSession(Session val) {
        session.setValue(val);
    }

    public Property<Session> sessionProperty() {
        return session;
    }

    public void connect(IdentityInfo identity) {
        disconnect();
        app.getSessionManager().setConnecting(true);

        app.asyncTask(() -> {
            try {
                DataProvider db = IdentityFactory.load(identity.getFile().getAbsolutePath());
                Platform.runLater(() -> {
                    try {
                        setSession(new Session(app, db));
                        getSession().start();
                    } catch (DatabaseException e) {
                        app.reportError(e);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    app.getSessionManager().setConnecting(false);
                    app.reportError(e);
                });
            }
        });
    }

    public void disconnect() {
        if (getSession() != null) {
            app.getSessionManager().setConnecting(false);
            getSession().close();
            setSession(null);
        }
    }
}
