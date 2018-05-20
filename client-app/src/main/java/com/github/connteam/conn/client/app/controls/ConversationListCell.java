package com.github.connteam.conn.client.app.controls;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.app.util.DeepObserver;
import com.github.connteam.conn.client.database.model.MessageEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.Node;

public class ConversationListCell extends ListCell<Conversation> {
    private final static PseudoClass NONEMPTY_PSEUDOCLASS = PseudoClass.getPseudoClass("nonempty");

    private final Node view;
    private final Property<Conversation> conversation = new SimpleObjectProperty<>();

    @FXML
    private Label usernameField;
    @FXML
    private Label lastMessageField;
    @FXML
    private Label timeField;

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
        DeepObserver.listen(conversation, (ctx, old, cur) -> {
            pseudoClassStateChanged(NONEMPTY_PSEUDOCLASS, cur != null);
            if (cur == null) {
                setGraphic(null);
                return;
            }

            setGraphic(view);
            usernameField.setText(cur.getUser().getUsername());
            lastMessageField.setText("");
            timeField.setText("");

            ctx.listen(cur.getMessages(), change -> {
                ObservableList<? extends MessageEntry> list = change.getList();

                if (list.isEmpty()) {
                    lastMessageField.setText("");
                    timeField.setText("");
                } else {
                    MessageEntry msg = list.get(list.size() - 1);

                    String txt = (msg.isOutgoing() ? "Ty: " : "") + msg.getMessage();
                    if (txt.length() >= 15 + 3) {
                        txt = txt.substring(0, 15) + "...";
                    }

                    lastMessageField.setText(txt);
                    timeField.setText(formatTime(msg.getTime()));
                }
            });
        });

        setPrefWidth(0);
    }

    private String formatTime(Date time) {
        long days = (new Date().getTime() - time.getTime()) / 1000 / 60 / 60 / 24;

        if (days < 1) {
            return new SimpleDateFormat("HH:mm").format(time);
        } else if (days < 365) {
            return new SimpleDateFormat("dd.MM").format(time);
        } else {
            return new SimpleDateFormat("dd.MM.yyyy").format(time);
        }
    }
}
