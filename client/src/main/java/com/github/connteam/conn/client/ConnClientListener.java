package com.github.connteam.conn.client;

public interface ConnClientListener {
    default void onDisconnect(Exception err) {}
    default void onLogin(boolean hasBeenRegistered) {}
    default void onTextMessage(String from, String message) {}
}
