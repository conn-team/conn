package com.github.connteam.conn.client.app.model;

import java.io.File;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.IdentityRegisterTask;
import com.github.connteam.conn.core.net.AuthenticationException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class IdentityManager {
    public static final String EXTENSION = ".id";
    public static final String EXTENSION_TEMP = ".id.pending";

    private final App app;
    private final ObservableList<IdentityInfo> identities = FXCollections.observableArrayList();

    public static class IdentityInfo implements Comparable<IdentityInfo> {
        private final File file;

        public IdentityInfo(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public String getName() {
            return file.getName().split("\\.")[0];
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int compareTo(IdentityInfo other) {
            return getName().compareTo(other.getName());
        }
    }

    public IdentityManager(App app) {
        this.app = app;
        update();
    }

    public ObservableList<IdentityInfo> getIdentities() {
        return identities;
    }

    public IdentityInfo getIdentityByName(String name) {
        for (IdentityInfo info : identities) {
            if (info.getName().equals(name)) {
                return info;
            }
        }
        return null;
    }

    public void update() {
        identities.removeIf(id -> !id.getFile().exists());

        for (File file : new File(App.CONFIG_DIR).listFiles()) {
            if (!file.isFile() || !file.getName().endsWith(EXTENSION)) {
                continue;
            }
            if (identities.stream().noneMatch(id -> id.getFile().equals(file))) {
                identities.add(new IdentityInfo(file));
            }
        }
    }

    public void createAndUseIdentity(String username) {
        app.getSessionManager().setConnecting(true);

        app.asyncTask(new IdentityRegisterTask(username, err -> Platform.runLater(() -> {
            app.getSessionManager().setConnecting(false);
            update();

            if (err != null) {
                if (err instanceof AuthenticationException) {
                    app.reportError(err.getLocalizedMessage());
                } else {
                    app.reportError(err);
                }
            } else {
                app.getSessionManager().connect(getIdentityByName(username));
            }
        })));
    }
}
