/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Assert;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/*  ##UTILS## */

/**
 *
 */
public class VSocketRestoreTest {

    private static final int NUMBER_OF_CLIENTS = 1;

    private VSServer server;
    private Thread socketKiller;

    private volatile boolean testIsRunning = true;

    private static final AtomicInteger serverIndex = new AtomicInteger();

    public class Server extends QuickExecutor.QuickTask {
        private VSChannel           channel;
        private final byte []       buffer;

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

                long index = 0;
                while (testIsRunning) {

                    in.readFully (buffer);

                    long first = DataExchangeUtils.readLong(buffer, 0);
                    long second = DataExchangeUtils.readLong(buffer, 8);
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

    private void  client(String host, int port, int packetSize, int numberOfMessages, int clientNumber)
            throws IOException
    {
        Thread.currentThread().setName("TestClient-" + clientNumber);
        byte []             buffer = new byte [packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer [ii] = (byte) ii;

        final VSClient            c = new VSClient (host, port);
        //c.setNumTransportChannels(1);
        c.connect ();

        VSChannel           s = c.openChannel(VSProtocol.CHANNEL_BUFFER_SIZE, VSProtocol.CHANNEL_BUFFER_SIZE, true);
        s.setAutoflush (true);
        OutputStream        os = s.getOutputStream ();
        AtomicInteger counter = new AtomicInteger(0);
        long                lastReportTime = TimeKeeper.currentTime;
        long                nextReportTime = lastReportTime + 1000;
        long                lastReportedCount = 0;


        socketKiller = new Thread(new SocketKiller(counter, c.getDispatcher(), clientNumber));
        socketKiller.start();

        long index = 0;
        DataExchangeUtils.writeLong(buffer, 0, index++);
        DataExchangeUtils.writeLong(buffer, 8, index++);

        while (index < numberOfMessages) {

            os.write (buffer);
            DataExchangeUtils.writeLong(buffer, 0, index++);
            DataExchangeUtils.writeLong(buffer, 8, index++);

            counter.incrementAndGet();

            long            now = TimeKeeper.currentTime;

            if (now > nextReportTime) {
                long        num = counter.intValue() - lastReportedCount;
                double      sec = (now - lastReportTime) * 0.001;
                double      rate = num / sec;

                System.out.printf (
                        "%,d m/s; %,d B/s \n",
                        (int) rate,
                        num * packetSize
                );


                lastReportedCount = counter.intValue();
                lastReportTime = now;
                nextReportTime = now + 1000;
            }
        }
    }

    private static class SocketKiller implements Runnable {
        private int count = 0;

        private final AtomicInteger    counter;
        private final VSDispatcher  vssispatcher;
        private int clientNumber;

        private SocketKiller(AtomicInteger counter, VSDispatcher vssispatcher, int clientNumber) {
            this.counter = counter;
            this.vssispatcher = vssispatcher;
            this.clientNumber = clientNumber;
        }


        @Override
        public void run() {
            Thread.currentThread().setName("SocketKiller-" + clientNumber);
            Random rnd = new Random(2010);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            while (true) {
                try {
                    int time = rnd.nextInt(1000) + 50;
                    if (count != counter.get()) {
                        //System.out.println("Sleeping time:" + time);
                        Thread.sleep(time);
                        vssispatcher.closeTransport();

                        count = counter.intValue();
                    } else {
                        // Wait for client to advance
                        Thread.sleep(1);
                    }
                }
                catch (Exception e) {
                    if ( e instanceof InterruptedException)
                        break;
                    else
                        e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void test () throws Exception {
        clientServer(67345, 100_000_000);

        // cleanup
        testIsRunning = false;
        socketKiller.interrupt();
        server.close();
        socketKiller.join(5000);
        server.join(5000);
    }

    public static void main (String [] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "1024" };

        VSocketRestoreTest test = new VSocketRestoreTest();

        if (args.length == 1) {
            test.clientServer(Integer.parseInt(args[0]), Integer.MAX_VALUE);
        } else {
            test.client(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.MAX_VALUE, 0);
        }
    }

    private void clientServer(final int packetSize, final  int numberOfMessages) throws IOException {
        server = startServer(packetSize);

        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            int clientId = i;
            new Thread() {
                @Override
                public void run() {
                    try {
                        client("localhost", server.getLocalPort(), packetSize, numberOfMessages, clientId + 1);
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                    System.out.println("Client finished: " + clientId);
                }
            }.start();
        }

        client ("localhost", server.getLocalPort(), packetSize, numberOfMessages, 0);
    }

    private VSServer startServer(final int packetSize) throws IOException {
        final VSServer      server = new VSServer ();

        server.setConnectionListener (
                new VSConnectionListener() {
                    public void connectionAccepted (QuickExecutor executor, final VSChannel serverChannel) {
                        System.out.println ("Server: connection accepted.");

                        try {
                            new Server(executor, serverChannel, packetSize).submit ();
                        } catch (Throwable x) {
                            x.printStackTrace ();
                            Assert.fail("Error in server thread: " + x.getMessage());
                        }
                    }
                }
        );

        System.out.println("Server listening on port " + server.getLocalPort() + "; packet size: " + packetSize);
        server.start ();
        return server;
    }
}