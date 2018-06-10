package com.github.connteam.conn.client.app.controls;

import java.io.IOException;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.util.DeepObserver;
import com.github.connteam.conn.client.database.model.MessageEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;

public class ConversationListCell extends ListCell<Conversation> {
    private final static PseudoClass NONEMPTY_PSEUDOCLASS = PseudoClass.getPseudoClass("nonempty");
    private final static PseudoClass UNREAD_PSEUDOCLASS = PseudoClass.getPseudoClass("unread");
    private final static PseudoClass FRIEND_PSEUDOCLASS = PseudoClass.getPseudoClass("friend");

    private final Node view;
    private final Property<Conversation> conversation = new SimpleObjectProperty<>();

    @FXML
    private Label usernameField;
    @FXML
    private Label lastMessageField;
    @FXML
    private Label timeField;

    private ContextMenu menu;
    private MenuItem toggleFriendMenuItem, resetConversationMenuItem;

    public ConversationListCell() {
        try {
            view = App.loadView("views/ConversationListCell.fxml", this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void updateItem(Conversation elem, boolean empty) {
        super.updateItem(elem, empty);
        conversation.setValue(empty ? null : elem);
    }

    @FXML
    private void initialize() {
        setPrefWidth(0);

        menu = new ContextMenu();
        toggleFriendMenuItem = new MenuItem();
        menu.getItems().add(toggleFriendMenuItem);

        resetConversationMenuItem = new MenuItem("Zresetuj konwersację");
        menu.getItems().add(resetConversationMenuItem);

        toggleFriendMenuItem.setOnAction(event -> {
            if (getItem() != null) {
                getItem().toggleFriend();
            }
        });

        resetConversationMenuItem.setOnAction(event -> {
            Conversation conv = conversation.getValue();
            if (conv == null) {
                return;
            }

            Alert alert = new Alert(AlertType.WARNING,
                    "Czy na pewno chcesz zresetować konwersację z " + conv.getUser().getUsername() + "?",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("Conn");

            alert.showAndWait().ifPresent(state -> {
                if (state == ButtonType.YES) {
                    conv.resetConversation();
                }
            });
        });

        DeepObserver.listen(conversation, (ctx, old, cur) -> {
            pseudoClassStateChanged(NONEMPTY_PSEUDOCLASS, cur != null);
            if (cur == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            setGraphic(view);
            setContextMenu(menu);

            usernameField.setText(cur.getUser().getUsername());
            lastMessageField.setText("");
            timeField.setText("");

            pseudoClassStateChanged(FRIEND_PSEUDOCLASS, cur.getUser().isFriend());
            toggleFriendMenuItem.setText(cur.getUser().isFriend() ? "Usuń z ulubionych" : "Dodaj do ulubionych");

            ctx.listen(cur.getMessages(), change -> {
                if (cur != getItem()) {
                    return;
                }

                ObservableList<? extends MessageEntry> list = change.getList();

                if (list.isEmpty()) {
                    lastMessageField.setText("");
                    timeField.setText("");
                } else {
                    MessageEntry msg = list.get(list.size() - 1);

                    String txt = (msg.isOutgoing() ? "Ty: " : "") + msg.getMessage().replace('\n', ' ');
                    if (txt.length() >= 15 + 3) {
                        txt = txt.substring(0, 15) + "...";
                    }

                    lastMessageField.setText(txt);
                    timeField.setText(App.formatTime(msg.getTime()));
                }
            });

            ctx.listen(cur.unreadProperty(), (prop, oldVal, curVal) -> {
                pseudoClassStateChanged(UNREAD_PSEUDOCLASS, curVal != null && curVal);
            });
        });
    }
}
