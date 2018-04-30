package com.github.connteam.conn.client.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKey;

public interface EphemeralKeyProvider {
    Stream<EphemeralKey> select();

    Optional<EphemeralKey> selectById(int id);

    Optional<Integer> insert(@NotNull EphemeralKey key);

    boolean deleteById(int id);

    Optional<EphemeralKey> popKey();
}
