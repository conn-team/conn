package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.core.database.DatabaseException;

public interface EphemeralKeyProvider {
    List<EphemeralKey> getEphemeralKeys() throws DatabaseException;

    Optional<EphemeralKey> getEphemeralKey(int id) throws DatabaseException;

    int insertEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException;

    boolean deleteEphemeralKey(int id) throws DatabaseException;

    Optional<EphemeralKey> popEphemeralKey() throws DatabaseException;
}
