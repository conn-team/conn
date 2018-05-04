package com.github.connteam.conn.client.app;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.client.database.provider.SqliteDataProvider;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        DataProvider provider = new SqliteDataProvider("/Users/teapot/Documents/Conn/user1.db");
        String username = provider.getSettings().get().getUsername();

        LOG.info("Connecting");
        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL)
                .setIdentity(provider).build();

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin(boolean ok) {
                LOG.info(ok ? "Logged in!" : "Authentication failed!");
                client.sendTextMessage(username, "bye world");
            }

            @Override
            public void onDisconnect(Exception err) {
                LOG.info("Disconnected: {}", err.toString());
            }

			@Override
			public void onTextMessage(String from, String message) {
				LOG.info("{}: {}", from, message);
			}
        });

        LOG.info("Authenticating");
        client.start();
    }
}
