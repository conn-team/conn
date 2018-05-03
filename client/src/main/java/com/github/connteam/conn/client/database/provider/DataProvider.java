package com.github.connteam.conn.client.database.provider;

public interface DataProvider
        extends EphemeralKeyProvider, FriendProvider, MessageProvider, SettingsProvider, UserProvider, AutoCloseable {
}