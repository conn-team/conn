package com.github.connteam.conn.client.app;

import java.io.IOException;

import com.budhash.cliche.Command;
import com.budhash.cliche.ShellFactory;
import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.client.IdentityFactory;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLI {
    private final static Logger LOG = LoggerFactory.getLogger(CLI.class);

    private DataProvider database;
    private ConnClient client;

    public void start() throws IOException {
        ShellFactory.createConsoleShell("conn", "conn-client", this).commandLoop();
    }

    @Command
    public void login(String username) throws Exception {
        logout();

        String path = System.getProperty("user.home") + "/" + username + ".db";

        try {
            database = IdentityFactory.load(path);
        } catch (DatabaseException e) {
            database = IdentityFactory.create(path, username);
        }

        DataProvider db = database;

        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL)
                .setIdentity(db).build();
        this.client = client;

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin(boolean hasBeenRegistered) {
                LOG.info("Logged in!" + (hasBeenRegistered ? " (new account)" : " (existing account)"));
            }

            @Override
            public void onDisconnect(Exception err) {
                LOG.info("Disconnected: {}", (err != null ? err.toString() : null));
                try {
                    db.close();
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
