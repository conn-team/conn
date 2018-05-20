package com.github.connteam.conn.client.database.provider;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.UsedEphemeralKeyEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface UsedEphemeralKeyProvider {
    List<UsedEphemeralKeyEntry> getUsedEphemeralKeys() throws DatabaseException;

    boolean isUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException;

    void insertUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException;

    boolean deleteUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException;
}