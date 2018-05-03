package com.github.connteam.conn.core.database;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetConverter<T> {
    T fromResultSet(ResultSet rs) throws SQLException;
}