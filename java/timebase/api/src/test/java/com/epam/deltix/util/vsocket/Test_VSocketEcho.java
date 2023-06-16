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

import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;

import java.io.*;

public class Test_VSocketEcho {
    public static void      main (String args []) throws Throwable {
        int port = SocketTestUtilities.parsePort(args);

        VSServer server = TestVServerSocketFactory.createEchoVServer(port);
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        client("localhost", server.getLocalPort());
    }

    private static void client(String host, int port) throws IOException {
        VSClient            client = new VSClient (host, port);
        VSChannel           channel = null;
        String              s = "Hello world";
        int                 counter = 0;
        long                lastReportTime = TimeKeeper.currentTime;
        long                nextReportTime = lastReportTime + 1000;
        long                lastReportedCount = 0;

        try {
            client.connect ();

            for (;;) {
                channel = client.openChannel ();

                DataOutputStream    os = channel.getDataOutputStream ();
                os.writeUTF (s);
                os.flush ();

                String              check = channel.getDataInputStream ().readUTF ();

                if (!s.equals (check))
                    throw new AssertionError (check + " != " + s);

                channel.close ();

                counter++;

                long            now = TimeKeeper.currentTime;

                if (now > nextReportTime) {
                    long        num = counter - lastReportedCount;
                    double      sec = (now - lastReportTime) * 0.001;
                    double      rate = num / sec;

                    System.out.println ((int) rate + " packets/sec");

                    lastReportedCount = counter;
                    lastReportTime = now;
                    nextReportTime = now + 1000;
                }
            }
        } catch (IOException x) {
            x.printStackTrace ();
        } finally {
            if (channel != null)
                channel.close ();

            client.close ();
        }
    }

    public void TestSocket() throws Throwable {
        Test_VSocketEcho.main(new String[0]);
    }
}