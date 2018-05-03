package com.github.connteam.conn.server.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.User;

public interface UserProvider {
    Optional<User> getUser(int id) throws DatabaseException;

    Optional<User> getUserByUsername(@NotNull String username) throws DatabaseException;

    int insertUser(@NotNull User user) throws DatabaseException;

    boolean updateUser(@NotNull User user) throws DatabaseException;

    boolean updateUserByUsername(@NotNull User username) throws DatabaseException;

    boolean deleteUser(int id) throws DatabaseException;

    boolean deleteUserByUsername(@NotNull String username) throws DatabaseException;
}
