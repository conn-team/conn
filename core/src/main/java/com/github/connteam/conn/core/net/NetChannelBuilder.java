package com.github.connteam.conn.core.net;

import java.io.IOException;

import com.github.connteam.conn.core.events.EventListener;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

public interface NetChannelBuilder {
    NetChannelBuilder setMessageRegistry(MessageRegistry inRegistry, MessageRegistry outRegistry);
    NetChannelBuilder setMessageHandler(EventListener<Message> incomingHandler);
    NetChannelBuilder setCloseHandler(EventListener<IOException> closeHandler);
    NetChannel build() throws IOException;
}
