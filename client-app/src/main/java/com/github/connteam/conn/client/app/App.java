package com.github.connteam.conn.client.app;

import java.io.IOException;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.ConnClientListener;
import com.github.connteam.conn.core.net.Transport;

public class App {
    public static void main(String[] args) throws IOException {
        System.out.println("Connecting");
        ConnClient client = ConnClient.builder().setHost("localhost").setPort(9090).setTransport(Transport.SSL).build();

        client.setHandler(new ConnClientListener() {
            @Override
            public void onLogin() {
                System.out.println("Logged in!");
            }

            @Override
            public void onDisconnect(IOException err) {
                System.out.println("Disconnected: " + err);
            }
        });

        System.out.println("Authenticating");
        client.start();
    }
}
