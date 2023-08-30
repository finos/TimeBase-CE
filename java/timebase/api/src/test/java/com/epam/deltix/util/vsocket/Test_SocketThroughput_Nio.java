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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestServerSocketFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 */
public class Test_SocketThroughput_Nio {

   public static void main (String [] args) throws IOException {
        int packetSize = SocketTestUtilities.parsePacketSize(args);
        int port = SocketTestUtilities.parsePort(args);

        TestServerSocketFactory.ServerThread server = TestServerSocketFactory.createThroughputServerSocket(port, packetSize);
        server.start ();

        client ("localhost", server.getLocalPort(), packetSize, 15);
   }

   public static void  client (String host, int port, int packetSize, int cycle)
        throws IOException {
       byte[] buffer = new byte[packetSize];

       for (int ii = 0; ii < packetSize; ii++)
           buffer[ii] = (byte) ii;

       Socket s = null;
       try {
           s = new Socket();
           s.connect(new InetSocketAddress(host, port), 5000);
           s.setTcpNoDelay (true);
           s.setSoTimeout (0);
           s.setKeepAlive (true);
           s.setReceiveBufferSize(1 << 16);
           s.setSendBufferSize(1 << 16);

           OutputStream os = s.getOutputStream();

           SocketTestUtilities.measureOutputThroughput(os, buffer, cycle);
       } catch (Throwable x) {
           x.printStackTrace();
           throw x;
       } finally {
           IOUtil.close(s);
       }
   }

    @Test
    public void TestSocket() throws IOException{
        Test_SocketThroughput_Nio.main(new String[0]);
    }
}