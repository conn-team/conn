package com.github.connteam.conn.client;

import java.security.KeyPair;

import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.provider.DataProvider;
import com.github.connteam.conn.client.database.provider.SqliteDataProvider;
import com.github.connteam.conn.core.crypto.CryptoUtil;
import com.github.connteam.conn.core.database.DatabaseException;

public final class IdentityFactory {
    private IdentityFactory() {
    }

    public static DataProvider load(String path) throws DatabaseException {
        SqliteDataProvider db = new SqliteDataProvider(path);
        db.getSettings().orElseThrow(() -> new DatabaseException("Missing identity settings"));
        return db;
    }

    public static DataProvider create(String path, String username) throws DatabaseException {
        SqliteDataProvider db = new SqliteDataProvider(path);
        db.createTables();

        KeyPair keys = CryptoUtil.generateKeyPair();

        SettingsEntry settings = new SettingsEntry();
        settings.setUsername(username);
        settings.setPublicKey(keys.getPublic());
        settings.setPrivateKey(keys.getPrivate());

        db.setSettings(settings);
        return db;
    }
}
