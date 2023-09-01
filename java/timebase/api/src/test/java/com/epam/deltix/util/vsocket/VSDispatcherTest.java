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

import com.epam.deltix.util.ContextContainer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
public class VSDispatcherTest {

    /**
     * Test for https://gitlab.deltixhub.com/Deltix/QuantServer/QuantServer/issues/43
     */
    @Test (timeout = 5_000) // Note: test timeout must be greater than reconnectInterval + 2000ms
    public void testNoHangsOnConcurrentDisconnects() throws IOException, InterruptedException {
        int reconnectInterval = 2000;

        VSDispatcher dispatcher = new VSDispatcher("TEST_SERVER", false, new ContextContainer());
        dispatcher.setLingerInterval(reconnectInterval);
        dispatcher.setStateListener(new ConnectionStateListener() {
            @Override
            void onDisconnected() {
            }

            @Override
            void onReconnected() {
            }

            @Override
            boolean onTransportStopped(VSocketRecoveryInfo recoveryInfo) {
                return false;
            }

            @Override
            boolean onTransportBroken(VSocketRecoveryInfo recoveryInfo) {
                return true;
            }
        });

        int transportCount = 2;
        int channelCount = 2;

        for (int i = 0; i < transportCount; i++) {
            MemorySocket receiverSocket = new MemorySocket(i + 1);
            MemorySocket senderSocket = new MemorySocket(receiverSocket, i + 1);
            dispatcher.addTransportChannel(senderSocket);
        }

        ArrayList<VSTransportChannel> transports = new ArrayList<>();

        for (int i = 0; i < transportCount; i++) {
            transports.add(dispatcher.checkOut());
        }

        for (VSTransportChannel transport : transports) {
            dispatcher.checkIn(transport);
        }

        CountDownLatch startBarrier = new CountDownLatch(1);
        List<Thread> errorThreads = new ArrayList<>();
        for (int i = 0; i < transportCount; i++) {
            VSTransportChannel transport = transports.get(i);
            int index = i + 1;
            Thread thread = new Thread(() -> {
                Thread.currentThread().setName("Error thread " + index);

                try {
                    startBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    dispatcher.transportStopped(transport, new Exception("EX" + index));
                } finally {
                    System.out.println("Error thread finished: " + index);
                }

            });
            thread.start();
            errorThreads.add(thread);
        }

        // Put some data into VSOutputStream
        List<VSChannelImpl> channels = new ArrayList<>();
        for (int i = 0; i < channelCount; i++) {
            VSChannelImpl channel = dispatcher.newChannel(1024, 1024, true);
            channel.onRemoteConnected(i, 64*1024, i);
            channels.add(channel);
            byte[] buffer = new byte[1024];
            VSOutputStream out = channel.getOutputStream();
            out.write(buffer, 0, buffer.length);
        }

        assertTrue(dispatcher.hasAvailableTransport());

        System.out.println("Emulating broken transports...");
        startBarrier.countDown();
        Thread.sleep(100); // Let threads get into blocked state

        // Now no transports should be available
        assertFalse(dispatcher.hasAvailableTransport());

        // Emulate Flusher thread
        VSChannelImpl vsChannel = channels.get(0);
        boolean gotChanelClosedException = false;
        try {
            // Note: test may hang here if Dispatcher bug still present
            vsChannel.getOutputStream().flushAvailable();
        } catch (ConnectionAbortedException e) {
            gotChanelClosedException = true;
            System.out.println("Got ConnectionAbortedException as expected");
        }
        Assert.assertTrue(gotChanelClosedException);

        // Waiting for error threads to finish
        for (Thread thread : errorThreads) {
            thread.join();
        }
    }
}