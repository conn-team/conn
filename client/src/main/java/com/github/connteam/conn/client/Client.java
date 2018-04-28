package com.github.connteam.conn.client;

import com.github.connteam.conn.core.HelloFactory;

public class Client {
    public static String makeHello() {
        return HelloFactory.makeHello("client");
    }
}
