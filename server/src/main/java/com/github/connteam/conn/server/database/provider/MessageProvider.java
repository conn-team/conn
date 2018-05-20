package com.github.connteam.conn.server.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.server.database.model.MessageEntry;

public interface MessageProvider {
    Optional<MessageEntry> getMessage(int idMessage) throws DatabaseException;

    List<MessageEntry> getMessagesFrom(int idFrom) throws DatabaseException;

    List<MessageEntry> getMessagesTo(int idTo) throws DatabaseException;

    int insertMessage(@NotNull MessageEntry message) throws DatabaseException;

    boolean updateMessage(@NotNull MessageEntry message) throws DatabaseException;

    boolean deleteMessage(int idMessage) throws DatabaseException;

    int deleteMessagesFrom(int idFrom) throws DatabaseException;

    int deleteMessagesTo(int idTo) throws DatabaseException;
}
