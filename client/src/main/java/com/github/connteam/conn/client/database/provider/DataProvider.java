package com.github.connteam.conn.client.database.provider;

import com.github.connteam.conn.core.database.DatabaseException;

public interface DataProvider
        extends EphemeralKeyProvider, FriendProvider, MessageProvider, SettingsProvider, UserProvider, AutoCloseable {
    void close() throws DatabaseException;
}