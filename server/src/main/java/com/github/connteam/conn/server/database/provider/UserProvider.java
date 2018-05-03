package com.github.connteam.conn.server.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.User;

public interface UserProvider {
    Optional<User> getUser(int id) throws DatabaseException;

    Optional<User> getUserByUsername(@NotNull String username) throws DatabaseException;

    Optional<Integer> insertUser(@NotNull User user) throws DatabaseException;

    boolean updateUser(@NotNull User user) throws DatabaseException;

    boolean updateUserByUsername(@NotNull String username) throws DatabaseException;

    int deleteUser(int id) throws DatabaseException;

    int deleteUserByUsername(@NotNull String username) throws DatabaseException;
}
