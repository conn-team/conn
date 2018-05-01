package com.github.connteam.conn.core.net;

import java.io.Closeable;
import java.io.IOException;

import com.github.connteam.conn.core.events.EventListener;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

public abstract class NetChannel implements Closeable {
    @FunctionalInterface
    public static interface Provider {
        NetChannel create(MessageRegistry in, MessageRegistry out) throws IOException;
    }

    private volatile EventListener<Message> messageHandler;
    private volatile EventListener<IOException> closeHandler;

    public abstract void open();
    public abstract void close(IOException err);
    public abstract void awaitTermination() throws InterruptedException;
    public abstract boolean isOpen();
    public abstract IOException getError();
    public abstract void sendMessage(Message msg);

    public void close() {
        close(null);
    }

    public EventListener<Message> getMessageHandler() {
        return messageHandler;
    }

    public EventListener<IOException> getCloseHandler() {
        return closeHandler;
    }

    public void setMessageHandler(EventListener<Message> handler) {
        messageHandler = handler;
    }

    public void setCloseHandler(EventListener<IOException> handler) {
        closeHandler = handler;
    }
}
