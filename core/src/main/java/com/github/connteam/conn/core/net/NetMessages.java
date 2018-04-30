package com.github.connteam.conn.core.net;

import com.github.connteam.conn.core.net.NetProtos.*;
import com.github.connteam.conn.core.io.MessageRegistry;

public final class NetMessages {
    public static final MessageRegistry CLIENTBOUND = new MessageRegistry();
    public static final MessageRegistry SERVERBOUND = new MessageRegistry();

    static {
        CLIENTBOUND.registerMessage(1, AuthRequest.getDefaultInstance());

        SERVERBOUND.registerMessage(2, AuthResponse.getDefaultInstance());
    }
}