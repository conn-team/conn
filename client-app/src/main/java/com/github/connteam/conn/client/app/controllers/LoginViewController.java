package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class LoginViewController {
    private final App app;

    public LoginViewController(App app) {
        this.app = app;
    }

    @FXML
    protected void onLogin(ActionEvent event) {
    }

    @FXML
    protected void onRegisterSwitch(ActionEvent event) {
        app.setView(app.getRegisterView());
    }
}
