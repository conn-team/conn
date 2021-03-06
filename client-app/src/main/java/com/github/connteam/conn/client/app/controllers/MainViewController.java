package com.github.connteam.conn.client.app.controllers;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.controls.ConversationsListView;
import com.github.connteam.conn.client.app.controls.EmojiPopup;
import com.github.connteam.conn.client.app.controls.MessageListCell;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.model.Session;
import com.github.connteam.conn.client.app.util.DeepObserver;
import com.github.connteam.conn.client.app.util.ToggleBinder;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.net.proto.NetProtos.UserStatus;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Text;

public class MainViewController {
    private final App app;

    @FXML
    private GridPane rootPane;
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
    private GridPane bottomPane;
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
    @FXML
    private HBox verificationNotice;
    @FXML
    private RadioMenuItem availableRadioMenuItem;
    @FXML
    private RadioMenuItem awayRadioMenuItem;
    @FXML
    private RadioMenuItem busyRadioMenuItem;
    @FXML
    private RadioMenuItem invisibleRadioMenuItem;
    @FXML
    private ImageView emojiPopupButton;

    private final EmojiPopup emojiPopup = new EmojiPopup();
    private final ToggleBinder<UserStatus> statusBinder = new ToggleBinder<>();
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
        verificationNotice.setVisible(false);

        statusBinder.addToggle(availableRadioMenuItem, UserStatus.AVAILABLE);
        statusBinder.addToggle(awayRadioMenuItem, UserStatus.AWAY);
        statusBinder.addToggle(busyRadioMenuItem, UserStatus.BUSY);
        statusBinder.addToggle(invisibleRadioMenuItem, UserStatus.DISCONNECTED);

        DeepObserver.listen(app.getSessionManager().sessionProperty(), (ctx, old, cur) -> {
            if (cur != null) {
                ctx.set(friendsListView.itemsProperty(), cur.getConversations(), null);
                ctx.bindBidirectional(statusBinder.getProperty(), cur.userStatusProperty());
                ctx.bindBidirectional(friendsListView.currentItemProperty(), cur.currentConversationProperty());
                ctx.deepListen(cur.currentConversationProperty(), (a, b, c) -> onConversationChange(a, b, c)); // hmm
            }
            emojiPopup.hide();
        });

        app.getSessionManager().connectingProperty().addListener((prop, old, cur) -> {
            rootPane.getStyleClass().removeIf(x -> x.equals("state-connecting"));
            if (cur) {
                rootPane.getStyleClass().add("state-connecting");
            }
        });

        submitField.textProperty().addListener((prop, old, cur) -> {
            Text text = new Text();
            text.setWrappingWidth(submitField.getWidth() - 20);
            text.setFont(submitField.getFont());
            text.setText(cur);

            // Yeah
            int rows = (int) (text.getLayoutBounds().getHeight() / 15);
            submitFieldRow.setPrefHeight(Double.max(Double.min(10 + rows * 17, 150), 40));
        });

        bottomPane.heightProperty().addListener((prop, old, cur) -> {
            Platform.runLater(() -> updateEmojiPopupPosition(false));
        });

        emojiPopup.setOnEmojiClick(emoji -> {
            int start = submitField.getSelection().getStart();
            if (start > 0 && submitField.getText().charAt(start - 1) != ' ') {
                insertTextToSubmit(" " + emoji.getPrimaryCode() + " ");
            } else {
                insertTextToSubmit(emoji.getPrimaryCode() + " ");
            }
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

            ctx.bind(verificationNotice.visibleProperty(), cur.needsVerificationProperty());

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
        } else {
            verificationNotice.setVisible(false);
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

        dialog.showAndWait().ifPresent(name -> {
            Session session = app.getSession();
            if (session == null) {
                return;
            }

            if (name.equalsIgnoreCase(session.getSettings().getUsername())) {
                app.reportError("Forever alone?");
                return;
            }

            app.getSession().openConversation(name, conv -> {
                app.getSession().setCurrentConversation(conv);
            });
        });
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
                insertTextToSubmit("\n");
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

    private void insertTextToSubmit(String text) {
        submitField.deleteText(submitField.getSelection());
        submitField.insertText(submitField.getSelection().getStart(), text);
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

    @FXML
    void onVerificationNoticeMouseClicked(MouseEvent event) {
        Session session = app.getSession();
        if (session == null) {
            return;
        }

        Conversation conv = session.getCurrentConversation();
        if (conv == null) {
            return;
        }

        SettingsEntry local = session.getSettings();
        UserEntry other = conv.getUser();
        String codeForUs = CryptoUtil.getIdentityVerificationCode(other.getRawPublicKey(), local.getRawPublicKey());

        TextInputDialog dialog = new TextInputDialog();
        prepareVerificationDialog(dialog, local, other);
        dialog.getDialogPane().setContentText("Kod od " + other.getUsername() + ":");

        dialog.showAndWait().ifPresent(code -> {
            if (code.replace("-", "").equalsIgnoreCase(codeForUs.replace("-", ""))) {
                conv.setNeedsVerification(false);
                showVerificationCode();
            } else {
                app.reportError("Niepoprawny kod!");
            }
        });
    }

    @FXML
    void onFingerprintMouseClicked(MouseEvent event) {
        showVerificationCode();
    }

    @FXML
    void onEmojiPopupButtonClick(MouseEvent event) {
        if (emojiPopup.isShowing()) {
            emojiPopup.hide();
        } else {
            updateEmojiPopupPosition(true);
        }
    }

    private void updateEmojiPopupPosition(boolean show) {
        if (emojiPopup.isShowing() || show) {
            Bounds paneBounds = bottomPane.localToScreen(bottomPane.getBoundsInLocal());
            emojiPopup.show(emojiPopupButton, paneBounds.getMaxX() - 5, paneBounds.getMinY());
        }
    }

    private void prepareVerificationDialog(Dialog<?> dialog, SettingsEntry local, UserEntry other) {
        String codeForOther = CryptoUtil.getIdentityVerificationCode(local.getRawPublicKey(), other.getRawPublicKey());
        dialog.setTitle("Conn");
        dialog.setHeaderText("Kod dla " + other.getUsername() + ":\n" + codeForOther);
        dialog.initOwner(app.getStage().getScene().getWindow());
    }

    private void showVerificationCode() {
        Session session = app.getSession();
        if (session == null) {
            return;
        }

        Conversation conv = session.getCurrentConversation();
        if (conv == null) {
            return;
        }

        SettingsEntry local = session.getSettings();
        UserEntry other = conv.getUser();

        Alert alert = new Alert(AlertType.INFORMATION);
        prepareVerificationDialog(alert, local, other);
        alert.getDialogPane().setContentText("Tożsamość " + other.getUsername() + " potwierdzona");
        alert.showAndWait();
    }
}
