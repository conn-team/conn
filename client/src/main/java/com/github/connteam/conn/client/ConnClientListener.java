package com.github.connteam.conn.client;

import com.github.connteam.conn.client.database.model.UserEntry;

public interface ConnClientListener {
    default void onDisconnect(Exception err) {
    }

    default void onLogin(boolean hasBeenRegistered) {
    }

    default void onTextMessage(UserEntry from, String message) {
    }
}
