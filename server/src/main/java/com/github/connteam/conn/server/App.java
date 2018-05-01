package com.github.connteam.conn.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.connteam.conn.server.net.NetServer;
import com.github.connteam.conn.server.net.NetServerClient;
import com.github.connteam.conn.server.net.NetServerClientHandler;

public class App {
    static AtomicInteger count = new AtomicInteger();

    public static void main(String[] args) throws IOException {
        try (NetServer server = NetServer.listen(9090)) {
            System.out.println("Listening");
            while (true) {
                handle(server.accept());
            }
        }
    }

    public static void handle(NetServerClient client) {
        int id = count.incrementAndGet();
        System.out.println(id + " connected");

        client.setHandler(new NetServerClientHandler(){
            @Override
            public boolean onLogin(String username) {
                System.out.println(id + " authenticated: " + username);
                return true;
            }
        
            @Override
            public void onDisconnect(IOException err) {
                System.out.println(id + " disconnected: " + err);
            }
        });

        client.handle();
    }
}
