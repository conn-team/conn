package com.github.connteam.conn.client.app.controls;

import java.io.IOException;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.database.model.Message;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.Node;

public class MessageListCell extends ListCell<Message> {
    private final static PseudoClass OUTGOING_PSEUDOCLASS = PseudoClass.getPseudoClass("outgoing");

    private final Node view;
    @FXML
    private Label messageLabel;

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
    protected void updateItem(Message elem, boolean empty) {
        super.updateItem(elem, empty);

        if (empty || elem == null) {
            setGraphic(null);
            return;
        }

        setGraphic(view);
        pseudoClassStateChanged(OUTGOING_PSEUDOCLASS, elem.isOutgoing());
        messageLabel.setText(elem.getMessage());
    }
}
