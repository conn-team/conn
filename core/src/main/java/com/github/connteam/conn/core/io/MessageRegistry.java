package com.github.connteam.conn.core.io;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class MessageRegistry {
    public static final int MIN_MESSAGE_ID = 0x00;
    public static final int MAX_MESSAGE_ID = 0xFF;

    private final Map<Integer, Message> idToMessage = new HashMap<>();
    private final Map<Class<? extends Message>, Integer> messageToId = new HashMap<>();

    public void registerMessage(int id, Message msg) {
        if (id < MIN_MESSAGE_ID || id > MAX_MESSAGE_ID) {
            throw new IllegalArgumentException("Message ID out of range");
        }

        msg = msg.getDefaultInstanceForType();
        Class<? extends Message> type = msg.getClass();

        if (idToMessage.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Message ID " + id + " already bound to " + idToMessage.get(id).getClass().getName());
        }

        if (messageToId.containsKey(type)) {
            throw new IllegalArgumentException(
                    "Message " + msg.getClass().getName() + " already bound to " + messageToId.get(type));
        }

        idToMessage.put(id, msg);
        messageToId.put(type, id);
    }

    public boolean containsMessage(Class<? extends Message> msg) {
        return messageToId.containsKey(msg);
    }

    public boolean containsMessage(Message msg) {
        return messageToId.containsKey(msg.getClass());
    }

    public boolean containsID(int id) {
        return idToMessage.containsKey(id);
    }

    public int getID(Class<? extends Message> msg) {
        Integer id = messageToId.get(msg);
        if (id == null) {
            throw new IllegalArgumentException("Message " + msg.getName() + " is not registered");
        }
        return id;
    }

    public int getID(Message msg) {
        return getID(msg.getClass());
    }

    public Message getPrototype(int id) {
        Message msg = idToMessage.get(id);
        if (msg == null) {
            throw new IllegalArgumentException("Message " + id + " is not registered");
        }
        return msg;
    }

    public Parser<? extends Message> getParser(int id) {
        return getPrototype(id).getParserForType();
    }
}
