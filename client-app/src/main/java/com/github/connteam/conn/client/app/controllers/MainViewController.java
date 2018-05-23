package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.controls.ConversationsListView;
import com.github.connteam.conn.client.app.controls.MessageListCell;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.util.DeepObserver;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.core.crypto.CryptoUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Text;

public class MainViewController {
    private final App app;

    @FXML
    private ConversationsListView friendsListView;
    @FXML
    private TextArea submitField;
    @FXML
    private ListView<MessageEntry> messagesView;
    @FXML
    private MenuButton mainMenu;
    @FXML
    private GridPane conversationBox;
    @FXML
    private VBox welcomeBox;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label conversationUsernameLabel;
    @FXML
    private Label conversationFingerprintLabel;
    @FXML
    private RowConstraints submitFieldRow;

    private boolean scrollbarFound = false;

    public MainViewController(App app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        messagesView.setCellFactory(x -> new MessageListCell());

        conversationUsernameLabel.setText("");
        conversationFingerprintLabel.setText("");

        welcomeBox.setVisible(true);
        conversationBox.setVisible(false);

        DeepObserver.listen(app.getSessionManager().sessionProperty(), (ctx, old, cur) -> {
            if (cur != null) {
                ctx.set(friendsListView.itemsProperty(), cur.getConversations(), null);
                ctx.bindBidirectional(friendsListView.currentItemProperty(), cur.currentConversationProperty());
                ctx.deepListen(cur.currentConversationProperty(), (a, b, c) -> onConversationChange(a, b, c)); // hmm
            }
        });

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            mainMenu.setText(cur ? "Łączenie..." : "Połączono!");
        });

        submitField.textProperty().addListener((prop, old, cur) -> {
            Text text = new Text();
            text.setWrappingWidth(submitField.getWidth() - 20);
            text.setFont(submitField.getFont());
            text.setText(cur);

            int rows = (int) (text.getLayoutBounds().getHeight() / 15);
            submitFieldRow.setPrefHeight(Double.max(Double.min(10 + rows * 17, 200), 40));
        });
    }

    private void onConversationChange(DeepObserver<? extends Conversation>.ObserverContext ctx, Conversation old,
            Conversation cur) {

        if (cur != null) {
            ctx.bindBidirectional(submitField.textProperty(), cur.currentMessageProperty());
            ctx.set(messagesView.itemsProperty(), cur.getMessages(), null);
            ctx.set(conversationUsernameLabel.textProperty(), cur.getUser().getUsername(), "");
            ctx.set(conversationFingerprintLabel.textProperty(),
                    CryptoUtil.getFingerprint(cur.getUser().getRawPublicKey()), "");

            ctx.listen(cur.getMessages(), change -> {
                while (change.next()) {
                    if (change.wasAdded() && change.getTo() == cur.getMessages().size()) {
                        messagesView.scrollTo(Integer.MAX_VALUE);
                        break;
                    }
                }
                change.reset();
            });

            ctx.set(cur.onFetchProperty(), () -> {
                messagesView.scrollTo(100);
            }, null);

            ctx.listen(cur.unreadProperty(), (prop, oldVal, curVal) -> {
                if (curVal != null && curVal) {
                    cur.setUnread(false);
                }
            });
        }

        welcomeBox.setVisible(cur == null);
        conversationBox.setVisible(cur != null);
    }

    @FXML
    void onAddFriend(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Conn");
        dialog.setHeaderText("Nowa rozmowa");
        dialog.getDialogPane().setContentText("Nazwa użytkownika:");
        dialog.initOwner(app.getStage().getScene().getWindow());

        dialog.showAndWait().ifPresent(name -> app.getSession().openConversation(name, conv -> {
            app.getSession().setCurrentConversation(conv);
        }));
    }

    @FXML
    void onLogout(ActionEvent event) {
        app.getSessionManager().disconnect();
    }

    @FXML
    void onSubmitFieldKeyPress(KeyEvent event) {
        switch (event.getCode()) {
        case ENTER:
            if (event.isShiftDown()) {
                insertNewLine();
            } else {
                onSubmit();
            }
            event.consume();
            break;

        default:
            break;
        }
    }

    @FXML
    void onSubmitButtonClick(MouseEvent event) {
        onSubmit();
    }

    private void onSubmit() {
        String msg = submitField.getText();
        if (msg == null) {
            return;
        }

        msg = msg.trim();
        if (msg.length() == 0) {
            return;
        }

        submitField.setText("");

        Conversation conv = app.getSession().getCurrentConversation();
        if (conv != null) {
            conv.sendMessage(msg);
        }
    }

    private void insertNewLine() {
        submitField.deleteText(submitField.getSelection());
        submitField.insertText(submitField.getSelection().getStart(), "\n");
    }

    @FXML
    void onMessagesViewMouseMoved(MouseEvent event) {
        if (scrollbarFound) {
            return;
        }

        // Let's hunt for ListView scrollbar!
        for (Node node : messagesView.lookupAll(".scroll-bar:vertical")) { // lookup doesn't work
            if (node instanceof ScrollBar) {
                ScrollBar scrollBar = (ScrollBar) node;

                scrollBar.valueProperty().addListener((prop, old, cur) -> {
                    if ((double) cur < 0.01) {
                        if (app.getSession() != null && app.getSession().getCurrentConversation() != null) {
                            app.getSession().getCurrentConversation().loadMoreMessages();
                        }
                    }
                });

                scrollbarFound = true;
                break;
            }
        }
    }
}
