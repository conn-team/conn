package com.github.connteam.conn.core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IOUtils {
    private IOUtils() {
    }

    public static void closeQuietly(AutoCloseable obj) {
        try {
            if (obj != null) {
                obj.close();
            }
        } catch (Exception e) {
        }
    }

    public static byte[] readAllBytes(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int nBytes = 0;

        while ((nBytes = is.read(chunk, 0, chunk.length)) >= 0) {
            buffer.write(chunk, 0, nBytes);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static byte[] getResourceBytes(Class<?> clazz, String resourceName) throws IOException {
        try (InputStream input = clazz.getClassLoader().getResourceAsStream(resourceName)) {
            return IOUtils.readAllBytes(input);
        }
    }
}
