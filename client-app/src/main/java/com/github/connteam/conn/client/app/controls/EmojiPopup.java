package com.github.connteam.conn.client.app.controls;

import java.io.IOException;
import java.util.function.Consumer;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.Emoji;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.stage.Popup;

public class EmojiPopup extends Popup {
    private final Node view;
    private final Property<Consumer<Emoji>> onEmojiClick = new SimpleObjectProperty<>();

    @FXML
    private Label emojiCodeLabel;
    @FXML
    private FlowPane emojiPanel;

    public EmojiPopup() {
        try {
            view = App.loadView("views/EmojiView.fxml", this);
            this.getContent().add(view);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setAutoFix(true);
        setAutoHide(true);
        setAnchorLocation(AnchorLocation.WINDOW_BOTTOM_RIGHT);
    }

    @FXML
    private void initialize() {
        for (Emoji emoji : Emoji.getEmojis()) {
            HBox box = new HBox();
            box.setAlignment(Pos.CENTER);
            box.setPrefWidth(32);
            box.setPrefHeight(32);
            box.getStyleClass().add("emoji-button");

            ImageView img = new ImageView(emoji.getImage());
            img.setFitWidth(24);
            img.setFitHeight(24);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            img.setCache(true);

            box.getChildren().add(img);
            emojiPanel.getChildren().add(box);

            box.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                getOnEmojiClick().accept(emoji);
            });
        }
    }

    public void show(Node node) {
        Bounds bounds = node.localToScreen(node.getBoundsInLocal());
        super.show(node, bounds.getMaxX(), bounds.getMinY());
    }

    public Consumer<Emoji> getOnEmojiClick() {
        return onEmojiClick.getValue();
    }

    public void setOnEmojiClick(Consumer<Emoji> run) {
        onEmojiClick.setValue(run);
    }

    public Property<Consumer<Emoji>> onEmojiClickProperty() {
        return onEmojiClick;
    }
}
