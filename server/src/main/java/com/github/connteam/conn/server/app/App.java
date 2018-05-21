package com.github.connteam.conn.server.app;

import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.ConnServer;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.github.connteam.conn.server.database.provider.PostgresDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static final String DB_NAME = "conn";
    public static final String DB_USER_NAME = "conn";
    public static final String DB_PASSWORD = "";

    public static final int PORT = 7312;
    public static final Transport TRANSPORT = Transport.SSL;

    public static void main(String[] args) throws Exception {
        try (DataProvider provider = new PostgresDataProvider.Builder().setName(DB_NAME).setUser(DB_USER_NAME)
                .setPassword(DB_PASSWORD).build()) {

            LOG.info("Creating tables");
            provider.createTables();

            try (ConnServer server = ConnServer.builder().setPort(PORT).setTransport(TRANSPORT)
                    .setDataProvider(provider).build()) {
                server.listen();
            }
        }
    }
}
