package com.github.connteam.conn.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import com.github.connteam.conn.core.io.IOUtils;

public final class LoggingUtil {
    private LoggingUtil() {
    }

    public static void setupLogging(boolean debug) throws IOException {
        String file = (debug ? "logging.debug.properties" : "logging.properties");
        InputStream in = LoggingUtil.class.getClassLoader().getResourceAsStream(file);

        try {
            LogManager.getLogManager().readConfiguration(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
