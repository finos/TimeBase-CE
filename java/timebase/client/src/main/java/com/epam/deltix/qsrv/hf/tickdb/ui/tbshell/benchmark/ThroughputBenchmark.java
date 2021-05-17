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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.ChannelAccessor;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.io.aeron.PublicationClosedException;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class ThroughputBenchmark extends ChannelBenchmarkBase {
    public static long execute(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, ChannelAccessor accessor, int payloadSize) throws InterruptedException {
        String channelKey = createChannel(accessor, tickDB);
        try {
            return executeOnChannel(tickDB, warmUpTimeMs, measurementTimeMs, accessor, channelKey, payloadSize);
        } finally {
            accessor.deleteChannel(tickDB, channelKey);
        }
    }

    private static long executeOnChannel(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, ChannelAccessor accessor, String channelKey, int payloadSize) throws InterruptedException {
        AtomicBoolean loaderStopSignal = new AtomicBoolean(false);

        MessageChannel<InstrumentMessage> loader = accessor.createLoader(tickDB, channelKey);
        Thread loaderThread = new Thread(() -> {
            InstrumentMessage instrumentMessage = createInstrumentMessage(payloadSize);
            try {
                while (!loaderStopSignal.get()) {
                    instrumentMessage.setTimeStampMs(System.currentTimeMillis());
                    loader.send(instrumentMessage);
                }
                loader.close();
            } catch (PublicationClosedException ignored) {
            }
        });
        loaderThread.setName("PRODUCER");
        //System.out.println("PRODUCER-" + accessor.getClass().getSimpleName());
        //System.out.println("Loader: " + loader.getClass().getSimpleName());
        loaderThread.start();


        AtomicBoolean readerStopSignal = new AtomicBoolean(false);
        AtomicLong messageCounter = new AtomicLong(0);
        MessageSource<InstrumentMessage> source = accessor.createConsumer(tickDB, channelKey);
        Thread readerThread = new Thread(() -> {
            try {
                while (!readerStopSignal.get()) {
                    if (source.next()) {
                        InstrumentMessage msg = source.getMessage();
                        if (msg.getTimeStampMs() < 0) {
                            throw new RuntimeException("Unexpected timestamp value");
                        }
                        messageCounter.incrementAndGet();
                    }
                }
                source.close();
            } catch (CursorIsClosedException ignored) {
            }
        });
        readerThread.setName("CONSUMER");
        readerThread.start();

        // WarmUp
        Thread.sleep(warmUpTimeMs);

        // Perform measurement
        long startNanos = System.nanoTime();
        // Reset message counter
        messageCounter.set(0);
        Thread.sleep(measurementTimeMs);
        long totalMessages = messageCounter.get();
        //System.out.println("TOTAL: " + totalMessages);
        long stopNanos = System.nanoTime();
        long messageRate = totalMessages * TimeUnit.SECONDS.toNanos(1) / (stopNanos - startNanos);

        // Stop threads
        // We stop readers first because it's easier to avoid deadlock for them
        readerStopSignal.set(true);
        loaderStopSignal.set(true);
        readerThread.join(100);
        loaderThread.join(100);
        source.close();
        loader.close();
        readerThread.join(100);
        loaderThread.join(100);
        if (readerThread.isAlive()) {
            readerThread.interrupt();
            readerThread.join(100);
            if (readerThread.isAlive()) {
                throw new RuntimeException("Failed to stop reader thread");
            }
        }
        if (loaderThread.isAlive()) {
            loaderThread.interrupt();
            loaderThread.join(100);
            if (loaderThread.isAlive()) {
                throw new RuntimeException("Failed to stop loader thread");
            }
        }
        return messageRate;
    }
}
