package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLServerSocketFactory;

import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.database.model.UserEntry;
import com.github.connteam.conn.server.database.provider.DataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServer implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServer.class);

    private final ServerSocket server;
    private final DataProvider database;
    private final ConcurrentHashMap<String, ConnServerClient> clients = new ConcurrentHashMap<>();

    public static class Builder {
        private Integer port;
        private Transport transport;
        private DataProvider database;

        private Builder() {
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setTransport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public Builder setDataProvider(DataProvider database) {
            this.database = database;
            return this;
        }

        public ConnServer build() throws IOException {
            if (port == null || transport == null || database == null) {
                throw new IllegalStateException("Missing builder parameters");
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

        database = builder.database;
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    public DataProvider getDataProvider() {
        return database;
    }

    private ConnServerClient accept() throws IOException {
        return new ConnServerClient(this, StandardNetChannel.fromSocket(server.accept()));
    }

    public void listen() throws IOException {
        while (true) {
            LOG.info("Listening on :{}", server.getLocalPort());
            accept().handle();
        }
    }

    public boolean addClient(ConnServerClient client) {
        UserEntry user = client.getUser();
        if (user != null && clients.putIfAbsent(user.getUsername(), client) == null) {
            NetChannel conn = client.getNetChannel();
            LOG.info("{} connected from {}:{}", user.getUsername(), conn.getAddress().getHostName(), conn.getPort());
            return true;
        }
        return false;
    }

    public boolean removeClient(ConnServerClient client, Exception err) {
        UserEntry user = client.getUser();
        if (user != null && clients.remove(user.getUsername(), client)) {
            NetChannel conn = client.getNetChannel();
            LOG.info("{} disconnected from {}:{}", user.getUsername(), conn.getAddress().getHostName(), conn.getPort(),
                    err);
            return true;
        }
        return false;
    }

    public ConnServerClient getClientByName(String username) {
        ConnServerClient client = clients.get(username);
        return (client != null && client.isEstablished() ? client : null);
    }
}
