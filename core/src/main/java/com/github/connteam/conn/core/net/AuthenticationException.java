package com.github.connteam.conn.core.net;

import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus;
import com.github.connteam.conn.core.net.proto.NetProtos.AuthStatus.Status;

public class AuthenticationException extends Exception {
    private static final long serialVersionUID = -2004468790691055465L;
    
    private final AuthStatus.Status status;

    public AuthenticationException() {
        this.status = Status.UNRECOGNIZED;
    }

    public AuthenticationException(AuthStatus.Status status) {
        super(getMessageFromStatus(status));
        this.status = status;
    }

    public AuthenticationException(AuthStatus.Status status, Throwable cause) {
        super(getMessageFromStatus(status), cause);
        this.status = status;
    }

    public AuthenticationException(String msg) {
        super(msg);
        this.status = Status.UNRECOGNIZED;
    }

    public AuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
        this.status = Status.UNRECOGNIZED;
    }

    public AuthenticationException(Throwable cause) {
        super(cause);
        this.status = Status.UNRECOGNIZED;
    }

    public AuthStatus.Status getStatus() {
        return status;
    }

    public static String getMessageFromStatus(AuthStatus.Status status) {
        switch (status) {
        case LOGGED_IN:
            return "Logged in";
        case REGISTERED:
            return "Registered";
        case MISMATCHED_PUBLICKEY:
            return "Publickey doesn't match (username taken by someone else)";
        case INVALID_SIGNATURE:
            return "Invalid signature";
        case ALREADY_ONLINE:
            return "User connected from another location";
        case INTERNAL_ERROR:
            return "Internal server error";
        default:
            return "Unknown authentication error";
        }
    }
}
