package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Message;

public interface MessageProvider {
    List<Message> getMessageFrom(int idFrom);

    List<Message> getMessageTo(int idFrom);

    Optional<Message> getMessage(int idMessage);

    int insertMessage(@NotNull Message message);

    boolean updateMessage(@NotNull Message message);

    int deleteMessageFrom(int idFrom);

    int deleteMessagesTo(int idTo);
}
