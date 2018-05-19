package com.github.connteam.conn.client.database.provider;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.UsedEphemeralKey;
import com.github.connteam.conn.core.database.DatabaseException;

public interface UsedEphemeralKeyProvider {
    List<UsedEphemeralKey> getUsedEphemeralKeys() throws DatabaseException;

    boolean isUsedEphemeralKey(@NotNull UsedEphemeralKey key) throws DatabaseException;

    void insertUsedEphemeralKey(@NotNull UsedEphemeralKey key) throws DatabaseException;

    boolean deleteUsedEphemeralKey(@NotNull UsedEphemeralKey key) throws DatabaseException;
}