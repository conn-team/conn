package com.github.connteam.conn.client.app.controllers;

import java.util.List;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.IdentityManager.IdentityInfo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;

public class LoginViewController {
    private final App app;
    
    @FXML private ProgressBar progressBar;
    @FXML private ComboBox<IdentityInfo> identityChooser;
    @FXML private Button loginButton;

    public LoginViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        loginButton.setDisable(true);
        identityChooser.setItems(app.getIdentityManager().getIdentities());

        identityChooser.valueProperty().addListener(x -> {
            loginButton.setDisable(identityChooser.getValue() == null);
        });

        List<IdentityInfo> list = app.getIdentityManager().getIdentities();
        if (!list.isEmpty()) {
            identityChooser.setValue(list.get(0));
        }
    }

    @FXML
    protected void onLogin(ActionEvent event) {
    }

    @FXML
    protected void onRegisterSwitch(ActionEvent event) {
        app.setView(app.getRegisterView());
    }
}
