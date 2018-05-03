package com.github.connteam.conn.client.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Settings;

public interface SettingsProvider {
    Optional<Settings> getSettings();

    boolean setSettings(@NotNull Settings settings);
}
