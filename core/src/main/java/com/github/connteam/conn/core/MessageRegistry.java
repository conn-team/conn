package com.github.connteam.conn.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;

public class MessageRegistry {
    public static final int MIN_MESSAGE_ID = 0x00;
    public static final int MAX_MESSAGE_ID = 0xFF;

    private final Map<Integer, Class<? extends Message>> idToMessage = new HashMap<>();
    private final Map<Integer, BinaryDecoder<? extends Message>> idToDecoder = new HashMap<>();
    private final Map<Class<? extends Message>, Integer> messageToId = new HashMap<>();

    public <T extends Message> void registerMessage(int id, Class<T> clazz, BinaryDecoder<T> decoder) {
        if (id < MIN_MESSAGE_ID || id > MAX_MESSAGE_ID) {
            throw new IllegalArgumentException("Message ID out of range");
        }

        if (idToMessage.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Message ID " + id + " already bound to " + idToMessage.get(id).getName());
        }

        if (messageToId.containsKey(clazz)) {
            throw new IllegalArgumentException("Message " + messageToId.get(clazz) + " already bound to " + id);
        }

        idToMessage.put(id, clazz);
        idToDecoder.put(id, decoder);
        messageToId.put(clazz, id);
    }

    public int getID(Message msg) {
        Integer id = messageToId.get(msg.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Message " + msg.getClass().getName() + " is not registered");
        }
        return id;
    }

    public boolean containsID(int id) {
        return idToDecoder.containsKey(id);
    }

    public Message parseFrom(int id, byte[] data) throws IOException {
        BinaryDecoder<? extends Message> decoder = idToDecoder.get(id);
        if (decoder == null) {
            throw new IllegalArgumentException("Message " + id + " is not registered");
        }
        return decoder.parseFrom(data);
    }
}
