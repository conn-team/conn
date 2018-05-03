package com.github.connteam.conn.server.database.provider;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.github.connteam.conn.core.database.DatabaseException;
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

        public Builder setPass(@NotNull String pass) {
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
        return null;
    }

    @Override
    public List<EphemeralKey> getEphemeralKeyByUserId(int userId) throws DatabaseException {
        return null;
    }

    @Override
    public Optional<Integer> insertEphemeralKey(EphemeralKey key) throws DatabaseException {
        return null;
    }

    @Override
    public boolean updateEphemeralKey(EphemeralKey key) throws DatabaseException {
        return false;
    }

    @Override
    public boolean deleteEphemeralKey(int keyId) throws DatabaseException {
        return false;
    }

    @Override
    public int deleteEphemeralKeyByUserId(int userId) throws DatabaseException {
        return 0;
    }

    @Override
    public Optional<Message> getMessage(int idMessage) throws DatabaseException {
        return null;
    }

    @Override
    public List<Message> getMessagesFrom(int idFrom) throws DatabaseException {
        return null;
    }

    @Override
    public List<Message> getMessagesTo(int idTo) throws DatabaseException {
        return null;
    }

    @Override
    public Optional<Integer> insertMessage(Message message) throws DatabaseException {
        return null;
    }

    @Override
    public boolean updateMessage(Message message) throws DatabaseException {
        return false;
    }

    @Override
    public boolean deleteMessage(int idMessage) throws DatabaseException {
        return false;
    }

    @Override
    public int deleteMessagesFrom(int idFrom) throws DatabaseException {
        return 0;
    }

    @Override
    public int deleteMessagesTo(int idTo) throws DatabaseException {
        return 0;
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
            return q.push(observed.getIdObserver(), observed.getIdObserved()).execute();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean deleteObserved(Observed observed) throws DatabaseException {
        try (SQLQuery q = new SQLQuery(cpds.getConnection(),
                "DELETE FROM observed WHERE (id_observer = ?) AND (id_observed = ?);")) {
            return q.push(observed.getIdObserver(), observed.getIdObserved()).execute();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public Optional<User> getUser(int id) throws DatabaseException {
        return null;
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws DatabaseException {
        return null;
    }

    @Override
    public Optional<Integer> insertUser(User user) throws DatabaseException {
        return null;
    }

    @Override
    public boolean updateUser(User user) throws DatabaseException {
        return false;
    }

    @Override
    public boolean updateUserByUsername(String username) throws DatabaseException {
        return false;
    }

    @Override
    public int deleteUser(int id) throws DatabaseException {
        return 0;
    }

    @Override
    public int deleteUserByUsername(String username) throws DatabaseException {
        return 0;
    }

    @Override
    public void close() {
        cpds.close();
    }
}
