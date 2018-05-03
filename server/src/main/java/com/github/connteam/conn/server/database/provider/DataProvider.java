package com.github.connteam.conn.server.database.provider;

public interface DataProvider
        extends EphemeralKeyProvider, MessageProvider, ObservedProvider, UserProvider, AutoCloseable {
}
