package com.github.connteam.conn.server.net;

import java.io.IOException;

public interface NetServerClientHandler {
    void onDisconnect(IOException err);
    boolean onLogin(String username);
}
