package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.core.Sanitization;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;

public class RegisterViewController {
    private final App app;

    @FXML private ProgressBar progressBar;
    @FXML private TextField usernameField;
    @FXML private Button registerButton;

    public RegisterViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        registerButton.setDisable(true);

        usernameField.textProperty().addListener((prop, old, cur) -> {
            registerButton.setDisable(!Sanitization.isValidUsername(cur));
            
            String filtered = Sanitization.filterUsername(cur);
            if (!filtered.equals(cur)) {
                usernameField.setText(filtered);
            }
        });
    }

    @FXML
    protected void onRegister(ActionEvent event) {
        String username = usernameField.getText();
        if (!Sanitization.isValidUsername(username)) {
            return;
        }

        app.getIdentityManager().createAndRegisterIdentity(username);
    }

    @FXML
    protected void onLoginSwitch(ActionEvent event) {
        app.setView(app.getLoginView());
    }
}
