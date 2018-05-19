package com.github.connteam.conn.client.database.provider;

import com.github.connteam.conn.core.database.DatabaseException;

public interface DataProvider
        extends EphemeralKeyProvider, MessageProvider, SettingsProvider, UserProvider, AutoCloseable, TableProvider {
    void close() throws DatabaseException;
}