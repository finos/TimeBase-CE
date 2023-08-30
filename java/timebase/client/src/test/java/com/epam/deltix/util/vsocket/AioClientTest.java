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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class AioClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        AsynchronousSocketChannel clientChannel = AsynchronousSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
        clientChannel.setOption(StandardSocketOptions.SO_RCVBUF, 16 * 1024 * 1024);
        clientChannel.connect(hostAddress, null, new CompletionHandler<Void, Void>() {
            @Override
            public void completed(Void result, Void attachment) {
                doForConnectedClinet(clientChannel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(countDownLatch::countDown));
        countDownLatch.await();

        clientChannel.close();
    }

    private static void doForConnectedClinet(AsynchronousSocketChannel client) {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024* 128);
        client.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer buf) {
                buf.flip();
                gotCallbackCalls.getAndIncrement();
                int newBytes = result;
                //System.out.println(result);
                gotBytes.addAndGet(newBytes);
                readMessages(buf);
                if (buf.hasRemaining()) {
                    buf.compact();
                } else {
                    buf.clear();
                }
                printStats();
                client.read(readBuffer, buf, this);
            }

            private void readMessages(ByteBuffer buf) {
                int remaining = buf.remaining();
                remaining -= Integer.BYTES;
                while (remaining >= 0) {
                    int size = buf.getInt(buf.position());
                    if (size <= remaining) {
                        assert size == 50 + Long.BYTES;
                        // Has full message
                        long messageNumber = gotMessages.incrementAndGet();
                        gotMessageBytes.addAndGet(+size);
                        buf.position(buf.position() + Integer.BYTES);
                        long messageNumberFromMessage = buf.getLong();
                        if (messageNumber != messageNumberFromMessage) {
                            throw new IllegalStateException("Lost message?");
                        }
                        for (int i = 0; i < 50; i++) {
                            byte b = buf.get();
                            if (b != i) {
                                System.out.println("Corrupted data");
                                throw new IllegalStateException("Corrupted data");
                            }
                        }
                        remaining -= size;
                    } else {
                        return;
                    }

                    remaining -= Integer.BYTES;
                }

            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {

            }
        });
    }

    private static void printStats() {
        long currentCallbackCalls = gotCallbackCalls.get();
        if (currentCallbackCalls % 10000 == 0) {
            long currentTime = System.currentTimeMillis();
            long timeDelta = currentTime - lastReportedTime;
            long msgNow = gotMessages.get();
            long msgDelta = msgNow - lastReportedCount;
            long currentBytes = gotBytes.get();
            long callbackCallsDelta = currentCallbackCalls - lastCallCount;
            long bytesDelta = currentBytes - lastBytes;
            System.out.println("Got " + msgNow + " messages. MMsg/s: "+ (msgDelta / (1000d * timeDelta)) + "  Mb/s: " + (bytesDelta / (1000d * timeDelta) + "  calls/s: " + (callbackCallsDelta * 1000 / (timeDelta))));

            lastReportedTime = currentTime;
            lastReportedCount = msgNow;
            lastBytes = currentBytes;
            lastCallCount = currentCallbackCalls;
        }
    }

    private static final AtomicLong gotMessages = new AtomicLong(0);
    private static final AtomicLong gotMessageBytes = new AtomicLong(0);
    private static final AtomicLong gotBytes = new AtomicLong(0);
    private static final AtomicLong gotCallbackCalls = new AtomicLong(0);
    private static volatile long lastReportedCount = 0;
    private static volatile long lastCallCount = 0;
    private static volatile long lastBytes = 0;
    private static volatile long lastReportedTime = System.currentTimeMillis();
}