package com.github.connteam.conn.server.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.EphemeralKeyEntry;

public interface EphemeralKeyProvider {
    Optional<EphemeralKeyEntry> getEphemeralKey(int keyId) throws DatabaseException;

    Optional<EphemeralKeyEntry> popEphemeralKeyByUserId(int userId) throws DatabaseException;

    List<EphemeralKeyEntry> getEphemeralKeysByUserId(int userId) throws DatabaseException;

    int countEphemeralKeysByUserId(int userId) throws DatabaseException;

    int insertEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException;

    boolean updateEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException;

    boolean deleteEphemeralKey(int keyId) throws DatabaseException;

    int deleteEphemeralKeysByUserId(int userId) throws DatabaseException;
}
