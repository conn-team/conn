package com.github.connteam.conn.client.database.provider;

import com.github.connteam.conn.core.database.DatabaseException;

public interface TableProvider {
    void createTables() throws DatabaseException;

    void dropTables() throws DatabaseException;
}