package com.github.connteam.conn.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLQuery implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(SQLQuery.class);

    private Connection connection = null;
    private PreparedStatement pstmt = null;
    private int index = 1;

    private Date creationTime = null;
    private String SQLString = null;

    private boolean autoCloseConnection = true;

    public SQLQuery(@NotNull Connection connection, @NotNull String SQLString, boolean autoCloseConnection)
            throws SQLException {
        try {
            this.autoCloseConnection = autoCloseConnection;
            if (connection == null || SQLString == null) {
                throw new NullPointerException();
            }

            this.connection = connection;
            this.SQLString = SQLString;
            this.pstmt = connection.prepareStatement(SQLString);
            this.creationTime = new Date();
            LOG.trace("|{}| connection pooled.", SQLString);
        } catch (Throwable t) {
            if (connection != null) {
                closeConnection();
            }
            throw t;
        }
    }

    public SQLQuery(@NotNull Connection connection, @NotNull String SQLString) throws SQLException {
        this(connection, SQLString, true);
    }

    public SQLQuery set(int i, Object value) throws SQLException {
        if (value instanceof Integer) {
            pstmt.setInt(i, (Integer) value);
        } else if (value instanceof String) {
            pstmt.setString(i, (String) value);
        } else if (value instanceof byte[]) {
            pstmt.setBytes(i, (byte[]) value);
        } else if (value instanceof Timestamp) {
            pstmt.setTimestamp(i, (Timestamp) value);
        } else if (value instanceof Boolean) {
            pstmt.setBoolean(i, (Boolean) value);
        } else {
            throw new UnsupportedOperationException(value.getClass().getSimpleName());
        }
        return this;
    }

    public SQLQuery push(Object value) throws SQLException {
        return set(index++, value);
    }

    public SQLQuery push(Object... values) throws SQLException {
        for (Object value : values) {
            push(value);
        }
        return this;
    }

    public ResultSet executeQuery() throws SQLException {
        return pstmt.executeQuery();
    }

    public <T> List<T> executeQuery(ResultSetConverter<T> converter) throws SQLException {
        ResultSet resultSet = executeQuery();
        ArrayList<T> resultList = new ArrayList<>();
        while (resultSet.next()) {
            resultList.add(converter.fromResultSet(resultSet));
        }
        return resultList;
    }

    public <T> Optional<T> executeQueryFirst(ResultSetConverter<T> converter) throws SQLException {
        List<T> resultList = executeQuery(converter);
        return Optional.ofNullable(resultList.isEmpty() ? null : resultList.get(0));
    }

    public int executeInsert() throws DatabaseInsertException, SQLException {
        execute();
        ResultSet keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
            return keys.getInt(1);
        }

        throw new DatabaseInsertException();
    }

    public int executeUpdate() throws SQLException {
        return pstmt.executeUpdate();
    }

    public boolean execute() throws SQLException {
        return pstmt.execute();
    }

    @Override
    public void close() throws SQLException {
        final long milli = new Date().getTime() - creationTime.getTime();
        LOG.trace("|{}| {} took {} ms.", SQLString, autoCloseConnection ? "connection closed," : "", milli);
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        } finally {
            if (connection != null) {
                closeConnection();
            }
        }
    }

    private void closeConnection() throws SQLException {
        if (autoCloseConnection && connection != null) {
            connection.close();
        }
    }
}