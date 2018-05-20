package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface MessageProvider {
    List<MessageEntry> getMessageFrom(int idFrom) throws DatabaseException;

    List<MessageEntry> getMessageTo(int idFrom) throws DatabaseException;

    Optional<MessageEntry> getMessage(int idMessage) throws DatabaseException;

    int insertMessage(@NotNull MessageEntry message) throws DatabaseException;

    boolean updateMessage(@NotNull MessageEntry message) throws DatabaseException;

    int deleteMessagesFrom(int idFrom) throws DatabaseException;

    int deleteMessagesTo(int idTo) throws DatabaseException;
}
