package com.github.connteam.conn.client.database.provider;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.client.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.MessageEntry;
import com.github.connteam.conn.client.database.model.SettingsEntry;
import com.github.connteam.conn.client.database.model.SqliteModelFactory;
import com.github.connteam.conn.client.database.model.UsedEphemeralKeyEntry;
import com.github.connteam.conn.client.database.model.UserEntry;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.database.DatabaseUtil;
import com.github.connteam.conn.core.database.SQLQuery;

public class SqliteDataProvider implements DataProvider {
    private Connection connection;

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

    private SQLQuery query(String SQLString) throws SQLException {
        return new SQLQuery(connection, SQLString, false);
    }

    @Override
    synchronized public List<EphemeralKeyEntry> getEphemeralKeys() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM ephemeral_keys;")) {
            return q.executeQuery(SqliteModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<EphemeralKeyEntry> getEphemeralKeyByPublicKey(@NotNull byte[] publicKey)
            throws DatabaseException {
        if (publicKey == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("SELECT * FROM ephemeral_keys WHERE public_key = ?;")) {
            return q.push(publicKey).executeQueryFirst(SqliteModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<EphemeralKeyEntry> getEphemeralKey(int id) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM ephemeral_keys WHERE id_key = ?;")) {
            return q.push(id).executeQueryFirst(SqliteModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        final String SQLString = "INSERT INTO ephemeral_keys (public_key, private_key) VALUES (?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(key.getRawPublicKey(), key.getRawPrivateKey()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteEphemeralKey(int id) throws DatabaseException {
        try (SQLQuery q = query("DELETE FROM ephemeral_keys WHERE id_key = ?;")) {
            return q.push(id).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<MessageEntry> getMessages(int idFrom) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE id_user = ?;")) {
            return q.push(idFrom).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<MessageEntry> getMessagesPage(int idFrom, int count, int maxID) throws DatabaseException {
        try (SQLQuery q = query(
                "SELECT * FROM messages WHERE id_user = ? AND id_message <= ? ORDER BY id_message DESC LIMIT ?;")) {
            return q.push(idFrom).push(maxID).push(count).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<MessageEntry> getMessagesFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE (id_user = ?) AND (is_outgoing = 0);")) {
            return q.push(idFrom).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<MessageEntry> getMessagesTo(int idTo) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE (id_user = ?) AND (is_outgoing = 1);")) {
            return q.push(idTo).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<MessageEntry> getMessage(int idMessage) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE id_message = ?;")) {
            return q.push(idMessage).executeQueryFirst(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertMessage(@NotNull MessageEntry message) throws DatabaseException {
        if (message == null) {
            throw new NullPointerException();
        }

        String SQLString = "INSERT INTO messages (id_user, is_outgoing, message, time) VALUES (?, ?, ?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(message.getIdUser(), message.isOutgoing(), message.getMessage(), message.getTime())
                    .executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteMessage(int idMessage) throws DatabaseException {
        String SQLString = "DELETE FROM messages WHERE id_message = ?;";
        try (SQLQuery q = query(SQLString)) {
            return q.push(idMessage).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean updateMessage(@NotNull MessageEntry message) throws DatabaseException {
        if (message == null) {
            throw new NullPointerException();
        }

        String SQLString = "UPDATE messages SET id_user = ?, is_outgoing = ?, message = ?, time = ? WHERE id_message = ?;";
        try (SQLQuery q = query(SQLString)) {
            return q.push(message.getIdUser(), message.isOutgoing(), message.getMessage(), message.getTime(),
                    message.getIdMessage()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int deleteMessagesFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = query("DELETE FROM messages WHERE (id_user = ?) AND (is_outgoing = 0);")) {
            return q.push(idFrom).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int deleteMessagesTo(int idTo) throws DatabaseException {
        try (SQLQuery q = query("DELETE FROM messages WHERE (id_user = ?) AND (is_outgoing = 1);")) {
            return q.push(idTo).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<SettingsEntry> getSettings() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM settings;")) {
            return q.executeQueryFirst(SqliteModelFactory::settingsFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean setSettings(@NotNull SettingsEntry settings) throws DatabaseException {
        if (settings == null) {
            throw new NullPointerException();
        }

        String SQLString = "INSERT OR REPLACE INTO settings (row_guard, username, public_key, private_key) VALUES (0, ?, ?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(settings.getUsername(), settings.getRawPublicKey(), settings.getRawPrivateKey())
                    .executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<UserEntry> getUser(int id) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users WHERE id_user = ?;")) {
            return q.push(id).executeQueryFirst(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<UserEntry> getUserByUsername(@NotNull String username) throws DatabaseException {
        if (username == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("SELECT * FROM users WHERE LOWER(username) = LOWER(?);")) {
            return q.push(username).executeQueryFirst(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<UserEntry> getUsers() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users;")) {
            return q.executeQuery(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<UserEntry> getVerifiedUsers() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users WHERE is_verified = 1;")) {
            return q.executeQuery(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<UserEntry> getFriends() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users WHERE is_friend = 1;")) {
            return q.executeQuery(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertUser(@NotNull UserEntry user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        String SQLString = "INSERT INTO users (username, public_key, is_verified, out_sequence, in_sequence, is_friend) VALUES (?, ?, ?, ?, ?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(user.getUsername(), user.getRawPublicKey(), user.isVerified(), user.getOutSequence(),
                    user.getInSequence(), user.isFriend()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteUser(int id) throws DatabaseException {
        try (SQLQuery q = query("DELETE FROM users WHERE id_user = ?;")) {
            return q.push(id).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteUserByUsername(@NotNull String username) throws DatabaseException {
        if (username == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("DELETE FROM users WHERE LOWER(username) = LOWER(?);")) {
            return q.push(username).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public void close() throws DatabaseException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public void createTables() throws DatabaseException {
        try {
            DatabaseUtil.executeScriptFromResource(connection, getClass(), "sql/create-tables.sql");
        } catch (SQLException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public void dropTables() throws DatabaseException {
        try {
            DatabaseUtil.executeScriptFromResource(connection, getClass(), "sql/drop-tables.sql");
        } catch (SQLException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<UsedEphemeralKeyEntry> getUsedEphemeralKeys() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM used_ephemeral_keys;")) {
            return q.executeQuery(SqliteModelFactory::usedEphemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean isUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("SELECT COUNT(*) FROM used_ephemeral_keys WHERE key = ?;")) {
            return q.push(key.getRawKey()).executeQueryCount() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public void insertUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("INSERT OR REPLACE INTO used_ephemeral_keys (key) VALUES (?);")) {
            q.push(key.getRawKey()).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteUsedEphemeralKey(@NotNull UsedEphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("DELETE FROM used_ephemeral_keys WHERE (key) = ?;")) {
            return q.push(key.getRawKey()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
}
