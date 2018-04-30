package com.github.connteam.conn.core.io;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.OutputStream;

import com.google.protobuf.Message;

public class MessageOutputStream extends DataOutputStream {
    private final MessageRegistry registry;

    public MessageOutputStream(OutputStream out, MessageRegistry registry) {
        super(out);
        this.registry = registry;
    }

    public void writeMessage(Message msg) throws IOException {
        byte[] data = msg.toByteArray();
        writeByte(registry.getID(msg));
        writeInt(data.length);
        write(data);
    }
}
