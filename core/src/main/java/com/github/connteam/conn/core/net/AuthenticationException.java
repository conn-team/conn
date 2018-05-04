package com.github.connteam.conn.core.net;

public class AuthenticationException extends Exception {
    private static final long serialVersionUID = -2004468790691055465L;

    public AuthenticationException() {
        super();
    }

    public AuthenticationException(String msg) {
        super(msg);
    }

    public AuthenticationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    public AuthenticationException(Throwable throwable) {
        super(throwable);
    }
}
