package com.github.connteam.conn.core;

import com.github.connteam.conn.core.MessageProtos.*;

public final class Messages {
    public static final MessageRegistry CLIENTBOUND = new MessageRegistry();
    public static final MessageRegistry SERVERBOUND = new MessageRegistry();

    static {
        CLIENTBOUND.registerMessage(1, AuthRequest.getDefaultInstance());

        SERVERBOUND.registerMessage(2, AuthResponse.getDefaultInstance());
    }
}
