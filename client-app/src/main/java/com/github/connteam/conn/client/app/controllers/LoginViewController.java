package com.github.connteam.conn.client.app.controllers;

import java.util.List;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.IdentityManager.IdentityInfo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;

public class LoginViewController {
    private final App app;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private ComboBox<IdentityInfo> identityChooser;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink registerSwitchButton;

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

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            if (old != cur) {
                progressBar.setProgress(cur ? ProgressBar.INDETERMINATE_PROGRESS : 0);
                identityChooser.setDisable(cur);
                loginButton.setDisable(cur || identityChooser.getValue() == null);
                registerSwitchButton.setDisable(cur);
            }
        });
    }

    @FXML
    protected void onLogin(ActionEvent event) {
        IdentityInfo info = identityChooser.getValue();
        if (info != null) {
            app.getSessionManager().connect(info);
        }
    }

    @FXML
    protected void onRegisterSwitch(ActionEvent event) {
        app.setView(app.getRegisterView());
    }
}
