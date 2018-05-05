package com.github.connteam.conn.server.database.provider;

import java.io.IOException;

import com.github.connteam.conn.core.database.DatabaseException;

public interface TableProvider {
    void createTables() throws DatabaseException, IOException;
}