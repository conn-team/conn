package com.github.connteam.conn.client.app.util;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import javafx.scene.control.Toggle;

public class ToggleBinder<T> {
    private final Property<T> property = new SimpleObjectProperty<>();

    private final Map<T, Toggle> fromValues = new HashMap<>();
    private final Map<Toggle, T> toValues = new HashMap<>();

    public ToggleBinder() {
        property.addListener((prop, old, cur) -> {
            if (cur != null) {
                Toggle target = fromValues.get(cur);
                if (target != null) {
                    target.setSelected(true);
                }
            }

            for (Map.Entry<Toggle, T> entry : toValues.entrySet()) {
                if (entry.getValue() != cur) {
                    entry.getKey().setSelected(false);
                }
            }
        });
    }

    public void addToggle(Toggle toggle, T value) {
        if (fromValues.containsKey(value) || toValues.containsKey(toggle)) {
            throw new IllegalArgumentException();
        }

        fromValues.put(value, toggle);
        toValues.put(toggle, value);

        toggle.selectedProperty().addListener((prop, old, cur) -> {
            if (property.getValue() != value) {
                if (cur) {
                    property.setValue(value);
                }
            } else if (!cur) {
                toggle.setSelected(true);
            }
        });
    }

    public Property<T> getProperty() {
        return property;
    }
}
