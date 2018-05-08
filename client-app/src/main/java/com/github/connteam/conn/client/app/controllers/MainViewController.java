package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;

public class MainViewController {
    private final App app;

    @FXML private ListView<?> friendsListView;
    @FXML private TextArea submitField;
    @FXML private TextArea messagesView;

    @FXML private MenuButton mainMenu;

    public MainViewController(App app) {
        this.app = app;
    }

    @FXML
    void onAddFriend(ActionEvent event) {
    }

    @FXML
    void onLogout(ActionEvent event) {
    }

    @FXML
    void onSubmitFieldKeyPress(KeyEvent event) {
    }
}
