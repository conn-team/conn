package com.github.connteam.conn.server.app;

import java.io.IOException;

import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.ConnServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private final static Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        try (ConnServer server = ConnServer.builder().setPort(9090).setTransport(Transport.SSL).build()) {
            LOG.info("Listening");
            server.listen();
        }
    }
}
