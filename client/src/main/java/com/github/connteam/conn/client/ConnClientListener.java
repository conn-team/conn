package com.github.connteam.conn.client;

import com.github.connteam.conn.client.database.model.User;

public interface ConnClientListener {
    default void onDisconnect(Exception err) {
    }

    default void onLogin(boolean hasBeenRegistered) {
    }

    default void onTextMessage(User from, String message) {
    }
}
