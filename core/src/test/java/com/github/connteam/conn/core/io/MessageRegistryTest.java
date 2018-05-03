package com.github.connteam.conn.core.io;

import static org.junit.Assert.*;

import java.io.IOException;

import com.github.connteam.conn.core.net.proto.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.NetMessages;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.junit.Test;

public class MessageRegistryTest {
    @Test
    public void test() throws IOException {
        AuthRequest req = AuthRequest.newBuilder().setPayload(ByteString.copyFromUtf8("something")).build();
        byte[] data = req.toByteArray();
        Message resp = NetMessages.CLIENTBOUND.getParser(NetMessages.CLIENTBOUND.getID(req)).parseFrom(data);
        assertEquals(req, resp);
    }
}
