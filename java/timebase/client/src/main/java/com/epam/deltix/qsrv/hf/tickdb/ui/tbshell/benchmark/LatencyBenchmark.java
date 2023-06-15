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
import org.HdrHistogram.Histogram;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class LatencyBenchmark extends ChannelBenchmarkBase {
    static TimeUnit histogramTimeUnit = TimeUnit.NANOSECONDS; // Histogram time unit
    static TimeUnit outputTimeUnit = TimeUnit.MICROSECONDS; // Output time unit

    public static Result execute(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, ChannelAccessor accessor, int targetMessageRatePerSecond, int payloadSize) throws InterruptedException {
        String channelKey = createChannel(accessor, tickDB);
        try {
            return executeOnChannel(tickDB, warmUpTimeMs, measurementTimeMs, accessor, channelKey, targetMessageRatePerSecond, payloadSize);
        } finally {
            accessor.deleteChannel(tickDB, channelKey);
        }
    }

    private static Result executeOnChannel(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, ChannelAccessor accessor, String channelKey, int targetMessageRatePerSecond, int payloadSize) throws InterruptedException {
        AtomicBoolean loaderStopSignal = new AtomicBoolean(false);

        MessageChannel<InstrumentMessage> loader = accessor.createLoader(tickDB, channelKey);
        Thread loaderThread = new Thread(() -> {
            InstrumentMessage instrumentMessage = createInstrumentMessage(payloadSize);
            long messageCount = 0;
            try {
                long startNanoTime = System.nanoTime();
                long nanosInSecond = TimeUnit.SECONDS.toNanos(1);
                while (!loaderStopSignal.get()) {
                    long currentNanoTime = System.nanoTime();
                    long targetMessageCount = (currentNanoTime - startNanoTime) * targetMessageRatePerSecond / nanosInSecond;
                    if (messageCount < targetMessageCount) {
                        instrumentMessage.setNanoTime(currentNanoTime);
                        loader.send(instrumentMessage);
                        messageCount++;
                    } else {
                        Thread.yield();
                    }
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
        //AtomicLong messageCounter = new AtomicLong(0);
        MessageSource<InstrumentMessage> source = accessor.createConsumer(tickDB, channelKey);
        AtomicReference<Histogram> histogramRef = new AtomicReference<>(createHistogram());
        Thread readerThread = new Thread(() -> {
            try {
                while (!readerStopSignal.get()) {
                    if (source.next()) {
                        InstrumentMessage msg = source.getMessage();
                        if (msg.getTimeStampMs() < 0) {
                            throw new RuntimeException("Unexpected timestamp value");
                        }
                        long currentNanos = System.nanoTime();
                        long nanosDiff = currentNanos - msg.getNanoTime();
                        histogramRef.get().recordValue(histogramTimeUnit.convert(nanosDiff, TimeUnit.NANOSECONDS));
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
        // Start real measurement
        histogramRef.set(createHistogram());
        Thread.sleep(measurementTimeMs);
        // Get the result and replace old object to avoid counting after finish
        Histogram histogram = histogramRef.getAndSet(createHistogram());

        histogram.setEndTimeStamp(System.currentTimeMillis());
        //System.out.println("TOTAL: " + totalMessages);
        long stopNanos = System.nanoTime();
        long messageRate = histogram.getTotalCount() * TimeUnit.SECONDS.toNanos(1) / (stopNanos - startNanos);

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
        return new Result(histogram, messageRate);
    }

    @NotNull
    private static Histogram createHistogram() {
        return new Histogram(3);
    }

    public static class Result {
        public final Histogram histogram;
        public final long actualMessageRate;

        public Result(Histogram histogram, long actualMessageRate) {
            this.histogram = histogram;
            this.actualMessageRate = actualMessageRate;
        }
    }
}