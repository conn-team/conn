package com.github.connteam.conn.server.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.EphemeralKey;

public interface EphemeralKeyProvider {
    Optional<EphemeralKey> getEphemeralKey(int keyId) throws DatabaseException;

    Optional<EphemeralKey> popEphemeralKeyByUserId(int userId) throws DatabaseException;

    List<EphemeralKey> getEphemeralKeysByUserId(int userId) throws DatabaseException;

    int countEphemeralKeysByUserId(int userId) throws DatabaseException;

    int insertEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException;

    boolean updateEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException;

    boolean deleteEphemeralKey(int keyId) throws DatabaseException;

    int deleteEphemeralKeysByUserId(int userId) throws DatabaseException;
}
