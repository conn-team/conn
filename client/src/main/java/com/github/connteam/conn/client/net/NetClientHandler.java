package com.github.connteam.conn.client.net;

import java.io.IOException;

public interface NetClientHandler {
    void onDisconnect(IOException err);
    void onLogin();
}
