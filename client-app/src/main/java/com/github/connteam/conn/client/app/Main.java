package com.github.connteam.conn.client.app;

import com.github.connteam.conn.client.app.model.Session;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            Session.setHost(args[0]);
            if (args.length > 1) {
                Session.setPort(Integer.parseInt(args[1]));
            }
        }

        Application.launch(App.class, args);
    }
}
