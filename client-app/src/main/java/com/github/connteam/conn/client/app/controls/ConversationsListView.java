package com.github.connteam.conn.client.app.controls;

import java.io.IOException;
import java.util.function.Predicate;

import com.github.connteam.conn.client.app.App;
import com.github.connteam.conn.client.app.model.Conversation;
import com.github.connteam.conn.client.database.model.UserEntry;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

public class ConversationsListView extends VBox {
    @FXML
    private TextField conversationsSearch;
    @FXML
    private TabPane tabPane;
    @FXML
    private ListView<Conversation> conversationsListView;

    private final Property<ObservableList<Conversation>> conversations = new SimpleObjectProperty<>();
    private final Property<Conversation> currentConversation = new SimpleObjectProperty<>();

    private final Property<Predicate<Conversation>> filter = new SimpleObjectProperty<>();

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
        tabPane.tabMinWidthProperty().bind(tabPane.widthProperty().divide(tabPane.getTabs().size()).subtract(20));
        filter.setValue(x -> true);

        conversationsListView.setCellFactory(x -> new ConversationListCell());

        conversations.addListener((prop, old, cur) -> {
            if (cur != null) {
                FilteredList<Conversation> all = new FilteredList<Conversation>(cur);
                all.predicateProperty().bind(filter);

                conversationsListView.setItems(all);
            } else {
                conversationsListView.setItems(null);
            }
        });

        currentConversation.addListener((prop, old, cur) -> {
            conversationsListView.getSelectionModel().select(cur);
        });

        conversationsListView.getSelectionModel().selectedItemProperty().addListener((prop, old, cur) -> {
            if (old != cur) {
                if (cur != null) {
                    setCurrentItem(cur);
                } else {
                    conversationsListView.getSelectionModel().select(old);
                }
            }
        });

        conversationsSearch.textProperty().addListener((prop, old, cur) -> updateFilter());
        tabPane.getSelectionModel().selectedItemProperty().addListener((prop, old, cur) -> updateFilter());
    }

    private void updateFilter() {
        String pattern = conversationsSearch.getText();
        boolean checkFriend = (tabPane.getSelectionModel().getSelectedIndex() == 1); // meh

        filter.setValue(conv -> {
            UserEntry user = conv.getUser();
            return user.getUsername().toLowerCase().contains(pattern) && (!checkFriend || user.isFriend());
        });
    }
}
