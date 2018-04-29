package com.github.connteam.conn.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import com.google.protobuf.Message;

public class MessageInputStream extends DataInputStream {
    public static final int MAX_MESSAGE_LENGTH = 1 << 20;

    private final MessageRegistry registry;
    
    public MessageInputStream(InputStream in, MessageRegistry registry) {
        super(in);
        this.registry = registry;
    }

    public Message readMessage() throws IOException {
        int id = readUnsignedByte();

        if (!registry.containsID(id)) {
            throw new IOException("Invalid message ID");
        }

        int len = readInt();
        
        if (len < 0 || len > MAX_MESSAGE_LENGTH) {
            throw new IOException("Invalid message length");
        }

        byte[] data = new byte[len];
        readFully(data);
        return registry.parseFrom(id, data);
    }
}
