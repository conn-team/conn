package com.github.connteam.conn.server.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.Message;

public interface MessageProvider {
    Optional<Message> getMessage(int idMessage) throws DatabaseException;

    List<Message> getMessagesFrom(int idFrom) throws DatabaseException;

    List<Message> getMessagesTo(int idTo) throws DatabaseException;

    Optional<Integer> insertMessage(@NotNull Message message) throws DatabaseException;

    boolean updateMessage(@NotNull Message message) throws DatabaseException;

    boolean deleteMessage(int idMessage) throws DatabaseException;

    int deleteMessagesFrom(int idFrom) throws DatabaseException;

    int deleteMessagesTo(int idTo) throws DatabaseException;
}
