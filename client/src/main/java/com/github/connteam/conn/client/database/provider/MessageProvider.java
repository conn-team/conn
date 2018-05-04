package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.core.database.DatabaseException;

public interface MessageProvider {
    List<Message> getMessageFrom(int idFrom) throws DatabaseException;

    List<Message> getMessageTo(int idFrom) throws DatabaseException;

    Optional<Message> getMessage(int idMessage) throws DatabaseException;

    int insertMessage(@NotNull Message message) throws DatabaseException;

    boolean updateMessage(@NotNull Message message) throws DatabaseException;

    int deleteMessageFrom(int idFrom) throws DatabaseException;

    int deleteMessagesTo(int idTo) throws DatabaseException;
}
