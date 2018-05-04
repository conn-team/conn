package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Friend;
import com.github.connteam.conn.core.database.DatabaseException;

public interface FriendProvider {
    List<Friend> getFriends() throws DatabaseException;

    Optional<Friend> getFriendById(int id) throws DatabaseException;

    Optional<Integer> insertFriend(@NotNull Friend friend) throws DatabaseException;

    boolean deleteFriend(int id) throws DatabaseException;
}
