package com.github.connteam.conn.client.app;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("-cli")) {
            new CLI().start();
            return;
        }
    }
}
