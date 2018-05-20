package com.github.connteam.conn.server.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.UserEntry;

public interface UserProvider {
    Optional<UserEntry> getUser(int id) throws DatabaseException;

    Optional<UserEntry> getUserByUsername(@NotNull String username) throws DatabaseException;

    int insertUser(@NotNull UserEntry user) throws DatabaseException;

    boolean updateUser(@NotNull UserEntry user) throws DatabaseException;

    boolean updateUserByUsername(@NotNull UserEntry username) throws DatabaseException;

    boolean deleteUser(int id) throws DatabaseException;

    boolean deleteUserByUsername(@NotNull String username) throws DatabaseException;
}
