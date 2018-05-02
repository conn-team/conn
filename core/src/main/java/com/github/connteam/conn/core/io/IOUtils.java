package com.github.connteam.conn.core.io;

import java.io.Closeable;
import java.io.IOException;

public final class IOUtils {
    private IOUtils() {}

    public static void closeQuietly(Closeable obj) {
        try {
            if (obj != null) {
                obj.close();
            }
        } catch (IOException e) {}
    }
}
