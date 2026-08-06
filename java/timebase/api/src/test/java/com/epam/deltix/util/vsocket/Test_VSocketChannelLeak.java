/*
 * Copyright 2024 EPAM Systems, Inc
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
import com.epam.deltix.util.lang.DisposableListener;
import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests if there is a memory leak in VSChannel when channel gets closed on the client side.
 *
 * <p>Run the test with -Xmx500m to see the problem.
 */
@SuppressWarnings("NewClassNamingConvention")
public class Test_VSocketChannelLeak {
    private static final boolean enableCloseFix = true;
    private static final int ITERATIONS = 1000;
    private static final int PAYLOAD_SIZE = 1_024 * 1_024; // 1 MB
    private static final boolean waitForFreeMem = false; // Needed for CI env - it's slow

//    public static void main (String [] args) throws Exception {
//        testImpl();
//        //Thread.sleep(Long.MAX_VALUE);
//    }

    @Ignore // Fails on CI
    @Test(timeout = 60_000)
    public void test() throws IOException, InterruptedException {
        testImpl();
    }

    @SuppressWarnings("Convert2Lambda")
    private static void testImpl() throws IOException, InterruptedException {
        VSServer server = new VSServer(0);

        AtomicLong openChannels = new AtomicLong();
        AtomicLong closedChannels = new AtomicLong();

        ExecutorService executorService = Executors.newCachedThreadPool();

        server.setConnectionListener(new VSConnectionListener() {
            @Override
            public void connectionAccepted(QuickExecutor executor, VSChannel serverChannel) {
                openChannels.incrementAndGet();

                // This payload will be kept in memory until the channel is closed
                byte[] payload = new byte[PAYLOAD_SIZE];
                payload[0] = 1;

                serverChannel.addDisposableListener(new DisposableListener<>() {
                    @Override
                    public void disposed(VSChannel resource) {
                        byte val = payload[11];
                        if (val != 0) {
                            System.out.println("Should never happen");
                        }
                        // Intentionally do not remove the listener from the channel to release the memory only if channel is released
                    }
                });

                if (enableCloseFix) {
                    executorService.submit(() -> {
                        DataInputStream dis = serverChannel.getDataInputStream();
                        //noinspection TryFinallyCanBeTryWithResources
                        try {
                            while (true) {
                                try {
                                    dis.readByte();
                                } catch (EOFException e) {
                                    // Graceful close
                                    break;
                                } catch (IOException e) {
                                    break;
                                }
                            }
                        } finally {
                            serverChannel.close(); // This will release the memory
                            closedChannels.incrementAndGet();
                        }
                    });
                }
            }
        });
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        try {
            createConnections("localhost", server.getLocalPort());
        } finally {
            System.out.println("Open channels: " + openChannels.get());
            System.out.println("Closed channels: " + closedChannels.get());
        }

        server.close();
        executorService.shutdown();
    }

    public static void createConnections(String host, int port)
            throws IOException, InterruptedException {

        VSClient client = new VSClient(host, port);
        client.connect();

        // This loop fails with OutOfMemoryError
        for (int i = 0; i < ITERATIONS; i++) {
            VSChannel s = client.openChannel();
            s.close(false);

            if (waitForFreeMem && (Runtime.getRuntime().freeMemory() < Runtime.getRuntime().totalMemory() / 5)) {
                // Wait if we below 20% of free memory
                System.gc();
                Thread.sleep(200);
            } else {
                Thread.yield();
            }
        }
        long usedMemory1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.gc();
        Thread.sleep(3000);

        long usedMemory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Used memory before gc: " + usedMemory1);
        System.out.println("Used memory after gc: " + usedMemory2);

        client.close();

        if (usedMemory2 > ITERATIONS * PAYLOAD_SIZE) {
            throw new RuntimeException("Memory leak detected");
        }
    }
}
