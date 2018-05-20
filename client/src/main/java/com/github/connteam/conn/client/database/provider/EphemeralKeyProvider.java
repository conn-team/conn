package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface EphemeralKeyProvider {
    List<EphemeralKeyEntry> getEphemeralKeys() throws DatabaseException;

    Optional<EphemeralKeyEntry> getEphemeralKey(int id) throws DatabaseException;

    Optional<EphemeralKeyEntry> getEphemeralKeyByPublicKey(@NotNull byte[] publicKey) throws DatabaseException;

    int insertEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException;

    boolean deleteEphemeralKey(int id) throws DatabaseException;
}
