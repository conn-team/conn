package com.github.connteam.conn.client;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.net.proto.NetProtos.UserStatus;

public interface ConnClientListener {
    default void onDisconnect(Exception err) {
    }

    default void onLogin(boolean hasBeenRegistered) {
    }

    default void onTextMessage(UserEntry from, MessageEntry msg) {
    }

    default void onStatusChange(UserEntry user, UserStatus status) {
    }
}
