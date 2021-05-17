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

import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;
import org.junit.Test;

import java.io.IOException;

public class Test_VSocketLatency {
    public static void main (String [] args) throws Exception {
        int port = SocketTestUtilities.parsePort(args);
        int packetSize = SocketTestUtilities.parsePacketSize(args, 1024);

        VSServer server = TestVServerSocketFactory.createLatencyVServer(port, packetSize);
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        client ("localhost", server.getLocalPort(), packetSize, 10);
    }

    public static void  client (String host, int port, int packetSize, int cycles)
            throws IOException, InterruptedException {
        byte[] buffer = new byte[packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer[ii] = (byte) ii;

        VSClient c = new VSClient(host, port);
        c.connect();

        VSChannel s = c.openChannel();
        s.setAutoflush(true);

        boolean measure = true;
        SocketTestUtilities.measureLatency(s.getDataOutputStream(), s.getDataInputStream(), buffer, cycles, measure);
    }

    @Test
    public void TestSocket() throws Throwable {
        Test_VSocketLatency.main(new String[0]);
    }
}
