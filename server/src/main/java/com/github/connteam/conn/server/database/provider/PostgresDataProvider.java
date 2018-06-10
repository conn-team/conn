package com.github.connteam.conn.server.database.provider;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
import com.github.connteam.conn.core.database.DatabaseUtil;
import com.github.connteam.conn.core.database.SQLQuery;
import com.github.connteam.conn.server.database.model.EphemeralKeyEntry;
import com.github.connteam.conn.server.database.model.MessageEntry;
import com.github.connteam.conn.server.database.model.PostgresModelFactory;
import com.github.connteam.conn.server.database.model.UserEntry;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class PostgresDataProvider implements DataProvider {
    public static class Builder {
        private String name = null;
        private String user = null;
        private String pass = null;

        private String host = "localhost";
        private int port = 5432;

        private int minPoolSize = 16;
        private int maxPoolSize = 128;
        private int acquireIncrement = 8;

        public Builder setName(@NotNull String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            this.name = name;
            return this;
        }

        public Builder setUser(@NotNull String user) {
            if (user == null) {
                throw new NullPointerException();
            }
            this.user = user;
            return this;
        }

        public Builder setPassword(@NotNull String pass) {
            if (pass == null) {
                throw new NullPointerException();
            }
            this.pass = pass;
            return this;
        }

        public Builder setHost(@NotNull String host) {
            if (host == null) {
                throw new NullPointerException();
            }
            this.host = host;
            return this;
        }

        public Builder setHost(int port) {
            this.port = port;
            return this;
        }

        public Builder setMinPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
            return this;
        }

        public Builder setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder setAcquireIncrementPoolSize(int acquireIncrement) {
            this.acquireIncrement = acquireIncrement;
            return this;
        }

        public PostgresDataProvider build() throws DatabaseException {
            if (name == null || user == null || pass == null) {
                throw new IllegalArgumentException();
            }

            return new PostgresDataProvider(name, user, pass, host, port, minPoolSize, maxPoolSize, acquireIncrement);
        }
    }

    private ComboPooledDataSource cpds = new ComboPooledDataSource();

    private PostgresDataProvider(String name, String user, String pass, String host, int port, int minPoolSize,
            int maxPoolSize, int acquireIncrement) throws DatabaseException {
        try {
            cpds.setDriverClass("org.postgresql.Driver");
        } catch (PropertyVetoException pve) {
            throw new DatabaseException("Missing postgresql driver");
        }

        cpds.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, name));

        cpds.setUser(user);
        cpds.setPassword(pass);

        cpds.setMinPoolSize(minPoolSize);
        cpds.setMaxPoolSize(maxPoolSize);
        cpds.setAcquireIncrement(acquireIncrement);
    }

    @Override
    public Optional<EphemeralKeyEntry> getEphemeralKey(int keyId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM ephemeral_keys WHERE id_key = ?;")) {
            return q.push(keyId).executeQueryFirst(PostgresModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<EphemeralKeyEntry> getEphemeralKeysByUserId(int userId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM ephemeral_keys WHERE id_user = ?;")) {
            return q.push(userId).executeQuery(PostgresModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<EphemeralKeyEntry> popEphemeralKeyByUserId(int userId) throws DatabaseException {
        Connection conn = null;
        try {
            conn = cpds.getConnection();
            conn.setAutoCommit(false);

            Optional<EphemeralKeyEntry> key = Optional.ofNullable(null);
            try (SQLQuery q = new SQLQuery(conn, "SELECT * FROM ephemeral_keys WHERE id_user = ?;", false)) {
                key = q.push(userId).executeQueryFirst(PostgresModelFactory::ephemeralKeyFromResultSet);
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }

            if (!key.isPresent()) {
                return key;
            }

            try (SQLQuery q = new SQLQuery(conn, "DELETE FROM ephemeral_keys WHERE id_key = ?;", false)) {
                q.push(key.get().getIdKey()).executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }

            return key;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.commit();
                } catch (SQLException e) {
                    throw new DatabaseException(e);
                } finally {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        throw new DatabaseException(e);
                    }
                }
            }
        }
    }

    @Override
    public int countEphemeralKeysByUserId(int userId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "SELECT COUNT(*) FROM ephemeral_keys WHERE id_user = ?;")) {
            return q.push(userId).executeQueryCount();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        String SQLString = "INSERT INTO ephemeral_keys (id_user, key, signature) VALUES (?, ?, ?);";
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), SQLString)) {
            return q.push(key.getIdUser(), key.getRawKey(), key.getSignature()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean updateEphemeralKey(@NotNull EphemeralKeyEntry key) throws DatabaseException {
        if (key == null) {
            throw new NullPointerException();
        }

        String SQLString = "UPDATE ephemeral_keys SET id_user = ?, key = ?, signature = ? WHERE id_key = ?;";
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), SQLString)) {
            return q.push(key.getIdUser(), key.getRawKey(), key.getSignature(), key.getIdKey()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteEphemeralKey(int keyId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM ephemeral_keys WHERE id_key = ?;")) {
            return q.push(keyId).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int deleteEphemeralKeysByUserId(int userId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM ephemeral_keys WHERE id_user = ?;")) {
            return q.push(userId).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<MessageEntry> getMessage(int idMessage) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_message = ?;")) {
            return q.push(idMessage).executeQueryFirst(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<MessageEntry> getMessagesFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_from = ?;")) {
            return q.push(idFrom).executeQuery(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<MessageEntry> getMessagesTo(int idTo) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_to = ?;")) {
            return q.push(idTo).executeQuery(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<MessageEntry> getMessagesToSince(int idTo, int minIdMsg) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "SELECT * FROM messages WHERE id_to = ? AND id_message >= ?;")) {
            return q.push(idTo).push(minIdMsg).executeQuery(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertMessage(@NotNull MessageEntry message) throws DatabaseException {
        if (message == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "INSERT INTO messages (id_from, id_to, message, partial_key1, partial_key2, signature, time) VALUES (?, ?, ?, ?, ?, ?, ?);")) {
            return q.push(message.getIdFrom(), message.getIdTo(), message.getMessage(), message.getRawPartialKey1(),
                    message.getRawPartialKey2(), message.getSignature(), message.getTime()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean updateMessage(MessageEntry message) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "UPDATE messages SET id_from = ?, id_to = ?, message = ?, partial_key1 = ?, partial_key2 = ?, signature = ?, time = ? WHERE id_message = ?;")) {
            return q.push(message.getIdFrom(), message.getIdTo(), message.getMessage(), message.getRawPartialKey1(),
                    message.getRawPartialKey2(), message.getSignature(), message.getTime(), message.getIdMessage())
                    .executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteMessage(int idMessage) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM messages WHERE id_message = ?;")) {
            return q.push(idMessage).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int deleteMessagesFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM messages WHERE id_from = ?;")) {
            return q.push(idFrom).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int deleteMessagesTo(int idTo) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM messages WHERE id_to = ?;")) {
            return q.push(idTo).executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<UserEntry> getUser(int id) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM users WHERE id_user = ?;")) {
            return q.push(id).executeQueryFirst(PostgresModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<UserEntry> getUserByUsername(String username) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM users WHERE LOWER(username) = LOWER(?);")) {
            return q.push(username).executeQueryFirst(PostgresModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertUser(@NotNull UserEntry user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "INSERT INTO users (username, public_key, signup_time) VALUES (?, ?, ?);")) {
            return q.push(user.getUsername(), user.getRawPublicKey(), user.getSignupTime()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean updateUser(@NotNull UserEntry user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "UPDATE users SET username = ?, public_key = ?, signup_time = ? WHERE id_user = ?;")) {
            return q.push(user.getUsername(), user.getRawPublicKey(), user.getSignupTime(), user.getIdUser())
                    .executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean updateUserByUsername(@NotNull UserEntry user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "UPDATE users SET public_key = ?, signup_time = ? WHERE username = ?;")) {
            return q.push(user.getRawPublicKey(), user.getSignupTime(), user.getUsername()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteUser(int id) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM users WHERE id_user = ?;")) {
            return q.push(id).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteUserByUsername(@NotNull String username) throws DatabaseException {
        if (username == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM users WHERE LOWER(username) = LOWER(?);")) {
            return q.push(username).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void close() {
        cpds.close();
    }

    @Override
    public void createTables() throws DatabaseException {
        try (Connection conn = cpds.getConnection()) {
            DatabaseUtil.executeScriptFromResource(conn, getClass(), "sql/create-tables.sql");
        } catch (SQLException | IOException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void dropTables() throws DatabaseException {
        try (Connection conn = cpds.getConnection()) {
            DatabaseUtil.executeScriptFromResource(conn, getClass(), "sql/drop-tables.sql");
        } catch (SQLException | IOException e) {
            throw new DatabaseException(e);
        }
    }
}
