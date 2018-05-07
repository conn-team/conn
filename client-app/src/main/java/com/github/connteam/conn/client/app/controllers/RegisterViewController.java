package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class RegisterViewController {
    private final App app;

    public RegisterViewController(App app) {
        this.app = app;
    }

    @FXML
    protected void onRegister(ActionEvent event) {
    }

    @FXML
    protected void onLoginSwitch(ActionEvent event) {
        app.setView(app.getLoginView());
    }
}
