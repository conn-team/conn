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

    public AuthStatus.Status getStatus() {
        return status;
    }

    @Override
    public String getLocalizedMessage() {
        switch (status) {
        case LOGGED_IN:
            return "Zalogowano";
        case REGISTERED:
            return "Zarejestrowano";
        case MISMATCHED_PUBLICKEY:
            return "Brak zgodności klucza - ktoś inny już używa tego nicku";
        case INVALID_SIGNATURE:
            return "Nieprawidłowa sygnatura";
        case INVALID_INPUT:
            return "Nieprawidłowe dane";
        case ALREADY_ONLINE:
            return "Użytkownik jest już online";
        case INTERNAL_ERROR:
            return "Błąd wewnętrzny serwera";
        default:
            return getMessage();
        }
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
        case INVALID_INPUT:
            return "Invalid input";
        case ALREADY_ONLINE:
            return "User connected from another location";
        case INTERNAL_ERROR:
            return "Internal server error";
        default:
            return "Unknown authentication error";
        }
    }
}
