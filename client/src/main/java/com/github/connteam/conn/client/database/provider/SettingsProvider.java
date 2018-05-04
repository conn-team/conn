package com.github.connteam.conn.client.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.core.database.DatabaseException;

public interface SettingsProvider {
    Optional<Settings> getSettings() throws DatabaseException;

    boolean setSettings(@NotNull Settings settings) throws DatabaseException;
}
