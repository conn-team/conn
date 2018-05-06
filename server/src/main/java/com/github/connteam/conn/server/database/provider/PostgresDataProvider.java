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
import com.github.connteam.conn.server.database.model.EphemeralKey;
import com.github.connteam.conn.server.database.model.Message;
import com.github.connteam.conn.server.database.model.Observed;
import com.github.connteam.conn.server.database.model.PostgresModelFactory;
import com.github.connteam.conn.server.database.model.User;
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
    public Optional<EphemeralKey> getEphemeralKey(int keyId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM ephemeral_keys WHERE id_key = ?;")) {
            return q.push(keyId).executeQueryFirst(PostgresModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<EphemeralKey> getEphemeralKeysByUserId(int userId) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM ephemeral_keys WHERE id_user = ?;")) {
            return q.push(userId).executeQuery(PostgresModelFactory::ephemeralKeyFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException {
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
    public boolean updateEphemeralKey(@NotNull EphemeralKey key) throws DatabaseException {
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
    public Optional<Message> getMessage(int idMessage) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_message = ?;")) {
            return q.push(idMessage).executeQueryFirst(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<Message> getMessagesFrom(int idFrom) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_from = ?;")) {
            return q.push(idFrom).executeQuery(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<Message> getMessagesTo(int idTo) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM messages WHERE id_to = ?;")) {
            return q.push(idTo).executeQuery(PostgresModelFactory::messageFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertMessage(@NotNull Message message) throws DatabaseException {
        if (message == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "INSERT INTO messages (id_from, id_to, message, key, signature, time) VALUES (?, ?, ?, ?, ?, ?);")) {
            return q.push(message.getIdFrom(), message.getIdTo(), message.getMessage(), message.getRawKey(),
                    message.getSignature(), message.getTime()).executeInsert();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean updateMessage(Message message) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "UPDATE messages SET id_from = ?, id_to = ?, message = ?, key = ?, signature = ?, time = ? WHERE id_message = ?;")) {
            return q.push(message.getIdFrom(), message.getIdTo(), message.getMessage(), message.getRawKey(),
                    message.getSignature(), message.getTime(), message.getIdMessage()).executeUpdate() > 0;
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
    public List<Observed> getObserved(int idObserver) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM observed WHERE id_observer = ?;")) {
            return q.push(idObserver).executeQuery(PostgresModelFactory::observedFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public List<Observed> getObservers(int idObserved) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM observed WHERE id_observed = ?;")) {
            return q.push(idObserved).executeQuery(PostgresModelFactory::observedFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean insertObserved(Observed observed) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "INSERT INTO observed (id_observer, id_observed) VALUES (?, ?);")) {
            return q.push(observed.getIdObserver(), observed.getIdObserved()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteObserved(Observed observed) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "DELETE FROM observed WHERE (id_observer = ?) AND (id_observed = ?);")) {
            return q.push(observed.getIdObserver(), observed.getIdObserved()).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<User> getUser(int id) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM users WHERE id_user = ?;")) {
            return q.push(id).executeQueryFirst(PostgresModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "SELECT * FROM users WHERE username = ?;")) {
            return q.push(username).executeQueryFirst(PostgresModelFactory::userFromResultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public int insertUser(@NotNull User user) throws DatabaseException {
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
    public boolean updateUser(@NotNull User user) throws DatabaseException {
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
    public boolean updateUserByUsername(@NotNull User user) throws DatabaseException {
        if (user == null) {
            throw new NullPointerException();
        }

        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "UPDATE users SET public_key = ?, signup_time = ? WHERE username = ?;")) {
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

        try (SQLQuery q = new SQLQuery(cpds.getConnection(), "DELETE FROM users WHERE username = ?;")) {
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
