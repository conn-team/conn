package com.github.connteam.conn.core;

import java.io.Closeable;
import java.io.IOException;

import com.google.protobuf.Message;

public interface NetChannel extends Closeable {
    public void close();
    public void awaitTermination() throws InterruptedException;
    public boolean isClosed();
    public IOException getError();
    public void sendMessage(Message msg);
}