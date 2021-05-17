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

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.memory.DataExchangeUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class SocketTest {
    public static final int     NUM_PACKETS = 100000;

    public static class Server extends Thread {
        public final ServerSocket   ss;
        private final byte []       buffer;

        private final long[]        results = new long[NUM_PACKETS];

        public Server (int port, int packetSize) throws IOException {
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);

            ss = new ServerSocket (port);
            ss.setReuseAddress(true);
            buffer = new byte [packetSize];
        }

        @Override
        public void run () {
            System.out.println ("Server listening on port " + ss.getLocalPort () + "; packet size: " + buffer.length);

            try (Socket s = ss.accept ()) {
                final DataInputStream in = new DataInputStream (s.getInputStream ());
                final DataOutputStream out = new DataOutputStream (s.getOutputStream());

                System.out.println ("Server: connection accepted.");
                System.out.println ("Time,Min,Avg,99%,99.9%,99.99%,Max");
                
                int index = 0;

                for (;;) {
                    in.readFully (buffer);
                    long current = System.nanoTime();

                    long time = DataExchangeUtils.readLong (buffer, 0);
                    results [index] = current - time;

                    index ++;

                    if (index == NUM_PACKETS) {
                        printStatistics();
                        index = 0;
                    }

                    out.writeLong (index);
                    out.flush ();
                }
            } catch (EOFException eof) {
                // disconnect
            } catch (Throwable x) {
                x.printStackTrace ();
            }

            IOUtil.close (ss);
        }

        private final StringBuilder sb = new StringBuilder ();
        
        private void printStatistics() {
            Arrays.sort (results);

            long      sum = 0;
            
            for (long x : results)
                sum += x;

            sb.setLength (0);
            
            sb.append (System.currentTimeMillis ());
            sb.append (',');
            
            sb.append (results [0]);
            sb.append (',');
            
            sb.append (sum / NUM_PACKETS);
            sb.append (',');
            
            sb.append (results [(int) (NUM_PACKETS * 0.99)]);
            sb.append (',');
            
            sb.append (results [(int) (NUM_PACKETS * 0.999)]);
            sb.append (',');
            
            sb.append (results [(int) (NUM_PACKETS * 0.9999)]);
            sb.append (',');
            
            sb.append (results [NUM_PACKETS - 1]);
            sb.append ('\n');
            
            synchronized (System.out) {
                for (int ii = 0; ii < sb.length (); ii++)
                    System.out.print (sb.charAt (ii));
            }
            
            Arrays.fill (results, 0);
        }
    }

    public static void  client (String host, int port, int packetSize)
            throws IOException, InterruptedException {
        byte []             buffer = new byte [packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer [ii] = (byte) ii;

        try (final Socket socket = new Socket (host, port)) {
            socket.setTcpNoDelay (true);
            socket.setSoTimeout (0);
            socket.setKeepAlive(true);
            socket.setReuseAddress(true);

            final OutputStream      out = socket.getOutputStream ();
            final DataInputStream   in = new DataInputStream(socket.getInputStream());

            for (;;) {
                DataExchangeUtils.writeLong (buffer, 0, System.nanoTime ());

                out.write (buffer);
                out.flush ();

                in.readLong();
            }           
        }
    }

    public static void main (String [] args) throws Exception {
        if (args.length == 0)
            args = new String [] { "64" };

        // warmup
        long[] times = new long[1000];
        for (int i = 0; i < times.length; i++)
            times[i] = System.nanoTime();

        int                 packetSize = Integer.parseInt (args [0]);
        Server              server = new Server (0, packetSize);

        server.start ();

        client ("localhost", server.ss.getLocalPort(), packetSize);
    }
}


