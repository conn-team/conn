package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.util.DeepObserver;
import com.github.connteam.conn.client.database.model.Message;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

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
    @FXML
    private GridPane conversationBox;
    @FXML
    private VBox welcomeBox;
    @FXML
    private Label welcomeLabel;

    public MainViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        DeepObserver.listen(app.getSessionManager().sessionProperty(), (ctx, old, cur) -> {
            if (cur != null) {
                friendsListView.setItems(cur.getConversations());

                ctx.deepListen(cur.currentConversationProperty(), (ctxConv, oldConv, curConv) -> {
                    friendsListView.getSelectionModel().select(curConv);

                    if (curConv != null) {
                        ctxConv.bindBidirectional(submitField.textProperty(), curConv.currentMessageProperty());
                        ctxConv.listen(curConv.getMessages(), x -> onMessagesChange(x));
                    }

                    welcomeBox.setVisible(curConv == null);
                    conversationBox.setVisible(curConv != null);
                });

                friendsListView.getSelectionModel().selectedItemProperty().addListener((prop, oldElem, curElem) -> {
                    if (oldElem != curElem) {
                        cur.setCurrentConversation(curElem);
                    }
                });
            } else {
                friendsListView.setItems(null);
            }
        });

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            mainMenu.setText(cur ? "Łączenie..." : "Połączono!");
        });
    }

    private void onMessagesChange(ListChangeListener.Change<? extends Message> change) {
        StringBuilder str = new StringBuilder();

        for (Message msg : change.getList()) {
            if (msg.isOutgoing()) {
                str.append(app.getSession().getClient().getSettings().getUsername());
            } else {
                str.append(app.getSession().getCurrentConversation().getUser().getUsername());
            }
            str.append(": ");
            str.append(msg.getMessage());
            str.append("\n");
        }

        messagesView.setText(str.toString().trim());

        // Hacky way to scroll to bottom (setScrollTop didn't work as expected)
        messagesView.selectEnd();
        messagesView.deselect();
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
