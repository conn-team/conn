package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocketFactory;

import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServer implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServer.class);

    private final ServerSocket server;

    public static class Builder {
        private Integer port;
        private Transport transport;

        private Builder() {}

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setTransport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public ConnServer build() throws IOException {
            if (port == null || transport == null) {
                throw new IllegalStateException();
            }
            return new ConnServer(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private ConnServer(Builder builder) throws IOException {
        switch (builder.transport) {
        case TCP:
            server = new ServerSocket(builder.port);
            break;
        case SSL:
            server = SSLServerSocketFactory.getDefault().createServerSocket(builder.port);
            break;
        default:
            throw new IllegalArgumentException("Unsupported transport layer");
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    private ConnServerClient accept() throws IOException {
        return new ConnServerClient(this, StandardNetChannel.fromSocket(server.accept()));
    }

    public void listen() throws IOException {
        while (true) {
            accept().handle();
        }
    }

    public void addClient(ConnServerClient client) {
        LOG.info("add {}", client);
    }

    public void removeClient(ConnServerClient client) {
        LOG.info("remove {}", client);
    }
}
