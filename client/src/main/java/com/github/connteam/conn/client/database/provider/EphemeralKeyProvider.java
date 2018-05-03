package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKey;

public interface EphemeralKeyProvider {
    List<EphemeralKey> getEphemeralKeys();

    Optional<EphemeralKey> getEphemeralKey(int id);

    int insertEphemeralKey(@NotNull EphemeralKey key);

    boolean deleteEphemeralKey(int id);

    Optional<EphemeralKey> popEphemeralKey();
}
