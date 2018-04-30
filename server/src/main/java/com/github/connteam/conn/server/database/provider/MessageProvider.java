package com.github.connteam.conn.server.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.exception.DatabaseException;
import com.github.connteam.conn.server.database.model.Message;

public interface MessageProvider {
    Optional<Message> selectByIdMessage(int idMessage) throws DatabaseException;

    Stream<Message> selectByIdFrom(int idFrom) throws DatabaseException;

    Stream<Message> selectByIdTo(int idTo) throws DatabaseException;

    Optional<Integer> insert(@NotNull Message message) throws DatabaseException;

    boolean update(@NotNull Message message) throws DatabaseException;

    boolean deleteByMessageId(int idMessage) throws DatabaseException;

    boolean deleteByIdFrom(int idFrom) throws DatabaseException;

    boolean deleteByIdTo(int idTo) throws DatabaseException;
}
