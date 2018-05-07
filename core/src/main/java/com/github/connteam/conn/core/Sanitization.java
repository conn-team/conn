package com.github.connteam.conn.core;

import java.util.regex.Pattern;

public final class Sanitization {
    public static final int MIN_USERNAME_LENGTH = 4;
    public static final int MAX_USERNAME_LENGTH = 32;
    private static final Pattern USERNAME_REGEXP = Pattern.compile("[a-zA-Z0-9_]{4,32}");
    private static final Pattern USERNAME_FILTER = Pattern.compile("[^a-zA-Z0-9_]");

    private Sanitization() {}

    public static boolean isValidUsername(String username) {
        return USERNAME_REGEXP.matcher(username).matches();
    }

    public static String filterUsername(String username) {
        username = USERNAME_FILTER.matcher(username).replaceAll("");
        if (username.length() > MAX_USERNAME_LENGTH) {
            username = username.substring(0, MAX_USERNAME_LENGTH);
        }
        return username;
    }
}
