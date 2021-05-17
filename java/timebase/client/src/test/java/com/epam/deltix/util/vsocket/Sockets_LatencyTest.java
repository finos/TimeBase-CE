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
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Date: Mar 1, 2010
 */
public class Sockets_LatencyTest {
    public static final int     NUM_MESSAGES = 10000;
    public static final int     NUM_PER_BURST = 100;

    public static class Server extends Thread {
        public final ServerSocket ss;
        private final byte []       buffer;

        public Server (int port, int packetSize) throws IOException {
            ss = new ServerSocket (port);
            ss.setReuseAddress(true);
            buffer = new byte [packetSize];
        }

        @Override
        public void run () {
            long  count = 1;
            double minLatency = Long.MAX_VALUE;
            double maxLatency = 0;
            double avgLatency = 0;
            long[] result = new long[NUM_MESSAGES];

            Socket s = null;
            System.out.println("Server listening on port " + ss.getLocalPort () + "; packet size: " + buffer.length);
            int connects = 0;

            while (connects++ < 100) {
                minLatency = Long.MAX_VALUE;
                maxLatency = 0;
                avgLatency = 0;
                count = 1;

                try {
                    s = ss.accept ();   // Only one accept is handled

                    DataInputStream in = new DataInputStream (s.getInputStream ());
                    DataOutputStream out = new DataOutputStream (s.getOutputStream());

                    System.out.println ("Server: connection accepted.");

                    for (;;) {
                        in.readFully (buffer);
                        out.writeLong(count);
                        out.flush();
//                        long time = DataExchangeUtils.readLong(buffer, 0);
//
//                        long latency = (System.nanoTime() - time) / 1000;
//
//                        if (latency > 0) {
//                            minLatency = Math.min(minLatency, latency);
//                            maxLatency = Math.max(maxLatency, latency);
//                            avgLatency = (avgLatency * (count - 1) + latency) / count;
//                        }
                        //result[(int)count] = latency;

                        count ++;
                    }
                } catch (EOFException eof) {
                   // disconnect
                } catch (Throwable x) {
                    x.printStackTrace ();
                } finally {
                    IOUtil.close(s);

//                    System.out.printf ("Max Latency %,.0f ns \n", maxLatency * 1000);
//                    System.out.printf ("Min Latency %,.0f ns \n", minLatency * 1000);
//                    System.out.printf ("AVG Latency %,.0f ns \n", avgLatency * 1000);
//                    System.out.println ("--------------------------------------------");
                }
            }

            IOUtil.close (ss);
        }
    }

    public static void  client (String host, int port, int packetSize)
            throws IOException, InterruptedException {
        byte []             buffer = new byte [packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer [ii] = (byte) ii;

        Socket              socket = new Socket (host, port);
        socket.setTcpNoDelay (true);
        socket.setSoTimeout (0);
        socket.setKeepAlive(true);

        socket.setReuseAddress(true);

        OutputStream        out = socket.getOutputStream ();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int                 counter = 0;
//        long                lastReportTime = TimeKeeper.currentTime;
//        long                nextReportTime = lastReportTime + 1000;
//        long                lastReportedCount = 0;

        double minLatency = Long.MAX_VALUE;
        double maxLatency = 0;
        double avgLatency = 0;

        while (counter++ < NUM_MESSAGES ) {
            for (int i = 0; i < NUM_PER_BURST; i++) {
                DataExchangeUtils.writeLong(buffer, 0, System.nanoTime());

                long start = System.nanoTime();

                out.write(buffer);
                out.flush();
                long count = in.readLong();

                long latency = (System.nanoTime() - start) / 1000;

                if (latency > 0) {
                    minLatency = Math.min(minLatency, latency);
                    maxLatency = Math.max(maxLatency, latency);
                    avgLatency = (avgLatency * (count - 1) + latency) / count;
                }

                counter++;
            }

            out.flush();

            Thread.sleep(10);
        }

        System.out.printf ("Max Latency %,.0f ns \n", maxLatency * 1000);
        System.out.printf ("Min Latency %,.0f ns \n", minLatency * 1000);
        System.out.printf ("AVG Latency %,.0f ns \n", avgLatency * 1000);
        System.out.println ("--------------------------------------------");
        
        socket.close();
        Thread.sleep(100);
    }

    public static void main (String [] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "1024" };

        if (args.length == 1) {
            int                 packetSize = Integer.parseInt (args [0]);
            Server              server = new Server (8011, packetSize);

            server.start ();

            for (int i = 0; i < 10000; i++ )
                client ("localhost", server.ss.getLocalPort(), packetSize);
        } else {

            for (int i = 0; i < 10000; i++ )
                client (args [0], Integer.parseInt(args[1]), Integer.parseInt (args [2]));
        }
   }
}


