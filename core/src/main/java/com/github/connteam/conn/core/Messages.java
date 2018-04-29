package com.github.connteam.conn.core;

import com.github.connteam.conn.core.MessageProtos.*;

public class Messages {
    public static final MessageRegistry CLIENTBOUND = new MessageRegistry();
    public static final MessageRegistry SERVERBOUND = new MessageRegistry();

    static {
        CLIENTBOUND.registerMessage(1, AuthRequest.class, AuthRequest::parseFrom);

        SERVERBOUND.registerMessage(1, AuthResponse.class, AuthResponse::parseFrom);
    }
}
