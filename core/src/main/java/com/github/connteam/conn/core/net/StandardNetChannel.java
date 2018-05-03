package com.github.connteam.conn.core.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import com.github.connteam.conn.core.io.IOUtils;
import com.github.connteam.conn.core.io.MessageInputStream;
import com.github.connteam.conn.core.io.MessageOutputStream;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardNetChannel extends NetChannel {
    private final static Logger LOG = LoggerFactory.getLogger(StandardNetChannel.class);
    
    private final Socket socket;
    private final MessageInputStream in;
    private final MessageOutputStream out;

    private final Thread readerThread;
    private final ExecutorService writerExecutor;

    private volatile boolean opened = false, closed = false;
    private volatile IOException lastError = null;

    public StandardNetChannel(Socket socket, MessageRegistry inRegistry, MessageRegistry outRegistry)
            throws IOException {

        this.socket = socket;
        
        try {
			in = new MessageInputStream(socket.getInputStream(), inRegistry);
            out = new MessageOutputStream(socket.getOutputStream(), outRegistry);
		} catch (IOException e) {
            socket.close();
            throw e;
		}

        readerThread = new Thread(() -> {
            try {
                while (!closed) {
                    Message msg = in.readMessage();
                    if (!closed) {
                        LOG.trace("Received {}\n{}", msg.getClass().getSimpleName(), msg.toString());
                        getMessageHandler().handle(msg);
                    }
                }
            } catch (IOException e) {
                close(e);
            }
        });

        writerExecutor = Executors.newSingleThreadExecutor();
    }

    public static Provider fromSocket(Socket socket) {
        return (in, out) -> new StandardNetChannel(socket, in, out);
    }

    public static Provider connectTCP(String host, int port) throws IOException {
        return fromSocket(new Socket(host, port));
    }

    public static Provider connectSSL(String host, int port) throws IOException {
        return fromSocket(SSLSocketFactory.getDefault().createSocket(host, port));
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
        if (closed) {
            return;
        }

        closed = true;
        lastError = err;

        writerExecutor.submit(() -> {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(socket);
        });

        writerExecutor.shutdown();
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
    public synchronized void sendMessage(Message msg) {
        if (!opened) {
            throw new IllegalStateException("Cannot send on not opened channel");
        }
        if (!out.getRegistry().containsMessage(msg)) {
            throw new IllegalArgumentException("Not registered message");
        }

        if (!closed) {
            writerExecutor.submit(() -> {
                LOG.trace("Sending {}\n{}", msg.getClass().getSimpleName(), msg.toString());
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
