package com.github.connteam.conn.core;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

public class StandardNetChannel implements NetChannel {
    private final Socket socket;
    private final MessageInputStream in;
    private final MessageOutputStream out;
    private final EventListener<Message> incomingHandler;
    private final EventListener<IOException> closeHandler;
    private final Thread readerThread;
    private final ExecutorService outgoingQueue;

    private volatile boolean closed = false;
    private volatile IOException lastError = null;

    public static class Builder {
        private Socket socket;
        private MessageRegistry inRegistry, outRegistry;
        private EventListener<Message> incomingHandler;
        private EventListener<IOException> closeHandler;

        private Builder() {}

        public Builder setSocket(Socket socket) {
            this.socket = socket;
            return this;
        }

        public Builder setInMessages(MessageRegistry inRegistry) {
            this.inRegistry = inRegistry;
            return this;
        }

        public Builder setOutMessages(MessageRegistry outRegistry) {
            this.outRegistry = outRegistry;
            return this;
        }

        public Builder setInMessageHandler(EventListener<Message> incomingHandler) {
            this.incomingHandler = incomingHandler;
            return this;
        }

        public Builder setCloseHandler(EventListener<IOException> closeHandler) {
            this.closeHandler = closeHandler;
            return this;
        }

        public NetChannel build() throws IOException {
            return new StandardNetChannel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private StandardNetChannel(Builder builder) throws IOException {
        if (builder.socket == null || builder.inRegistry == null || builder.outRegistry == null) {
            throw new IllegalStateException("Missing network channel parameters");
        }

        socket = builder.socket;
        in = new MessageInputStream(socket.getInputStream(), builder.inRegistry);
        out = new MessageOutputStream(socket.getOutputStream(), builder.outRegistry);

        incomingHandler = (builder.incomingHandler != null ? builder.incomingHandler : (m -> {}));
        closeHandler = (builder.closeHandler != null ? builder.closeHandler : (e -> {}));

        readerThread = new Thread(() -> {
            try {
                while (true) {
                    incomingHandler.handle(in.readMessage());
                }
            } catch (IOException e) {
                close(e);
            }
        });

        outgoingQueue = Executors.newSingleThreadExecutor();
    }

    @Override
    public void startListener() {
        synchronized (socket) {
            if (!closed && !readerThread.isAlive()) {
                readerThread.start();
            }
        }
    }

    private void close(IOException err) {
        synchronized (socket) {
            if (closed) {
                return;
            }

            closed = true;
            lastError = err;

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
        }

        closeHandler.handle(lastError);
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public IOException getError() {
        return lastError;
    }

    @Override
    public void sendMessage(Message msg) {
        outgoingQueue.submit(() -> {
            try {
                out.writeMessage(msg);
            } catch (IOException e) {
                close(e);
            }
        });
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        readerThread.join();
        outgoingQueue.awaitTermination(1, TimeUnit.DAYS);
    }
}
