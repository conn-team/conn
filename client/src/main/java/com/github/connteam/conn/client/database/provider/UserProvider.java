package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.User;

public interface UserProvider {
    Optional<User> getUser(int id);

    Optional<User> getUserByUsername(@NotNull String username);

    List<User> getUsers();

    List<User> getVerifiedUsers();

    int insertUser(@NotNull User user);

    boolean deleteUser(int id);

    boolean deleteUserByUsername(@NotNull String username);
}
