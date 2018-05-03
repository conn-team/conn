package com.github.connteam.conn.server.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocketFactory;

import com.github.connteam.conn.core.net.StandardNetChannel;

public class NetServer implements Closeable {
    private final ServerSocket server;

    public NetServer(ServerSocket socket) {
        this.server = socket;
    }

    public static NetServer listen(int port) throws IOException {
        ServerSocket socket = SSLServerSocketFactory.getDefault().createServerSocket(port);

        try {
            return new NetServer(socket);
        } catch (Throwable e) {
            if (!socket.isClosed()) {
                socket.close();
            }
            throw e;
        }
    }

	@Override
	public void close() throws IOException {
		server.close();
    }
    
    public NetServerClient accept() throws IOException {
        return new NetServerClient(StandardNetChannel.fromSocket(server.accept()));
    }
}
