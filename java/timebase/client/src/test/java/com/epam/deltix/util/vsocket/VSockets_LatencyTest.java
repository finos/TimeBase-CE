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

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.IOUtil;
import org.junit.Test;

import java.io.*;

/**
 * Date: Mar 1, 2010
 */
public class VSockets_LatencyTest {

    private TransportType             transportType = TransportType.SOCKET_TCP;

    public static void main (String ... args) throws Throwable {
        new VSockets_LatencyTest().Test();
    }

    @Test
    public void Test() throws IOException, InterruptedException {        
        VSServer    server = new VSServer ();

        server.setConnectionListener (
            new VSConnectionListener() {
                @Override
                public void connectionAccepted (QuickExecutor executor, VSChannel serverChannel) {
                    new LatencyServerThread (executor, serverChannel).submit();
                }
            }
        );

        server.start ();
        VSClient    client = new VSClient ("localhost", server.getLocalPort());
        client.connect ();

        for (int i = 0; i < 100; i++) {
            LatencyClientThread th = new LatencyClientThread(client);
            th.start();
            th.join();
        }

        client.close();
        server.close();
    }

    static class LatencyServerThread extends QuickExecutor.QuickTask {
        private final VSChannel     channel;

        //    private double minLatency;
//    private double maxLatency;
//    private double avgLatency;
        public static final int     NUM_MESSAGES = 100000;


        public LatencyServerThread (QuickExecutor executor, VSChannel serverChannel) {
            super (executor);
            this.channel = serverChannel;
        }

        @Override
        public void run () {
            long  count = 1;
            double minLatency = Long.MAX_VALUE;
            double maxLatency = 0;
            double avgLatency = 0;
            long[] result = new long[NUM_MESSAGES];

            try {
                DataInputStream in = new DataInputStream(channel.getInputStream ());

                for (;;) {
                    double             b = in.readDouble();

                    if (b < 0) {
                        //System.out.println ("SERVER: End of " + channel + "; b = " + b);
                        channel.getOutputStream ().write(1);
                        channel.getOutputStream ().flush();
                        //channel.getOutputStream ().close ();
                        break;
                    }
                    in.readDouble();
                    in.readDouble();
                    long time = in.readLong();
                    long latency = (System.nanoTime() - time) / 1000;

                    if (latency > 0) {
                        minLatency = Math.min(minLatency, latency);
                        maxLatency = Math.max(maxLatency, latency);
                        avgLatency = (avgLatency * (count - 1) + latency) / count;
                    }
                    result[(int)count] = latency;

                    count ++;
                }
            } catch (ChannelClosedException x) {
                System.out.println ("SERVER: Channel " + channel + " was closed by client.");
            } catch (IOException iox) {
                iox.printStackTrace ();
            } finally {
                System.out.printf ("Max Latency %,.0f ns \n", maxLatency * 1000);
                System.out.printf ("Min Latency %,.0f ns \n", minLatency * 1000);
                System.out.printf ("AVG Latency %,.0f ns \n", avgLatency * 1000);
                System.out.println ("--------------------------------------------");

                StringBuilder sb = new StringBuilder();
                for (long aResult : result) {
                    sb.append(aResult).append("\n");
                }
                File out = new File("C:\\TEMP\\" + channel.getLocalId() + ".csv");
                try {
                    IOUtil.writeTextFile(out, sb.toString());
                } catch (IOException e) {

                }
            }

            channel.setNoDelay(false);
            channel.close();
        }
    }

    static class LatencyClientThread extends Thread {
        public static final boolean PRINT_STATS = false;
        public static final int     NUM_MESSAGES = 100000;
        public static final int     NUM_PER_BURST = 100;

        private final VSChannel     channel;

        public LatencyClientThread (VSClient client) throws IOException {
            super ("Client test thread for " + client);
            this.channel = client.openChannel ();
        }

        @Override
        public void run () {
            try {
                OutputStream os = channel.getOutputStream ();
                channel.setAutoflush(true);
                channel.setNoDelay(true);
                DataOutputStream out = new DataOutputStream(os);

                for (int ii = 0; ii < NUM_MESSAGES; ii++) {

                    for (int j = 0; j < NUM_PER_BURST; j++) {
                        out.writeDouble(ii + 0.1);
                        out.writeDouble(ii + 1.1);
                        out.writeDouble(ii + 2.1);
                        out.writeLong(System.nanoTime());
                        ii++;
                    }
                    //out.flush();

                    Thread.sleep(10);
                }
                out.writeDouble(-1.0);
                out.flush();

                // wait for reply
                channel.getInputStream().read();

                os.close ();
            } catch (IOException iox) {
                iox.printStackTrace ();
            } catch (InterruptedException e) {

            } finally {
                channel.close();
            }

        }
    }

}