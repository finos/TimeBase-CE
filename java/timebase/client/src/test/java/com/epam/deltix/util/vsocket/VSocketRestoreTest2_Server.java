package com.epam.deltix.util.vsocket;

import com.epam.deltix.gflog.jul.JulBridge;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.memory.DataExchangeUtils;
import org.junit.Assert;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


public class VSocketRestoreTest2_Server {

    private volatile boolean testIsRunning = true;

    private static final AtomicInteger serverIndex = new AtomicInteger();

    public class Server extends QuickExecutor.QuickTask {
        private VSChannel           channel;
        private final byte []       buffer;
        private long clientNumber;

        public Server (QuickExecutor executor, VSChannel channel, int bufferCapacity) throws IOException {
            super (executor);
            this.channel = channel;
            this.buffer = new byte [bufferCapacity];
        }

        @Override
        public void run () {
            try {
                Thread.currentThread().setName("TestServer-" + serverIndex.incrementAndGet());
                DataInputStream     in = new DataInputStream (channel.getInputStream ());

                boolean threadNameWasSet = false;

                long index = 0;
                while (testIsRunning) {

                    in.readFully (buffer);
                    long size = DataExchangeUtils.readLong(buffer, 0);
                    if (size != buffer.length) {
                        System.out.println("Buffer size mismatch: expected " + buffer.length + " actual: " + size);
                    }

                    long first = DataExchangeUtils.readLong(buffer, 8);
                    long second = DataExchangeUtils.readLong(buffer, 16);
                    this.clientNumber = DataExchangeUtils.readLong(buffer, 24);

                    if (!threadNameWasSet) {
                        threadNameWasSet = true;
                        Thread.currentThread().setName("TestServer-" + serverIndex.incrementAndGet() + " for client " + this.clientNumber);
                    }

                    Assert.assertEquals ("first = " + first + "; second = " + second, 1, second - first);
                    Assert.assertEquals ("first(" + first + ") != index (" + index + ")", first, index);

//                    if (index > first) {
//                        System.out.println("==== Recieved old data:" + first + "; current = " + index);
//                    } else {
//                        assert (first == index) : "first[" + first + "] != index [" + index + "]";
//                    }

                    index += 2;

                }
            } catch (Throwable x) {
                x.printStackTrace ();
            } finally {
                channel.close ();
            }
        }
    }

    public static void main (String [] args) throws Exception {
        JulBridge.install();
        if (args.length == 0) {
            args = new String[]{
                    "8011",
                    "67345"
            };
        }

        int port = Integer.parseInt(args[0]);
        int packetSize = Integer.parseInt(args[1]);

        Thread debugThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println("Debug thread interrupted");
                        return;
                    }
                }
            }
        });
        debugThread.setName("Debug thread");
        debugThread.start();

        VSocketRestoreTest2_Server test = new VSocketRestoreTest2_Server();
        test.startServer(packetSize, port);
    }


    private static final AtomicInteger connectionIndex = new AtomicInteger(0);

    private VSServer startServer(final int packetSize, int port) throws IOException {
        final VSServer      server = new VSServer (port);

        server.setConnectionListener (
                new VSConnectionListener() {
                    public void connectionAccepted (QuickExecutor executor, final VSChannel serverChannel) {
                        int connectionIndex = VSocketRestoreTest2_Server.connectionIndex.incrementAndGet();
                        System.out.println ("Server: connection " + connectionIndex + " accepted.");

                        try {
                            new Server(executor, serverChannel, packetSize).submit ();
                        } catch (Throwable x) {
                            x.printStackTrace ();
                            Assert.fail("Error in server thread for connection " + connectionIndex + ": " + x.getMessage());
                        }
                    }
                }
        );

        System.out.println("Server listening on port " + server.getLocalPort() + "; packet size: " + packetSize);
        server.start ();
        return server;
    }
}