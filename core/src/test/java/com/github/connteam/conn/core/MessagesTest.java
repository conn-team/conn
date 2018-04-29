package com.github.connteam.conn.core;

import static org.junit.Assert.*;

import java.io.IOException;

import com.github.connteam.conn.core.MessageProtos.AuthRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.junit.Test;

public class MessagesTest {
    @Test
    public void test() throws IOException {
        AuthRequest req = AuthRequest.newBuilder().setPayload(ByteString.copyFromUtf8("something")).build();
        byte[] data = req.toByteArray();
        Message resp = Messages.CLIENTBOUND.parseFrom(Messages.CLIENTBOUND.getID(req), data);
        assertEquals(req, resp);
    }
}
