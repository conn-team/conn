package com.github.connteam.conn.server.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.EphemeralKey;

public interface EphemeralKeyProvider {
    Stream<EphemeralKey> selectByUserId(int userId) throws DatabaseException;

    Optional<EphemeralKey> selectByKeyId(int keyId) throws DatabaseException;

    Optional<Integer> insert(@NotNull EphemeralKey key) throws DatabaseException;

    boolean update(@NotNull EphemeralKey key) throws DatabaseException;

    boolean deleteByUserId(int userId) throws DatabaseException;

    boolean deleteByKeyId(int keyId) throws DatabaseException;
}
