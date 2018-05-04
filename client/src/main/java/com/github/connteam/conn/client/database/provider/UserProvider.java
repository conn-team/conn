package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.core.database.DatabaseException;

public interface UserProvider {
    Optional<User> getUser(int id) throws DatabaseException;

    Optional<User> getUserByUsername(@NotNull String username) throws DatabaseException;

    List<User> getUsers() throws DatabaseException;

    List<User> getVerifiedUsers() throws DatabaseException;

    int insertUser(@NotNull User user) throws DatabaseException;

    boolean deleteUser(int id) throws DatabaseException;

    boolean deleteUserByUsername(@NotNull String username) throws DatabaseException;
}
