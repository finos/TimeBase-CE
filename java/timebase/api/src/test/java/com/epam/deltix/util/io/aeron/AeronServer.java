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

import com.epam.deltix.util.vsocket.TransportProperties;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class AeronServer {
    public static final String CHANNEL = CommonContext.IPC_CHANNEL;
    public static final int streamId = 777;

    private final MediaDriver driver;
    private final Aeron aeron;

    public AeronServer(String aeronDir) {

        this.driver = createDriver(aeronDir);

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

    private static class DataSender implements Runnable {

        private final Publication publication;

        private DataSender(Aeron aeron) {
            this.publication = aeron.addPublication(CHANNEL, streamId);
        }

        @Override
        public void run() {
            int bufferSize = Long.BYTES; //1024 * 1024;
            UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bufferSize));


            int longsInBuffer = bufferSize / Long.BYTES;

            BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy(100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
            long counter = 0;
            while (true) {
                // Fill buffer
                for (int i = 0; i < longsInBuffer; i++) {
                    buffer.putLong(i * Long.BYTES, counter + i);
                }
                counter += longsInBuffer;

                idleStrategy.reset();
                // Send buffer
                while (true) {
                    long result = publication.offer(buffer);
                    if (result < 0) {
                        /*if (result == Publication.BACK_PRESSURED) {
                            System.out.println(" Offer failed due to back pressure");
                        } else if (result == Publication.NOT_CONNECTED) {
                            System.out.println(" Offer failed because publisher is not connected to subscriber");
                        } else if (result == Publication.ADMIN_ACTION) {
                            System.out.println("Offer failed because of an administration action in the system");
                        } else if (result == Publication.CLOSED) {
                            System.out.println("Offer failed publication is closed");
                        } else {
                            System.out.println(" Offer failed due to unknown reason");
                        }*/
                        idleStrategy.idle();
                    } else {
                        break;
                    }
                }

            }
        }
    }

    public static void main(String[] args) {

        String tempDir = System.getProperty("java.io.tmpdir");

        AeronServer aeronServer = new AeronServer(tempDir);
        new Thread(new DataSender(aeronServer.aeron)).start();
        //new Thread(new DataReceiver(aeronServer.aeron)).start();
        new SigIntBarrier().await();
        aeronServer.aeron.close();
        aeronServer.driver.close();
    }
}