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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.EchoMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.testmode.CommunicationType;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.NanoTimeSource;
import org.HdrHistogram.Histogram;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
public abstract class ReadEchoBase {

    protected final ExperimentFormat experimentFormat;

    abstract void work(RemoteTickDB client, BooleanSupplier stopCondition, MessageProcessor messageProcessor);

    private static final TimeUnit histogramTimeUnit = TimeUnit.NANOSECONDS; // Histogram time unit
    private static final TimeUnit outputTimeUnit = TimeUnit.MICROSECONDS; // Output time unit

    private final TimestampGetter nanotimeGetter;
    private final ExperimentIdGetter experimentIdGetter;

    protected ReadEchoBase(ExperimentFormat experimentFormat) {
        this.experimentFormat = experimentFormat;
        if (experimentFormat.getEchoMessageClass().equals(MessageWithNanoTime.class)) {
            this.nanotimeGetter = message1 -> ((MessageWithNanoTime) message1).getPublisherNanoTime();
            this.experimentIdGetter = message -> ((MessageWithNanoTime) message).getExperimentId();
        } else {
            this.nanotimeGetter = message1 -> ((EchoMessage) message1).getOriginalNanoTime();
            this.experimentIdGetter = message -> ((EchoMessage) message).getExperimentId();
        }
    }

    private static void writeStats(Histogram histogram, int loaderMessageRatePerMs, CommunicationType communicationType) {

        String outFileName = "demo_" + getSuffix(communicationType) + "_" + (System.currentTimeMillis()/1000) + "_r" + loaderMessageRatePerMs + "_e" + DemoConf.FRACTION_OF_MARKED + ".log";
        PrintStream printStream;
        try {
            printStream = new PrintStream(new File(outFileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        printStream.println("# FRACTION_OF_MARKED: " + DemoConf.FRACTION_OF_MARKED);
        printStream.println("# Message rate: " + loaderMessageRatePerMs + "k msg/s");
        double outputValueUnitScalingRatio = histogramTimeUnit.convert(1, outputTimeUnit) * 1.0;
        histogram.outputPercentileDistribution(printStream, outputValueUnitScalingRatio);

        synchronized (System.out) { // We want to avoid "output tearing"
            printLnToOutAndFile(printStream, "# Recorded latencies [in " + getTimeUnitName(outputTimeUnit) + "]:");

            histogram.outputPercentileDistribution(System.out, outputValueUnitScalingRatio);

            printLnToOutAndFile(printStream, "# Mean = " + histogram.getMean() / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 50% = " + histogram.getValueAtPercentile(50) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 90% = " + histogram.getValueAtPercentile(90) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 99% = " + histogram.getValueAtPercentile(99) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 99.9% = " + histogram.getValueAtPercentile(99.9) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 99.99% = " + histogram.getValueAtPercentile(99.99) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 99.999% = " + histogram.getValueAtPercentile(99.999) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# 99.9999% = " + histogram.getValueAtPercentile(99.9999) / outputValueUnitScalingRatio);
            printLnToOutAndFile(printStream, "# Max = " + histogram.getMaxValue() / outputValueUnitScalingRatio);

            printStream.close();
            System.out.println("# Written log to file " + outFileName);
        }
    }

    private static String getSuffix(CommunicationType communicationType) {
        switch (communicationType) {
            case TOPIC:
                return "topic";
            case IPC_STREAM:
                return "ipc";
            case SOCKET_STREAM:
                return "socket";
        }
        return null;
    }

    private static void printLnToOutAndFile(PrintStream printStream, String value) {
        printStream.println(value);
        System.out.println(value);
    }

    private static String getTimeUnitName(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return "nanos";
            case MICROSECONDS:
                return "usec"; // μs
            case MILLISECONDS:
                return "ms";
            default:
                return timeUnit.name().toLowerCase();
        }
    }

    public void runEchoReader(RemoteTickDB client, BooleanSupplier stopCondition, int loaderMessageRatePerMs, CommunicationType communicationType, int experimentId) {

        Histogram histogram = new Histogram(3);

        // Create poller
        MessageProcessor processor = new MessageProcessor() {
            @Override
            public void process(InstrumentMessage message) {
                // Process message
                // Note: we can't block in this method.

                long nanoTimeFromMessage = nanotimeGetter.getOriginalNanoTime(message);
                if (nanoTimeFromMessage > 0) {
                    long currentNanos = NanoTimeSource.getNanos();
                    long nanosDiff = currentNanos - nanoTimeFromMessage; // Round trip latency
                    if (nanosDiff < 0) {
                        throw new RuntimeException("ERROR: Negative time");
                    }
                    //long roundTripLatencyMicros = timeUni nanosDiff / 1000;
                    if (nanosDiff > TimeUnit.SECONDS.toNanos(1)) {
                        System.out.println("WARN: >1 SECOND lag! diff=" + nanosDiff + "  currentNanos=" + currentNanos + "  nanoTimeFromMessage=" + nanoTimeFromMessage);
                    }
                    if (experimentIdGetter.getExperimentId(message) == experimentId) {
                        // Record results only for current experiment
                        histogram.recordValue(histogramTimeUnit.convert(nanosDiff, TimeUnit.NANOSECONDS));
                    }
                }
            }
        };

        work(client, stopCondition, processor);

        writeStats(histogram, loaderMessageRatePerMs, communicationType);
    }

    public void stop() {
    }

    public interface TimestampGetter {
        long getOriginalNanoTime(InstrumentMessage message);
    }

    public interface ExperimentIdGetter {
        int getExperimentId(InstrumentMessage message);
    }
}