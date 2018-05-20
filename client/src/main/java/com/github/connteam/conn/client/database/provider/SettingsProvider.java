package com.github.connteam.conn.client.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface SettingsProvider {
    Optional<SettingsEntry> getSettings() throws DatabaseException;

    boolean setSettings(@NotNull SettingsEntry settings) throws DatabaseException;
}
