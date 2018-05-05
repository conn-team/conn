package com.github.connteam.conn.core.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.github.connteam.conn.core.io.IOUtils;

public final class DatabaseUtil {
    private DatabaseUtil() {
    }

    public static void executeScriptFromResource(Connection conn, Class<?> clazz, String resourceName)
            throws SQLException, IOException {
        byte[] bytes = IOUtils.getResourceBytes(clazz, resourceName);
        if (bytes == null) {
            throw new IOException("Resource not found");
        }

        final String SQLString = new String(bytes, StandardCharsets.UTF_8);

        try (Statement stmt = conn.createStatement()) {
            // Yes, it's bad, but we're executing trusted script, maybe rewrite it to
            // something better later
            for (String sql : SQLString.split(";")) {
                sql = sql.trim();
                if (sql.length() > 0) {
                    stmt.execute(sql);
                }
            }
        }
    }
}
