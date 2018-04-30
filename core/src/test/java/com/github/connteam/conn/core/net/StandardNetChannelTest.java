package com.github.connteam.conn.core.net;

import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.connteam.conn.core.net.NetProtos.AuthRequest;
import com.github.connteam.conn.core.net.NetProtos.AuthResponse;
import com.github.connteam.conn.core.io.MessageRegistry;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.junit.Test;

public class StandardNetChannelTest {

    static class SyncNetChan implements NetChannel {
        static class Elem {
            Message msg;

            public Elem(Message m) {
                msg = m;
            }
        }

        public final LinkedBlockingQueue<Elem> incoming = new LinkedBlockingQueue<>();
        public final NetChannel channel;
        public final String tag;

        public SyncNetChan(String tag, Socket sock, MessageRegistry in, MessageRegistry out) throws IOException {
            this.tag = tag;

            channel = StandardNetChannel.builder().setSocket(sock).setInMessages(in)
                    .setOutMessages(out).setInMessageHandler(msg -> {
                        try {
                            System.out.println(tag + " in: " + msg.getClass());
                            incoming.put(new Elem(msg));
                        } catch (InterruptedException e) {
                            fail();
                        }
                    }).setCloseHandler(err -> {
                        try {
                            System.out.println(tag + " close: " + err);
                            incoming.put(new Elem(null));
                        } catch (InterruptedException e) {
                            fail();
                        }
                    }).build();
        }

        @Override
        public void close() {
            try {
                channel.close();
                assertEquals(false, isOpen());
                awaitTermination();
            } catch (InterruptedException e) {
                fail();
            }
        }

        @Override
        public void open() {
            channel.open();
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public IOException getError() {
            return channel.getError();
        }

        @Override
        public void sendMessage(Message msg) {
            System.out.println(tag + " out: " + msg.getClass());
            channel.sendMessage(msg);
        }
        
        public Message recvMessage() throws InterruptedException {
            return incoming.take().msg;
        }

        @Override
        public void awaitTermination() throws InterruptedException {
            channel.awaitTermination();
        }
    }

    @Test(timeout=3000)
    public void test() throws UnknownHostException, IOException, InterruptedException {
        final AuthRequest req = AuthRequest.newBuilder().setPayload(ByteString.copyFromUtf8("hello")).build();
        final AuthResponse resp = AuthResponse.newBuilder().setUsername("name")
                .setSignature(ByteString.copyFromUtf8("signature")).build();
        
        Thread clientThread = new Thread(() -> {
            try {
                try (SyncNetChan conn = new SyncNetChan("[C]", new Socket(InetAddress.getLocalHost(), 9090),
                        NetMessages.CLIENTBOUND, NetMessages.SERVERBOUND)) {
                    conn.open();
                    assertEquals(req, conn.recvMessage());
                    conn.sendMessage(resp);
                    assertEquals(null, conn.recvMessage());
                    assertEquals(EOFException.class, conn.getError().getClass());
                }
            } catch (IOException e) {
                fail();
            } catch (InterruptedException e) {
                fail();
            }
            
            System.out.println("[C] finished");
        });

        try (ServerSocket server = new ServerSocket(9090, 1, InetAddress.getLocalHost())) {
            clientThread.start();

            try (SyncNetChan conn = new SyncNetChan("[S]", server.accept(), NetMessages.SERVERBOUND,
                    NetMessages.CLIENTBOUND)) {
                conn.open();
                conn.sendMessage(req);
                assertEquals(resp, conn.recvMessage());
            }
        }

        System.out.println("[S] finished");
        clientThread.join();
    }
}
