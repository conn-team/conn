package com.github.connteam.conn.client.app.controls;

import java.io.IOException;
import java.util.function.Predicate;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

public class ConversationsListView extends VBox {
    @FXML
    private TextField conversationsSearch;
    @FXML
    private ListView<Conversation> allConversationsListView;
    @FXML
    private ListView<Conversation> friendsListView;

    private final Property<ObservableList<Conversation>> conversations = new SimpleObjectProperty<>();
    private final Property<Conversation> currentConversation = new SimpleObjectProperty<>();

    private final Property<Predicate<Conversation>> allFilter = new SimpleObjectProperty<>();

    public ConversationsListView() {
        try {
            App.loadView("views/ConversationsListView.fxml", this, this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ObservableList<Conversation> getItems() {
        return conversations.getValue();
    }

    public void setItems(ObservableList<Conversation> val) {
        conversations.setValue(val);
    }

    public Property<ObservableList<Conversation>> itemsProperty() {
        return conversations;
    }

    public Conversation getCurrentItem() {
        return currentConversation.getValue();
    }

    public void setCurrentItem(Conversation val) {
        currentConversation.setValue(val);
    }

    public Property<Conversation> currentItemProperty() {
        return currentConversation;
    }

    @FXML
    public void initialize() {
        allFilter.setValue(x -> true);

        allConversationsListView.setCellFactory(x -> new ConversationListCell());
        friendsListView.setCellFactory(x -> new ConversationListCell());

        conversations.addListener((prop, old, cur) -> {
            if (cur != null) {
                FilteredList<Conversation> all = new FilteredList<Conversation>(cur);
                all.predicateProperty().bind(allFilter);

                allConversationsListView.setItems(all);
                friendsListView.setItems(new FilteredList<Conversation>(all, x -> x.getUser().isFriend()));
            } else {
                allConversationsListView.setItems(null);
                friendsListView.setItems(null);
            }
        });

        currentConversation.addListener((prop, old, cur) -> {
            allConversationsListView.getSelectionModel().select(cur);
            friendsListView.getSelectionModel().select(cur);
        });

        ChangeListener<Conversation> selectedListener = (prop, old, cur) -> {
            if (old != cur) {
                if (cur != null) {
                    setCurrentItem(cur);
                } else {
                    allConversationsListView.getSelectionModel().select(old);
                    friendsListView.getSelectionModel().select(old);
                }
            }
        };

        allConversationsListView.getSelectionModel().selectedItemProperty().addListener(selectedListener);
        friendsListView.getSelectionModel().selectedItemProperty().addListener(selectedListener);

        conversationsSearch.textProperty().addListener((prop, old, cur) -> {
            allFilter.setValue(conv -> conv.getUser().getUsername().toLowerCase().contains(cur.toLowerCase()));
        });
    }
}
