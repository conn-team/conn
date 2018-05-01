package com.github.connteam.conn.client.app;

import java.io.IOException;

import com.github.connteam.conn.client.net.NetClient;
import com.github.connteam.conn.client.net.NetClientHandler;

public class App {
    public static void main(String[] args) throws IOException {
        System.out.println("Connecting");
        NetClient client = NetClient.connect("127.0.0.1", 9090);

        client.setHandler(new NetClientHandler() {
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
        client.login("admin123");
    }
}
