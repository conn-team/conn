package com.github.connteam.conn.client;

public interface ConnClientListener {
    void onDisconnect(Exception err);
    void onLogin(boolean hasBeenRegistered);
    void onTextMessage(String from, String message);
}
