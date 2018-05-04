package com.github.connteam.conn.client.database.provider;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.github.connteam.conn.client.database.model.SqliteModelFactory;
import com.github.connteam.conn.client.database.model.User;
import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.database.SQLQuery;
import com.github.connteam.conn.core.io.IOUtils;

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
    synchronized public List<EphemeralKey> getEphemeralKeys() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM ephemeral_keys;")) {
            return q.executeQuery(SqliteModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<EphemeralKey> getEphemeralKey(int id) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM ephemeral_keys;")) {
            return q.executeQueryFirst(SqliteModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        final String SQLString = "INSERT INTO ephemeral_keys (public_key, private_key) VALUES (?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(key.getRawPublicKey(), key.getRawPublicKey()).executeInsert();
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
    synchronized public List<Friend> getFriends() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM friends;")) {
            return q.executeQuery(SqliteModelFactory::friendFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<Friend> getFriendById(int id) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM friends WHERE id_user = ?;")) {
            return q.executeQueryFirst(SqliteModelFactory::friendFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<Integer> insertFriend(@NotNull Friend friend) throws DatabaseException {
        if (friend == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("INSERT INTO friends (id_user) VALUES (?);")) {
            q.push(friend.getId()).execute();
            return Optional.of(friend.getId());
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean deleteFriend(int id) throws DatabaseException {
        try (SQLQuery q = query("DELETE FROM friends WHERE id_user = ?;")) {
            return q.push(id).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<Message> getMessageFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE (id_user = ?) AND (is_outgoing = 0);")) {
            return q.push(idFrom).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<Message> getMessageTo(int idTo) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE (id_user = ?) AND (is_outgoing = 1);")) {
            return q.push(idTo).executeQuery(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<Message> getMessage(int idMessage) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM messages WHERE id_message = ?;")) {
            return q.push(idMessage).executeQueryFirst(SqliteModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertMessage(@NotNull Message message) throws DatabaseException {
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
    synchronized public boolean updateMessage(Message message) throws DatabaseException {
        String SQLString = "UPDATE messages SET id_user = ?, is_outgoing = ?, message = ?, time = ? WHERE id_message = ?;";
        try (SQLQuery q = query(SQLString)) {
            return q.push(message.getIdUser(), message.isOutgoing(), message.getMessage(), message.getTime())
                    .executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int deleteMessageFrom(int idFrom) throws DatabaseException {
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
    synchronized public Optional<Settings> getSettings() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM settings;")) {
            return q.executeQueryFirst(SqliteModelFactory::settingsFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public boolean setSettings(@NotNull Settings settings) throws DatabaseException {
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
    synchronized public Optional<User> getUser(int id) throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users WHERE id_user = ?;")) {
            return q.push(id).executeQueryFirst(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public Optional<User> getUserByUsername(@NotNull String username) throws DatabaseException {
        if (username == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = query("SELECT * FROM users WHERE username = ?;")) {
            return q.push(username).executeQueryFirst(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<User> getUsers() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users;")) {
            return q.executeQuery(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public List<User> getVerifiedUsers() throws DatabaseException {
        try (SQLQuery q = query("SELECT * FROM users WHERE is_verified = 1;")) {
            return q.executeQuery(SqliteModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    synchronized public int insertUser(@NotNull User user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        String SQLString = "INSERT INTO users (username, public_key, is_verified, out_sequence, in_sequence) VALUES (?, ?, ?, ?, ?);";
        try (SQLQuery q = query(SQLString)) {
            return q.push(user.getUsername(), user.getRawPublicKey(), user.isVerified(), user.getOutSequence(),
                    user.getinSequence()).executeInsert();
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
            throw new DatabaseException();
        }

        try (SQLQuery q = query("DELETE FROM users WHERE username = ?;")) {
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
    synchronized public void createTables() throws IOException, DatabaseException {
        byte[] bytes = IOUtils.readAllBytes(getClass().getClassLoader().getResourceAsStream("sql/create-tables.sql"));
        if (bytes == null) {
            throw new IOException("Missing create-tables.sql");
        }

        final String SQLString = new String(bytes, StandardCharsets.UTF_8);
        throw new UnsupportedOperationException();
        // TODO: Write multi statement execution
    }
}