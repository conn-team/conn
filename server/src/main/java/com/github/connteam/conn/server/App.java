package com.github.connteam.conn.server;

import java.io.IOException;

import com.github.connteam.conn.core.net.Transport;

public class App {
    public static void main(String[] args) throws IOException {
        try (ConnServer server = ConnServer.builder().setPort(9090).setTransport(Transport.SSL).build()) {
            System.out.println("Listening");
            server.listen();
        }
    }
}
