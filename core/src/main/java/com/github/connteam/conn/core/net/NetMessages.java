package com.github.connteam.conn.core.net;

import com.github.connteam.conn.core.net.proto.NetProtos.*;
import com.github.connteam.conn.core.io.MessageRegistry;

public final class NetMessages {
    public static final MessageRegistry CLIENTBOUND = new MessageRegistry();
    public static final MessageRegistry SERVERBOUND = new MessageRegistry();

    static {
        CLIENTBOUND.registerMessage(1, AuthRequest.getDefaultInstance());
        CLIENTBOUND.registerMessage(2, AuthStatus.getDefaultInstance());
        CLIENTBOUND.registerMessage(3, KeepAlive.getDefaultInstance());

        SERVERBOUND.registerMessage(101, AuthResponse.getDefaultInstance());
        SERVERBOUND.registerMessage(102, KeepAlive.getDefaultInstance());
    }
}
