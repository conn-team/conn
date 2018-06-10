package com.github.connteam.conn.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLServerSocketFactory;

import com.github.connteam.conn.core.net.NetChannel;
import com.github.connteam.conn.core.net.StandardNetChannel;
import com.github.connteam.conn.core.net.Transport;
import com.github.connteam.conn.server.database.model.UserEntry;
import com.github.connteam.conn.server.database.provider.DataProvider;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnServer implements Closeable {
    private final static Logger LOG = LoggerFactory.getLogger(ConnServer.class);

    private final ServerSocket server;
    private final DataProvider database;
    private final ConcurrentHashMap<String, ConnServerClient> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<ConnServerClient>> observedBy = new HashMap<>();

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

    public ConnServerClient getClientByName(String username) {
        ConnServerClient client = clients.get(username);
        return (client != null && client.isEstablished() ? client : null);
    }

    protected boolean addClient(ConnServerClient client) {
        UserEntry user = client.getUser();
        if (user != null && clients.putIfAbsent(user.getUsername(), client) == null) {
            NetChannel conn = client.getNetChannel();
            LOG.info("{} connected from {}:{}", user.getUsername(), conn.getAddress().getHostName(), conn.getPort());
            return true;
        }
        return false;
    }

    protected boolean removeClient(ConnServerClient client, Exception err) {
        UserEntry user = client.getUser();
        if (user != null && clients.remove(user.getUsername(), client)) {
            NetChannel conn = client.getNetChannel();
            LOG.info("{} disconnected from {}:{}", user.getUsername(), conn.getAddress().getHostName(), conn.getPort(),
                    err);
            return true;
        }
        return false;
    }

    protected boolean addObserver(String target, ConnServerClient client) {
        synchronized (observedBy) {
            Set<ConnServerClient> observers = observedBy.get(target);
            if (observers == null) {
                observers = new HashSet<>();
                observedBy.put(target, observers);
            }
            return observers.add(client);
        }
    }

    protected boolean removeObserver(String target, ConnServerClient client) {
        synchronized (observedBy) {
            Set<ConnServerClient> observers = observedBy.get(target);
            if (observers != null && observers.remove(client)) {
                if (observers.isEmpty()) {
                    observedBy.remove(target);
                }
                return true;
            }
            return false;
        }
    }

    protected void notifyObservers(String source, Message msg) {
        ConnServerClient[] clients = null;

        synchronized (observedBy) {
            Set<ConnServerClient> observers = observedBy.get(source);
            if (observers != null) {
                clients = observers.toArray(new ConnServerClient[observers.size()]);
            }
        }

        if (clients != null) {
            for (ConnServerClient client : clients) {
                client.getNetChannel().sendMessage(msg);
            }
        }
    }
}
