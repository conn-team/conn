package com.github.connteam.conn.core;

import java.util.regex.Pattern;

public final class Sanitization {
    private static final Pattern USERNAME_REGEXP = Pattern.compile("[a-zA-Z0-9_]{4,32}");

    private Sanitization() {}

    public static boolean isValidUsername(String username) {
        return USERNAME_REGEXP.matcher(username).matches();
    }
}
