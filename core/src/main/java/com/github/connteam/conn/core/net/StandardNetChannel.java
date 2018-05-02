package com.github.connteam.conn.core.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.github.connteam.conn.core.io.IOUtils;
import com.github.connteam.conn.core.io.MessageInputStream;
import com.github.connteam.conn.core.io.MessageOutputStream;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

public class StandardNetChannel extends NetChannel {
    private final Socket socket;
    private final MessageInputStream in;
    private final MessageOutputStream out;
    
    private final Thread readerThread;
    private final ExecutorService writerExecutor;

    private volatile boolean opened = false;
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

        writerExecutor = Executors.newSingleThreadExecutor();
    }

    public static Provider newProvider(Socket socket) {
        return (in, out) -> new StandardNetChannel(socket, in, out);
    }

    @Override
    public synchronized void open() {
        if (opened) {
            throw new IllegalStateException("Cannot reopen network channel");
        }

        opened = true;
        readerThread.start();
    }

    @Override
    public synchronized void close(IOException err) {
        if (!opened) {
            throw new IllegalStateException("Cannot close not opened channel");
        }
        if (writerExecutor.isShutdown()) {
            return;
        }

        try {
			socket.shutdownInput();
		} catch (IOException e) {}

        writerExecutor.submit(() -> {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(socket);
        });

        lastError = err;
        writerExecutor.shutdown();
        getCloseHandler().handle(lastError);
    }

    @Override
    public boolean isOpen() {
        return opened && !writerExecutor.isShutdown();
    }

    @Override
    public IOException getError() {
        return lastError;
    }

    @Override
    public synchronized void sendMessage(Message msg) {
        if (!opened) {
            throw new IllegalStateException("Cannot send on not opened channel");
        }
        if (!out.getRegistry().containsMessage(msg)) {
            throw new IllegalArgumentException("Not registered message");
        }

        if (!writerExecutor.isShutdown()) {
            writerExecutor.submit(() -> {
                try {
                    out.writeMessage(msg);
                } catch (IOException e) {
                    close(e);
                }
            });
        }
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (!opened) {
            throw new IllegalStateException("Cannot await termination of not opened channel");
        }

        readerThread.join();
        writerExecutor.awaitTermination(timeout, unit);
    }
}
