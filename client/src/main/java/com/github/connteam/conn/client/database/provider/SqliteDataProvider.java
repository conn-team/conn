package com.github.connteam.conn.client.database.provider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKey;
import com.github.connteam.conn.client.database.model.Friend;
import com.github.connteam.conn.client.database.model.Message;
import com.github.connteam.conn.client.database.model.Settings;
import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.core.database.DatabaseException;

public class SqliteDataProvider implements DataProvider {
    Connection connection;

    public SqliteDataProvider(@NotNull String dbname) throws DatabaseException {
        if (dbname == null) {
            throw new NullPointerException();
        }

        try {
            connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbname));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<EphemeralKey> getEphemeralKeys() {
        return null;
    }

    @Override
    public Optional<EphemeralKey> getEphemeralKey(int id) {
        return null;
    }

    @Override
    public int insertEphemeralKey(EphemeralKey key) {
        return 0;
    }

    @Override
    public boolean deleteEphemeralKey(int id) {
        return false;
    }

    @Override
    public Optional<EphemeralKey> popEphemeralKey() {
        return null;
    }

    @Override
    public List<Friend> getFriends() {
        return null;
    }

    @Override
    public Optional<Friend> getFriendById(int id) {
        return null;
    }

    @Override
    public Optional<Integer> insertFriend(Friend friend) {
        return null;
    }

    @Override
    public boolean deleteFriend(int id) {
        return false;
    }

    @Override
    public List<Message> getMessageFrom(int idFrom) {
        return null;
    }

    @Override
    public List<Message> getMessageTo(int idFrom) {
        return null;
    }

    @Override
    public Optional<Message> getMessage(int idMessage) {
        return null;
    }

    @Override
    public int insertMessage(Message message) {
        return 0;
    }

    @Override
    public boolean updateMessage(Message message) {
        return false;
    }

    @Override
    public int deleteMessageFrom(int idFrom) {
        return 0;
    }

    @Override
    public int deleteMessagesTo(int idTo) {
        return 0;
    }

    @Override
    public Optional<Settings> getSettings() {
        return null;
    }

    @Override
    public boolean setSettings(Settings settings) {
        return false;
    }

    @Override
    public Optional<User> getUser(int id) {
        return null;
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return null;
    }

    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public List<User> getVerifiedUsers() {
        return null;
    }

    @Override
    public int insertUser(User user) {
        return 0;
    }

    @Override
    public boolean deleteUser(int id) {
        return false;
    }

    @Override
    public boolean deleteUserByUsername(String username) {
        return false;
    }

    @Override
    public void close() {

    }
}