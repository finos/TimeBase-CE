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
package com.epam.deltix.util.io.aeron;

import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.TransportProperties;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class AeronClient {
    public static final String CHANNEL = AeronServer.CHANNEL;
    public static final int streamId = AeronServer.streamId;

    private final Aeron aeron;

    public AeronClient(String aeronDir) {

        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(aeronDir);
        this.aeron = Aeron.connect(context);
    }

    private MediaDriver createDriver(String aeronDir) {
        final MediaDriver.Context context = new MediaDriver.Context();

        //* min latency
/*        context.threadingMode(ThreadingMode.DEDICATED)
                .dirsDeleteOnStart(true)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .sharedIdleStrategy(new NoOpIdleStrategy());*/
        //*/

        context.aeronDirectoryName(aeronDir);
        return MediaDriver.launchEmbedded(context);
    }



    private static class DataReceiver implements Runnable {
        private final Subscription subscription;

        private DataReceiver(Aeron aeron) {
            this.subscription = aeron.addSubscription(CHANNEL, streamId);
        }

        @Override
        public void run() {

            BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy(1, 1, 1, 1000);

            //PrintingCounter bytesCounter = new PrintingCounter("Bytes read");

            ReaderState state = new ReaderState();

            FragmentHandler fragmentHandler = (buffer, offset, length, header) -> {
                int pos = offset;
                while (pos < offset + length) {
                    long value = buffer.getLong(pos);
                    if (value != state.counter) {
                        throw new IllegalStateException("Invalid data");
                    }
                    state.counter ++;
                    pos += Long.BYTES;
                }
                state.gotBytes += length;
                long newValue = state.gotBytes;
                long byteDelta = newValue - state.reportedBytes;
                if (byteDelta > 1024 * 1024 * 1024) {
                    long time0 = state.lastReportedTime;
                    long time1 = TimeKeeper.currentTime;
                    state.lastReportedTime = time1;
                    long timeDeltaMs = time1 - time0;
                    System.out.println(byteDelta / (1000 * timeDeltaMs) + " M/s");
                    state.reportedBytes = newValue;
                }
                //bytesCounter.add(length);
            };
            //bytesCounter.start();
            state.lastReportedTime = TimeKeeper.currentTime;
            while (true) {

                final int fragmentsRead = subscription.poll(fragmentHandler, 10);
                if (fragmentsRead > 0) {
                    state.fragmentCounter += fragmentsRead;
                }
                idleStrategy.idle(fragmentsRead);
            }
        }

        private class ReaderState {
            long counter = 0;
            long gotBytes = 0;
            long reportedBytes = 0;
            long lastReportedTime = 0;
            long fragmentCounter = 0;
        }
    }

    public static void main(String[] args) {
        String tempDir = System.getProperty("java.io.tmpdir");

        AeronClient aeronClient = new AeronClient(tempDir);
        new Thread(new DataReceiver(aeronClient.aeron)).start();
        new SigIntBarrier().await();
        aeronClient.aeron.close();
    }
}