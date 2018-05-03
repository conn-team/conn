package com.github.connteam.conn.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

public class SQLQuery implements AutoCloseable {
    private Connection connection = null;
    private PreparedStatement pstmt = null;
    private int index = 1;

    public SQLQuery(@NotNull Connection connection, @NotNull String SQLString) throws SQLException {
        try {
            if (connection == null || SQLString == null) {
                throw new NullPointerException();
            }

            this.connection = connection;
            this.pstmt = connection.prepareStatement(SQLString);
        } catch (Throwable t) {
            if (connection != null) {
                connection.close();
            }
            throw t;
        }
    }

    public SQLQuery set(int i, Object value) throws SQLException {
        if (value instanceof Integer) {
            pstmt.setInt(i, (Integer) value);
        } else if (value instanceof String) {
            pstmt.setString(i, (String) value);
        } else if (value instanceof byte[]) {
            pstmt.setBytes(i, (byte[]) value);
        } else {
            throw new UnsupportedOperationException();
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

    public <T> List<T> executeQuery(ResultSetConverter<T> rsc) throws SQLException {
        ResultSet rs = executeQuery();
        ArrayList<T> rl = new ArrayList<>();
        while (rs.next()) {
            rl.add(rsc.fromResultSet(rs));
        }
        return rl;
    }

    public int executeUpdate() throws SQLException {
        return pstmt.executeUpdate();
    }

    public boolean execute() throws SQLException {
        return pstmt.execute();
    }

    @Override
    public void close() throws SQLException {
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}