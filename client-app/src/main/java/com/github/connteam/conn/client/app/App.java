package com.github.connteam.conn.client.app;

import java.io.IOException;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        LOG.info("Connecting");
        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL).build();

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin() {
                LOG.info("Logged in!");
            }

            @Override
            public void onDisconnect(IOException err) {
                LOG.info("Disconnected: {}", err);
            }
        });

        LOG.info("Authenticating");
        client.start();
    }
}
