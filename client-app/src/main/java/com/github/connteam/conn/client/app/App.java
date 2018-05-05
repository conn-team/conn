package com.github.connteam.conn.client.app;

import com.budhash.cliche.Command;
import com.budhash.cliche.ShellFactory;
import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.client.database.provider.SqliteDataProvider;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    private DataProvider database;
    private ConnClient client;

    public static void main(String[] args) throws Exception {
        ShellFactory.createConsoleShell("conn", "conn-client", new App()).commandLoop();;
    }

    @Command
    public void login(String identity) throws Exception {
        logout();

        DataProvider database = new SqliteDataProvider(System.getProperty("user.home") + "/" + identity);
        this.database = database;

        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL)
                .setIdentity(database).build();
        this.client = client;

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin(boolean ok) {
                LOG.info(ok ? "Logged in!" : "Authentication failed!");
            }

            @Override
            public void onDisconnect(Exception err) {
                LOG.info("Disconnected: {}", err.toString());
                try {
					database.close();
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
            }

			@Override
			public void onTextMessage(String from, String message) {
				LOG.info("{}: {}", from, message);
			}
        });

        client.start();
    }

    @Command
    public void logout() {
        if (client != null) {
            client.close();
            client = null;
        }
        if (database != null) {
            try {
                database.close();
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            database = null;
        }
    }

    @Command
    public void send(String to, String msg) {
        if (client != null) {
            client.sendTextMessage(to, msg);
        }
    }
}
