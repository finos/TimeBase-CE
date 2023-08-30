/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.util.vsocket.netio;

import com.epam.deltix.util.cmdline.DefaultApplication;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Date: Mar 1, 2010
 */
public class SocketLatencyTest extends DefaultApplication {

    SocketLatencyTest (String [] args) {
        super(args);
    }

    @Override
    public void printUsage(OutputStream os) throws IOException, InterruptedException {
        PrintStream out = os instanceof PrintStream ? (PrintStream)os : new PrintStream(os);

        out.println("Usage: -local OR -server OR -client    Specifies test mode");
        out.println("Optional parameters: ");
        out.println("      -host <hostname>     - hostname in client mode or interface in server mode");
        out.println("      -port <value>        - connection port (7777)");
        out.println("      -count <value>       - number of messages");
        out.println("      -minPacketSize <value> - min packet size (1)");
        out.println("      -maxPacketSize <value> - max packet size (1024)");
    }

    @Override
    protected void run() throws Throwable {
        final int minPacketSize = getIntArgValue("-minPacketSize", 1);
        final int maxPacketSize = getIntArgValue("-maxPacketSize", 1024);
        final int port = getIntArgValue("-port", 7777);
        final int numberOfIterations = getIntArgValue("-count", 10000);

        if (isArgSpecified("-local")) {
            Server              server = new Server (port, minPacketSize, maxPacketSize, numberOfIterations, InetAddress.getByName("localhost"));
            server.start ();
            for (int i = 0; i < 100; i++ )
                client ("localhost", port, numberOfIterations, minPacketSize, maxPacketSize);
        } else {
            if (isArgSpecified("-server")) {
                String host = getArgValue("-host", null);
                Server              server = new Server (port, minPacketSize, maxPacketSize, numberOfIterations, (host != null) ? InetAddress.getByName(host) : null);
                server.start ();
                Thread.sleep(Long.MAX_VALUE); //TODO
            } else if (isArgSpecified("-client")) {
                String host = getArgValue("-host", "localhost");
                while (true)
                    client (host, port, numberOfIterations, minPacketSize, maxPacketSize);
            } else {
                throw new Exception("ERROR: Specify test mode (-local | -server | -client)");
            }
        }
    }


    public static class Server extends Thread {
        private final ServerSocket ss;
        private final int minPacketSize, maxPacketSize;
        private final byte []       buffer;
        private final int numberOfIterations;

        public Server (int port, int minPacketSize, int maxPacketSize, int numberOfIterations, InetAddress iface) throws IOException {
            super("TestServer@ " + iface.getHostName() + ':' + port);

            ss = new ServerSocket (port, 50, iface);
            ss.setReuseAddress(true);

            this.numberOfIterations = numberOfIterations;
            buffer = new byte [maxPacketSize];
            this.minPacketSize = minPacketSize;
            this.maxPacketSize = maxPacketSize;
        }

        @Override
        public void run () {

            System.out.println (
                "Server listening on port " + ss.getLocalPort () + "; number of iteration: " + numberOfIterations + " maxPacketSize: " + buffer.length
            );
            
            for (;;) {

                try (Socket s = ss.accept ()) {
                    s.setTcpNoDelay (true);
                    System.out.println ("Server: connection accepted from " + s.getInetAddress());

                    DataInputStream in = new DataInputStream (s.getInputStream ());
                    OutputStream out = s.getOutputStream();
                    while (true)
                        serverRun(in, out);

                } catch (EOFException eof) {
                    System.out.println ("EOF"); // disconnect
                } catch (Throwable x) {
                    x.printStackTrace ();
                }
            }
        }

        private void serverRun(DataInputStream in, OutputStream out) throws IOException {

            for (int packetSize = minPacketSize; packetSize <= maxPacketSize; packetSize = packetSize << 1) {
                for (int i=0; i < numberOfIterations; i++) {
                    in.readFully(buffer, 0, packetSize);
                    out.write(buffer, 0, packetSize);
                    //TODO: out.flush ();
                }
            }
        }
    }

    public static void  client (String host, int port, int numberOfIterations, int minPacketSize, int maxPacketSize)
            throws IOException, InterruptedException 
    {
        try (Socket socket = new Socket (host, port)) {
            socket.setTcpNoDelay (true);
            socket.setSoTimeout (0);
            socket.setKeepAlive (true);
            socket.setReuseAddress (true);

            OutputStream        out = socket.getOutputStream ();
            DataInputStream     in = new DataInputStream (socket.getInputStream ());

            while (true)
                clientRun(numberOfIterations, minPacketSize, maxPacketSize, out, in);
        }
    }

    private static void clientRun(final int numberOfIterations, final int minPacketSize, final int maxPacketSize, final OutputStream out, final DataInputStream in) throws IOException, InterruptedException {
        final byte[] buffer = new byte[maxPacketSize];
        final int[] latencies = new int[numberOfIterations];

        for (int ii = 0; ii < buffer.length; ii++)
            buffer[ii] = (byte) ii;

        final int delay = 5;

        System.out.println ("---------------------------------------------------------------------------------------------------------");
        System.out.println ("ROUND TRIP latencies in microseconds [ #iterations: " + numberOfIterations + "; approx rate: " + 1000000/delay + " msg/sec ]");
        System.out.println ("---------------------------------------------------------------------------------------------------------");
        System.out.println ("    #bytes\t     MIN\t 50.000%\t 90.000%\t 99.000%\t 99.900%\t 99.990%\t 99.999%\t      MAX");


        for (int packetSize = minPacketSize; packetSize <= maxPacketSize; packetSize = packetSize << 1) {

            runClient(numberOfIterations, out, in, buffer, latencies, delay, packetSize);
            Arrays.sort(latencies);
            System.out.printf("%8d\t%8d\t%8d\t%8d\t%8d\t%8d\t%8d\t%8d\t%8d\n",
                packetSize,
                latencies[0],
                latencies[numberOfIterations / 2],
                latencies[(int) (numberOfIterations * 0.90000)],
                latencies[(int) (numberOfIterations * 0.99000)],
                latencies[(int) (numberOfIterations * 0.99900)],
                latencies[(int) (numberOfIterations * 0.99990)],
                latencies[(int) (numberOfIterations * 0.99999)],
                latencies[numberOfIterations - 1]);
        }

        Thread.sleep(10);
    }

    private static void runClient(int numberOfIterations, OutputStream out, DataInputStream in, byte[] buffer, int[] latencies, int delay, int packetSize) throws IOException, InterruptedException {
        for (int ii = 0; ii < numberOfIterations; ii++) {
            final long startTime = System.nanoTime();

            out.write(buffer, 0, packetSize);
            //TODO: out.flush ();
            in.readFully(buffer, 0, packetSize);

            latencies[ii] = (int) (System.nanoTime() - startTime) / 1000;

            Thread.sleep(delay); // controls message rate
        }
    }

    public static void main (String [] args) throws Exception {
        new SocketLatencyTest(args).start();
    }
}