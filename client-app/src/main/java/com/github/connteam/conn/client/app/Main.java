package com.github.connteam.conn.client.app;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("-cli")) {
            new CLI().start();
        } else {
            Application.launch(App.class, args);
        }
    }
}
