package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.client.database.model.Message;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

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

    private final ChangeListener<Conversation> currentConversationObserver = this::onCurrentConversationChange;
    private final ListChangeListener<Message> messagesObserver = this::onMessagesChange;

    public MainViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        app.getSessionManager().sessionProperty().addListener((prop, old, cur) -> {
            if (old != null) {
                old.currentConversationProperty().removeListener(currentConversationObserver);
                onCurrentConversationChange(old.currentConversationProperty(), old.getCurrentConversation(), null);
            }

            if (cur != null) {
                friendsListView.setItems(cur.getConversations());
                cur.currentConversationProperty().addListener(currentConversationObserver);
                onCurrentConversationChange(cur.currentConversationProperty(), null, cur.getCurrentConversation());
            } else {
                friendsListView.setItems(null);
                messagesView.setText("");
            }
        });

        friendsListView.getSelectionModel().selectedItemProperty().addListener((prop, old, cur) -> {
            Session session = app.getSession();
            if (session != null && old != cur) {
                session.setCurrentConversation(cur);
            }
        });

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            mainMenu.setText(cur ? "Łączenie..." : "Połączono!");
        });
    }

    private void onCurrentConversationChange(ObservableValue<?> observable, Conversation old, Conversation cur) {
        if (old != null) {
            submitField.textProperty().unbindBidirectional(old.currentMessageProperty());
            old.getMessages().removeListener(messagesObserver);
        }
        if (cur != null) {
            friendsListView.getSelectionModel().select(cur);
            submitField.textProperty().bindBidirectional(cur.currentMessageProperty());
            cur.getMessages().addListener(messagesObserver);
            onMessagesChange(null); // TODO
        }
    }

    private void onMessagesChange(ListChangeListener.Change<? extends Message> change) {
        StringBuilder str = new StringBuilder();

        for (Message msg : app.getSession().getCurrentConversation().getMessages()) {
            if (msg.isOutgoing()) {
                str.append(app.getSession().getClient().getSettings().getUsername());
            } else {
                str.append(app.getSession().getCurrentConversation().getUser().getUsername());
            }
            str.append(": ");
            str.append(msg.getMessage());
            str.append("\n");
        }

        messagesView.setText(str.toString());
        messagesView.setScrollTop(100000);
    }

    @FXML
    void onAddFriend(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Conn");
        dialog.setHeaderText("Dodaj znajomego");
        dialog.getDialogPane().setContentText("Nazwa użytkownika:");
        dialog.initOwner(app.getStage().getScene().getWindow());

        dialog.showAndWait().ifPresent(name -> app.getSession().openConversation(name));
    }

    @FXML
    void onLogout(ActionEvent event) {
        app.getSessionManager().disconnect();
    }

    @FXML
    void onSubmitFieldKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            onSubmit();
        }
    }

    @FXML
    void onSubmitButtonClick(MouseEvent event) {
        onSubmit();
    }

    void onSubmit() {
        String msg = submitField.getText().trim();
        if (msg.length() == 0) {
            return;
        }

        submitField.setText("");

        Conversation conv = app.getSession().getCurrentConversation();
        if (conv != null) {
            conv.sendMessage(msg);
        }
    }
}
