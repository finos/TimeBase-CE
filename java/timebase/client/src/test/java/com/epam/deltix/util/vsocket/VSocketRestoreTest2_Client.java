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

import com.epam.deltix.gflog.jul.JulBridge;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/*  ##UTILS## */

/**
 *
 */
public class VSocketRestoreTest2_Client {

    private static final int NUMBER_OF_CLIENTS = 10;
    public static final String SERVER_HOST = "18.218.202.204"; //"localhost"; //
    public static final int SERVER_PORT = 8011;
    private static boolean useKillerThread = true;
    private static boolean printRate = false;

    private AtomicLong[] lastUpdateTs;
    private AtomicBoolean[] finishedClients;
    private AtomicInteger finishedWithError = new AtomicInteger(0);
    private AtomicInteger finishedSuccessfully = new AtomicInteger(0);

    private void clientTask(String host, int port, int packetSize, int numberOfMessages, int clientNumber, CountDownLatch killerStart)
            throws IOException {
        AtomicLong clientTs = lastUpdateTs[clientNumber];
        AtomicBoolean finishedFlag = finishedClients[clientNumber];

        Thread.currentThread().setName("TestClient-" + clientNumber);
        byte[] buffer = new byte[packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer[ii] = (byte) ii;

        final VSClient c = new VSClient(host, port);
        //c.setNumTransportChannels(1);
        c.connect();

        VSChannel s = c.openChannel(VSProtocol.CHANNEL_MAX_BUFFER_SIZE, VSProtocol.CHANNEL_MAX_BUFFER_SIZE, true);
        s.setAutoflush(true);
        OutputStream os = s.getOutputStream();
        AtomicInteger counter = new AtomicInteger(0);
        long lastReportTime = TimeKeeper.currentTime;
        long nextReportTime = lastReportTime + 1000;
        long lastReportedCount = 0;


        Thread socketKiller = new Thread(new SocketKiller(counter, c.getDispatcher(), clientNumber, killerStart, finishedFlag));
        socketKiller.start();

        long index = 0;

        while (index < numberOfMessages) {
            DataExchangeUtils.writeLong(buffer, 0, buffer.length);
            DataExchangeUtils.writeLong(buffer, 8, index++);
            DataExchangeUtils.writeLong(buffer, 16, index++);
            DataExchangeUtils.writeLong(buffer, 24, clientNumber);

            os.write(buffer);

            counter.incrementAndGet();

            long now = TimeKeeper.currentTime;

            if (now > nextReportTime) {
                long num = counter.intValue() - lastReportedCount;
                double sec = (now - lastReportTime) * 0.001;
                double rate = num / sec;

                if (printRate) {
                    System.out.printf(
                            "%,d: %,d m/s; %,d B/s \n",
                            clientNumber,
                            (int) rate,
                            num * packetSize
                    );
                }


                lastReportedCount = counter.intValue();
                lastReportTime = now;
                nextReportTime = now + 1000;
            }

            clientTs.set(now);
        }
    }

    private static class SocketKiller implements Runnable {
        private final CountDownLatch killerStart;
        private final AtomicBoolean clientFinishedFlag;

        private int count = 0;

        private final AtomicInteger counter;
        private final VSDispatcher vssispatcher;
        private int clientNumber;

        private SocketKiller(AtomicInteger counter, VSDispatcher vssispatcher, int clientNumber, CountDownLatch killerStart, AtomicBoolean clientFinishedFlag) {
            this.counter = counter;
            this.vssispatcher = vssispatcher;
            this.clientNumber = clientNumber;
            this.killerStart = killerStart;
            this.clientFinishedFlag = clientFinishedFlag;
        }


        @Override
        public void run() {
            Thread.currentThread().setName("SocketKiller-" + clientNumber);
            Random rnd = new Random(2010);

            if (useKillerThread) {
                try {
                    killerStart.await();
                } catch (InterruptedException e) {
                }
                try {
                    Thread.sleep(rnd.nextInt(10000));
                } catch (InterruptedException e) {
                }
            }

            while (!clientFinishedFlag.get()) {
                try {
                    if (useKillerThread) {
                        int time = rnd.nextInt(1000) + 200;
                        if (count != counter.get()) {
                            // System.out.println("Sleeping time:" + time);
                            Thread.sleep(time);
                            vssispatcher.closeTransport();

                            count = counter.intValue();
                        } else {
                            // Wait for client to advance
                            Thread.sleep(10);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException)
                        break;
                    else
                        e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void test() throws Exception {
        JulBridge.install();
        startClients(67345, 100_000_000, SERVER_HOST, SERVER_PORT);
    }

    private void startClients(final int packetSize, final int numberOfMessages, String serverHost, int serverPort) throws IOException, InterruptedException {
        lastUpdateTs = new AtomicLong[NUMBER_OF_CLIENTS + 1];
        finishedClients = new AtomicBoolean[NUMBER_OF_CLIENTS + 1];

        CountDownLatch killerStart = new CountDownLatch(1);

        List<Thread> clientThreads = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            int clientId = i + 1;
            lastUpdateTs[clientId] = new AtomicLong(0);
            finishedClients[clientId] = new AtomicBoolean(false);
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        clientTask(serverHost, serverPort, packetSize, numberOfMessages, clientId, killerStart);
                        System.err.println("Client " + clientId + " finished successfully");
                        finishedSuccessfully.incrementAndGet();
                    } catch (Throwable e) {
                        finishedWithError.incrementAndGet();
                        synchronized (System.out) {
                            System.err.println("Client " + clientId + " finished with error");
                            e.printStackTrace(System.out);
                        }
                    } finally {
                        finishedClients[clientId].set(true);
                    }
                }
            };
            clientThreads.add(thread);
            thread.start();
            Thread.sleep(1000); // Let client to start and avoid clogging the server
        }

        Thread.sleep(3_000);

        // Start killer threads
        killerStart.countDown();

        //client(serverHost, serverPort, packetSize, numberOfMessages, 0);

        System.out.println("ALL CLIENTS STARTED");
        int activeClients0 = countActiveClients(30);
        System.out.println("Active clients on start: " + activeClients0 + "/" + NUMBER_OF_CLIENTS);
        Thread.sleep(20_000);
        int activeClients1 = countActiveClients(30);
        System.out.println("Active clients after first delay: " + activeClients1 + "/" + NUMBER_OF_CLIENTS);

        while (!Thread.currentThread().isInterrupted()) {
            int activeClients = countActiveClients(30);
            if (activeClients < NUMBER_OF_CLIENTS) {
                printIdle(60);
                printIdleNotFinished(60);
                printFinished();
                System.out.println("Some clients are idle!");

                if (countActiveClients(120) == 0) {
                    System.err.println("ALL CLIENTS ARE IDLE");
                }
            }
            if (activeClients == 0) {
                int liveThreads = 0;
                for (Thread clientThread : clientThreads) {
                    if (clientThread.isAlive()) {
                        liveThreads ++;
                    }
                }
                if (liveThreads == 0) {
                    System.err.println("No more live client threads");
                    break;
                }
            }
            System.err.println("=================> Active clients: " + activeClients + "/" + NUMBER_OF_CLIENTS);
            Thread.sleep(10_000);
        }
        System.out.println("Finished with error: " + finishedWithError.get());
        System.out.println("Finished successfully: " + finishedSuccessfully.get());

        Assert.assertEquals(0, finishedWithError.get());
        Assert.assertTrue(finishedSuccessfully.get() > 0);
    }

    private int countActiveClients(int duration) {
        long now = TimeKeeper.currentTime;
        int activeClients = 0;
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            AtomicLong lastUpdateForClient = lastUpdateTs[i + 1];
            if (lastUpdateForClient.get() >= now - TimeUnit.SECONDS.toMillis(duration)) {
                activeClients++;
            }
        }
        return activeClients;
    }

    private StringBuilder sb = new StringBuilder();

    private void printIdle(int duration) {
        long now = TimeKeeper.currentTime;
        sb.setLength(0);
        sb.append("Idle clients: ");
        boolean first = true;
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            AtomicLong lastUpdateForClient = lastUpdateTs[i + 1];
            if (lastUpdateForClient.get() < now - TimeUnit.SECONDS.toMillis(duration)) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(i+1);
            }
        }
        System.out.println(sb);
    }

    private void printIdleNotFinished(int duration) {
        long now = TimeKeeper.currentTime;
        sb.setLength(0);
        sb.append("Idle not finished clients: ");
        boolean first = true;
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            AtomicLong lastUpdateForClient = lastUpdateTs[i + 1];
            boolean finished = finishedClients[i + 1].get();
            if (lastUpdateForClient.get() < now - TimeUnit.SECONDS.toMillis(duration) && !finished) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(i+1);
            }
        }
        System.out.println(sb);
    }


    private void printFinished() {
        sb.setLength(0);
        sb.append("Finished clients: ");
        boolean first = true;
        for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
            boolean finished = finishedClients[i + 1].get();
            if (finished) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(i+1);
            }
        }
        System.out.println(sb);
    }
}