package com.github.connteam.conn.server;

import com.github.connteam.conn.core.HelloFactory;

public class App {
    public static void main(String[] args) {
        System.out.println(HelloFactory.makeHello("server"));
    }
}
