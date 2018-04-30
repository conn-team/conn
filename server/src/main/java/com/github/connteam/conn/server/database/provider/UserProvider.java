package com.github.connteam.conn.server.database.provider;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.exception.DatabaseException;
import com.github.connteam.conn.server.database.model.User;

public interface UserProvider {
    Optional<User> selectById(int id) throws DatabaseException;

    Optional<User> selectByUsername(@NotNull String username) throws DatabaseException;

    Optional<Integer> insert(@NotNull User user) throws DatabaseException;

    boolean updateById(@NotNull User user) throws DatabaseException;

    boolean updateByUsername(@NotNull String username) throws DatabaseException;

    boolean deleteById(int id) throws DatabaseException;

    boolean deleteByUsername(@NotNull String username) throws DatabaseException;
}
