package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.core.Sanitization;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

public class RegisterViewController {
    private final App app;

    @FXML private ProgressBar progressBar;
    @FXML private TextField usernameField;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginSwitchButton;

    public RegisterViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        registerButton.setDisable(true);

        usernameField.textProperty().addListener((prop, old, cur) -> {
            if (old != cur) {
                updateButtonState();
                String filtered = Sanitization.filterUsername(cur);
                if (!filtered.equals(cur)) {
                    usernameField.setText(filtered);
                }
            }
        });

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            if (old != cur) {
                updateButtonState();
                progressBar.setProgress(cur ? ProgressBar.INDETERMINATE_PROGRESS : 0);
                usernameField.setDisable(cur);
                loginSwitchButton.setDisable(cur);
            }
        });
    }

    private void updateButtonState() {
        String name = usernameField.getText();
        registerButton.setDisable(app.getSessionManager().isConnecting() || !Sanitization.isValidUsername(name)
                || app.getIdentityManager().getIdentityByName(name) != null);
    }

    @FXML
    protected void onRegister(ActionEvent event) {
        String username = usernameField.getText();
        if (Sanitization.isValidUsername(username)) {
            app.getIdentityManager().createAndUseIdentity(username);
        }
    }

    @FXML
    protected void onLoginSwitch(ActionEvent event) {
        app.setView(app.getLoginView());
    }
}
