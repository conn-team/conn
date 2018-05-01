package com.github.connteam.conn.core.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.github.connteam.conn.core.io.MessageInputStream;
import com.github.connteam.conn.core.io.MessageOutputStream;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

public class StandardNetChannel extends NetChannel {
    private final Socket socket;
    private final MessageInputStream in;
    private final MessageOutputStream out;
    private final Thread readerThread;
    private final ExecutorService outgoingQueue;

    private volatile boolean opened = false, closed = false;
    private volatile IOException lastError = null;

    public StandardNetChannel(Socket socket, MessageRegistry inRegistry, MessageRegistry outRegistry)
            throws IOException {

        this.socket = socket;
        in = new MessageInputStream(socket.getInputStream(), inRegistry);
        out = new MessageOutputStream(socket.getOutputStream(), outRegistry);

        readerThread = new Thread(() -> {
            try {
                while (true) {
                    getMessageHandler().handle(in.readMessage());
                }
            } catch (IOException e) {
                close(e);
            }
        });

        outgoingQueue = Executors.newSingleThreadExecutor();
    }

    public static Provider newProvider(Socket socket) {
        return (in, out) -> new StandardNetChannel(socket, in, out);
    }

    @Override
    public void open() {
        synchronized (this) {
            if (opened) {
                throw new IllegalStateException("Cannot reopen network channel");
            }
            opened = true;
        }
        readerThread.start();
    }

    @Override
    public void close(IOException err) {
        synchronized (this) {
            if (!opened) {
                throw new IllegalStateException("Cannot close not opened channel");
            }
            if (closed) {
                return;
            }

            closed = true;
            lastError = err;
        }

        try {
            in.close();
        } catch (IOException e) {}
        try {
            out.close();
        } catch (IOException e) {}
        try {
            socket.close();
        } catch (IOException e) {}

        outgoingQueue.shutdown();
        getCloseHandler().handle(lastError);
    }

    @Override
    public boolean isOpen() {
        return opened && !closed;
    }

    @Override
    public IOException getError() {
        return lastError;
    }

    @Override
    public void sendMessage(Message msg) {
        if (!opened) {
            throw new IllegalStateException("Cannot send on not opened channel");
        }

        if (!out.getRegistry().containsMessage(msg)) {
            throw new IllegalArgumentException("Not registered message");
        }

        try {
            outgoingQueue.submit(() -> {
                try {
                    out.writeMessage(msg);
                } catch (IOException e) {
                    close(e);
                }
            });
        } catch (RejectedExecutionException e) {}
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        if (!opened) {
            throw new IllegalStateException("Cannot await termination of not opened channel");
        }

        readerThread.join();
        outgoingQueue.awaitTermination(1, TimeUnit.DAYS);
    }
}
