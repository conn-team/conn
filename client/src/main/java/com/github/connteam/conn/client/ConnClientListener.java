package com.github.connteam.conn.client;

import java.io.IOException;

public interface ConnClientListener {
    void onDisconnect(IOException err);
    void onLogin();
}
