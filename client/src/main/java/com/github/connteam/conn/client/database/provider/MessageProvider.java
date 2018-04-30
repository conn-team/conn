package com.github.connteam.conn.client.database.provider;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Message;

public interface MessageProvider {
    Stream<Message> selectByIdFrom(int idFrom);

    Optional<Message> selectByIdMessage(int idMessage);

    Optional<Integer> insert(@NotNull Message message);

    boolean update(@NotNull Message message);

    boolean deleteByIdFrom(int idFrom);

    boolean deleteByIdMessage(int idMessage);
}
