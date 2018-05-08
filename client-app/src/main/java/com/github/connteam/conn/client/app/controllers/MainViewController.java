package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.model.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyEvent;

public class MainViewController {
    private final App app;

    @FXML
    private ListView<Conversation> friendsListView;
    @FXML
    private TextArea submitField;
    @FXML
    private TextArea messagesView;

    @FXML
    private MenuButton mainMenu;

    public MainViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        app.getSessionManager().sessionProperty().addListener((prop, old, cur) -> {
            if (old != null) {
                unbindSession(old);
            }
            if (cur != null) {
                bindSession(cur);
            }
        });
    }

    private void bindSession(Session session) {
        friendsListView.setItems(session.getConversations());
    }

    private void unbindSession(Session session) {
        friendsListView.setItems(null);
    }

    @FXML
    void onAddFriend(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Conn");
        dialog.setHeaderText("Dodaj znajomego");
        dialog.getDialogPane().setContentText("Nazwa użytkownika:");
        dialog.initOwner(app.getStage().getScene().getWindow());

        dialog.showAndWait().ifPresent(name -> app.getSessionManager().getSession().openConversation(name));
    }

    @FXML
    void onLogout(ActionEvent event) {
        app.getSessionManager().disconnect();
    }

    @FXML
    void onSubmitFieldKeyPress(KeyEvent event) {
    }
}
