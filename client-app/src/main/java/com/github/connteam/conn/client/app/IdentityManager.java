package com.github.connteam.conn.client.app;

import java.io.File;

import com.github.connteam.conn.client.app.App;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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

        FXCollections.sort(identities);
    }

    public void createAndRegisterIdentity(String username) {
        app.asyncTask(new IdentityRegisterTask(username, err -> Platform.runLater(() -> {
            update();

            if (err != null) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Conn");
                alert.setHeaderText("Błąd rejestracji");
                alert.setContentText(err.toString());
                alert.showAndWait();
            } else {
                app.setView(app.getLoginView());
            }
        })));
    }
}
