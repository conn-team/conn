package com.github.connteam.conn.core.net;

import com.github.connteam.conn.core.net.proto.NetProtos.*;
import com.github.connteam.conn.core.net.proto.PeerProtos.*;
import com.github.connteam.conn.core.io.MessageRegistry;

public final class NetMessages {
    public static final MessageRegistry CLIENTBOUND = new MessageRegistry();
    public static final MessageRegistry SERVERBOUND = new MessageRegistry();
    public static final MessageRegistry PEER = new MessageRegistry();

    static {
        CLIENTBOUND.registerMessage(1, AuthRequest.getDefaultInstance());
        CLIENTBOUND.registerMessage(2, AuthStatus.getDefaultInstance());
        CLIENTBOUND.registerMessage(3, KeepAlive.getDefaultInstance());
        CLIENTBOUND.registerMessage(4, DeprecatedTextMessage.getDefaultInstance());
        CLIENTBOUND.registerMessage(5, UserInfo.getDefaultInstance());
        CLIENTBOUND.registerMessage(6, EphemeralKeysDemand.getDefaultInstance());
        CLIENTBOUND.registerMessage(7, TransmissionResponse.getDefaultInstance());
        CLIENTBOUND.registerMessage(8, PeerSendAck.getDefaultInstance());
        CLIENTBOUND.registerMessage(9, PeerRecv.getDefaultInstance());

        SERVERBOUND.registerMessage(101, AuthResponse.getDefaultInstance());
        SERVERBOUND.registerMessage(102, KeepAlive.getDefaultInstance());
        SERVERBOUND.registerMessage(103, DeprecatedTextMessage.getDefaultInstance());
        SERVERBOUND.registerMessage(104, UserInfoRequest.getDefaultInstance());
        SERVERBOUND.registerMessage(105, EphemeralKeysUpload.getDefaultInstance());
        SERVERBOUND.registerMessage(106, TransmissionRequest.getDefaultInstance());
        SERVERBOUND.registerMessage(107, PeerSend.getDefaultInstance());
        SERVERBOUND.registerMessage(108, PeerRecvAck.getDefaultInstance());

        PEER.registerMessage(201, PeerTextMessage.getDefaultInstance());
    }
}
