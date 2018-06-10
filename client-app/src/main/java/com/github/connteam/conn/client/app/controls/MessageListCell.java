package com.github.connteam.conn.client.app.controls;

import java.io.IOException;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.database.model.MessageEntry;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.Node;

public class MessageListCell extends ListCell<MessageEntry> {
    private final static PseudoClass OUTGOING_PSEUDOCLASS = PseudoClass.getPseudoClass("outgoing");
    private final static PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
    private final static PseudoClass SENDING_PSEUDO_CLASS = PseudoClass.getPseudoClass("sending");

    private final Node view;
    @FXML
    private Label messageLabel;
    @FXML
    private Label timeLabel;

    public MessageListCell() {
        try {
            view = App.loadView("views/MessageListCell.fxml", this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        setPrefWidth(0);
    }

    @Override
    protected void updateItem(MessageEntry elem, boolean empty) {
        super.updateItem(elem, empty);

        if (empty || elem == null) {
            setGraphic(null);
            return;
        }

        setGraphic(view);

        pseudoClassStateChanged(OUTGOING_PSEUDOCLASS, elem.isOutgoing());
        pseudoClassStateChanged(ERROR_PSEUDO_CLASS, elem.getIdMessage() == Conversation.SENDING_ERROR);
        pseudoClassStateChanged(SENDING_PSEUDO_CLASS, elem.getIdMessage() == Conversation.SENDING_MESSAGE);

        messageLabel.setText(elem.getMessage());
        timeLabel.setText(App.formatTime(elem.getTime()));

        // Cannot set this in CSS
        view.setNodeOrientation(elem.isOutgoing() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
    }
}
