package com.github.connteam.conn.client.app;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;

import com.github.connteam.conn.client.ConnClient;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.net.Transport;

public class Session {
    public static final String HOST = "localhost";
    public static final int PORT = 9090;
    public static final Transport TRANSPORT = Transport.SSL;

    private final App app;

    public Session(App app) {
        this.app = app;
    }

    public static ConnClient createClient(DataProvider db)
            throws IOException, DatabaseException, InvalidKeySpecException {
        return ConnClient.builder().setHost(HOST).setPort(PORT).setTransport(TRANSPORT).setIdentity(db).build();
    }
}
