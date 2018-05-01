package com.github.connteam.conn.core.net;

import java.io.Closeable;
import java.io.IOException;

import com.google.protobuf.Message;

public interface NetChannel extends Closeable {
    public void open();
    public void close();
    public void close(IOException err);
    public void awaitTermination() throws InterruptedException;
    public boolean isOpen();
    public IOException getError();
    public void sendMessage(Message msg);
}
