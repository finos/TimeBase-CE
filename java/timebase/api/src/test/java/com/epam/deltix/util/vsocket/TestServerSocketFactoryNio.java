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
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TestServerSocketFactoryNio {
    public static abstract class ServerThread extends Thread {
        protected ServerSocket ss;

        public int getLocalPort(){
            return ss.getLocalPort();
        }
        public String getLocalHost(){
            return ss.getInetAddress().getHostAddress();
        }

        protected void setUtSocket(ServerSocket socket) throws SocketException {
            socket.setReuseAddress(true);
            socket.setSoTimeout (0);
        }
    }

    public static class ThroughputServerSocket extends ServerThread {
        private final byte [] buffer;

        public ThroughputServerSocket (int port, int bufferCapacity) throws IOException {
            ss = new ServerSocket (port);
            setUtSocket(ss);

            buffer = new byte [bufferCapacity];
        }

        @Override
        public void run () {
            Socket s = null;

            try {
                System.out.println ("Server: listening on port " + ss.getLocalPort ());
                for(;;) {
                    s = ss.accept ();   // Only one accept is handled

                    DataInputStream in = new DataInputStream (s.getInputStream ());
                    System.out.println ("Server: connection accepted.");

                    while(s.isConnected())
                    {
                        in.read(buffer);
                        //in.readFully (buffer);
                    }
                }
            } catch (Throwable x) {
                x.printStackTrace ();
            } finally {
                IOUtil.close (s);
                IOUtil.close (ss);
            }
        }
    }

    public static class LatencyServerSocket extends ServerThread {
        public static final int     NUM_MESSAGES = 10000;
        public static final int     NUM_PER_BURST = 100;

        private final byte[] buffer;

        public LatencyServerSocket(int port, int packetSize) throws IOException {
            ss = new ServerSocket(port);
            setUtSocket(ss);

            buffer = new byte[packetSize];
        }

        @Override
        public void run() {
            Socket s = null;
            System.out.println("Server listening on port " + ss.getLocalPort() + "; packet size: " + buffer.length);

            try {
                s = ss.accept();   // Only one accept is handled
                System.out.println("Server: connection accepted.");

                DataInputStream in = new DataInputStream(s.getInputStream());
                DataOutputStream out = new DataOutputStream(s.getOutputStream());

                SocketTestUtilities.proccessLatencyRequests(out, in, buffer, false);
            }  catch (Throwable x) {
                x.printStackTrace();
            } finally {
                IOUtil.close(s);
            }


            IOUtil.close(ss);
        }
    }

    public static class EchoServerSocket extends ServerThread {

        public EchoServerSocket (int port) throws IOException {
            ss = new ServerSocket (port);
            setUtSocket(ss);
        }

        @Override
        public void run () {
            Socket          s = null;

            try {
                System.out.println ("Server: listening on port " + ss.getLocalPort ());

                for(;;)
                {
                    s = ss.accept ();   // Only one accept is handled

                    DataInputStream     in = new DataInputStream (s.getInputStream ());
                    DataOutputStream    out = new DataOutputStream (s.getOutputStream ());

                    System.out.println ("Server: connection accepted.");


                    String utfString = in.readUTF ();
                    out.writeUTF (utfString);
                    out.flush ();
                }
            } catch (Throwable x) {
                x.printStackTrace ();
            } finally {
                IOUtil.close (s);
                IOUtil.close (ss);
            }
        }
    }

    public static ServerThread createThroughputServerSocket(int port, int packetSize) throws IOException {
        ThroughputServerSocket serverSocket = new ThroughputServerSocket(port, packetSize);
        serverSocket.setDaemon(true);
        serverSocket.setPriority(Thread.MAX_PRIORITY);
        return serverSocket;
    }

    public static ServerThread createLatencyServerSocket(int port, int packetSize) throws IOException {
        LatencyServerSocket serverSocket = new LatencyServerSocket(port, packetSize);
        serverSocket.setDaemon(true);
        serverSocket.setPriority(Thread.MAX_PRIORITY);
        return serverSocket;
    }

    public static ServerThread createEchoServerSocket(int port) throws IOException {
        EchoServerSocket serverSocket = new EchoServerSocket(port);
        serverSocket.setDaemon(true);
        serverSocket.setPriority(Thread.MAX_PRIORITY);
        return serverSocket;
    }
}