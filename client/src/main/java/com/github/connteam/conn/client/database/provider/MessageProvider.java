package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.core.database.DatabaseException;

public interface MessageProvider {
    List<MessageEntry> getMessages(int idFrom) throws DatabaseException;

    List<MessageEntry> getMessagesPage(int idFrom, int count, int maxID) throws DatabaseException;

    List<MessageEntry> getMessagesFrom(int idFrom) throws DatabaseException;

    List<MessageEntry> getMessagesTo(int idFrom) throws DatabaseException;

    Optional<MessageEntry> getMessage(int idMessage) throws DatabaseException;

    int insertMessage(@NotNull MessageEntry message) throws DatabaseException;

    boolean deleteMessage(int idMessage) throws DatabaseException;

    boolean updateMessage(@NotNull MessageEntry message) throws DatabaseException;

    int deleteMessagesFrom(int idFrom) throws DatabaseException;

    int deleteMessagesTo(int idTo) throws DatabaseException;
}
