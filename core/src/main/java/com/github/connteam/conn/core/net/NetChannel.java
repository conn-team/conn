package com.github.connteam.conn.core.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import com.github.connteam.conn.core.events.EventListener;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

public abstract class NetChannel implements Closeable {
    @FunctionalInterface
    public static interface Provider {
        NetChannel create(MessageRegistry in, MessageRegistry out) throws IOException;
    }

    private volatile EventListener<Message> messageHandler;
    private volatile EventListener<Exception> closeHandler;

    public abstract void open();
    public abstract void close(Exception err);
    public abstract void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    public abstract boolean isOpen();
    public abstract Exception getError();
    public abstract void sendMessage(Message msg);
    public abstract InetAddress getAddress();
    public abstract int getPort();
    public abstract int getTimeout() throws IOException;
    public abstract void setTimeout(int millis) throws IOException;

    public void close() {
        close(null);
    }

    public EventListener<Message> getMessageHandler() {
        return messageHandler;
    }

    public EventListener<Exception> getCloseHandler() {
        return closeHandler;
    }

    public void setMessageHandler(EventListener<Message> handler) {
        messageHandler = handler;
    }

    public void setCloseHandler(EventListener<Exception> handler) {
        closeHandler = handler;
    }
}
