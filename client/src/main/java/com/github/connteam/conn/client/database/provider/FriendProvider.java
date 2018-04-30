package com.github.connteam.conn.client.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Friend;

public interface FriendProvider {
    Stream<Friend> select();

    Optional<Friend> selectById(int id);

    Optional<Integer> insert(@NotNull Friend friend);

    boolean deleteById(int id);
}
