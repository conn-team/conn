package com.github.connteam.conn.core.database;

public class DatabaseInsertException extends DatabaseException {
    private static final long serialVersionUID = 8710996481595570879L;

    public DatabaseInsertException() {
        super();
    }

    public DatabaseInsertException(String msg) {
        super(msg);
    }

    public DatabaseInsertException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public DatabaseInsertException(Throwable throwable) {
        super(throwable);
    }
}
