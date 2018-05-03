package com.github.connteam.conn.client.database.provider;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.Friend;

public interface FriendProvider {
    List<Friend> getFriends();

    Optional<Friend> getFriendById(int id);

    Optional<Integer> insertFriend(@NotNull Friend friend);

    boolean deleteFriend(int id);
}
