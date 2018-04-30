package com.github.connteam.conn.client.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.User;

public interface UserProvider {
    Optional<User> selectById(int id);

    Optional<User> selectByUsername(@NotNull String username);

    Stream<User> selectAll();

    Stream<User> selectVerified();

    Optional<Integer> insert(@NotNull User user);

    boolean updateUser(@NotNull User user);

    boolean deleteById(int id);

    boolean deleteByUsername(@NotNull String username);
}
