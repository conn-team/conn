package com.github.connteam.conn.core.database;

public class DatabaseException extends Exception {
    private static final long serialVersionUID = -2004468790691055465L;

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