package com.github.connteam.conn.core.io;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.github.connteam.conn.core.net.proto.NetProtos.*;
import com.github.connteam.conn.core.net.NetMessages;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.junit.Test;

public class MessageStreamTest {
    @Test
    public void test() throws IOException {
        List<Message> messages = Arrays.asList(
            AuthResponse.newBuilder().setUsername("foo").setSignature(ByteString.copyFromUtf8("one")).build(),
            AuthResponse.newBuilder().setUsername("bar").setSignature(ByteString.copyFromUtf8("two")).build(),
            AuthResponse.newBuilder().setUsername("MarcKo").setSignature(ByteString.copyFromUtf8("three")).build()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (MessageOutputStream msgOut = new MessageOutputStream(out, NetMessages.SERVERBOUND)) {
            for (Message msg : messages) {
                msgOut.writeMessage(msg);
            }
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        
        try (MessageInputStream msgIn = new MessageInputStream(in, NetMessages.SERVERBOUND)) {
            for (Message msg : messages) {
                assertEquals(msg, msgIn.readMessage());
            }
        }
    }
}
