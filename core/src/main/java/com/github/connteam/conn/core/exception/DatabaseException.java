package com.github.connteam.conn.core.exception;

public class DatabaseException extends Exception {
    private static final long serialVersionUID = -3645623781051859120L;

    DatabaseException() {
        super();
    }

    DatabaseException(String message) {
        super(message);
    }
}