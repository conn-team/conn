package com.github.connteam.conn.server.database.provider;

import com.github.connteam.conn.core.database.DatabaseException;

public interface DataProvider
        extends EphemeralKeyProvider, MessageProvider, UserProvider, AutoCloseable, TableProvider {
    void close() throws DatabaseException;
}
