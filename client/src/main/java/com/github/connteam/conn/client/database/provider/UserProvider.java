package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface UserProvider {
    Optional<UserEntry> getUser(int id) throws DatabaseException;

    Optional<UserEntry> getUserByUsername(@NotNull String username) throws DatabaseException;

    List<UserEntry> getUsers() throws DatabaseException;

    List<UserEntry> getVerifiedUsers() throws DatabaseException;

    List<UserEntry> getFriends() throws DatabaseException;

    int insertUser(@NotNull UserEntry user) throws DatabaseException;

    boolean updateUser(@NotNull UserEntry message) throws DatabaseException;

    boolean deleteUser(int id) throws DatabaseException;

    boolean deleteUserByUsername(@NotNull String username) throws DatabaseException;
}
