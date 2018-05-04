package com.github.connteam.conn.server.app;

import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.ConnServer;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.github.connteam.conn.server.database.provider.PostgresDataProvider;

public class App {
    public static void main(String[] args) throws Exception {
        try (DataProvider provider = new PostgresDataProvider.Builder().setName("conn").setUser("conn").setPassword("")
                .build()) {
            try (ConnServer server = ConnServer.builder().setPort(9090).setTransport(Transport.SSL)
                    .setDataProvider(provider).build()) {
                server.listen();
            }
        }
    }
}
