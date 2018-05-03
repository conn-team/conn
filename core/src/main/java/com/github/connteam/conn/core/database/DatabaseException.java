package com.github.connteam.conn.core.database;

public class DatabaseException extends Exception {
    public DatabaseException() {
        super();
    }

    public DatabaseException(String msg) {
        super(msg);
    }

    public DatabaseException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public DatabaseException(Throwable throwable) {
        super(throwable);
    }
}