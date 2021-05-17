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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.ChannelAccessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.StreamAccessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.TopicAccessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.UdpSingleProducerTopicAccessor;
import com.epam.deltix.util.cmdline.DefaultApplication;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class LatencyBenchmarkManager {
    private static double outputValueUnitScalingRatio = LatencyBenchmark.histogramTimeUnit.convert(1, LatencyBenchmark.outputTimeUnit) * 1.0;

    public static void execute(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, int iterations, List<BenchmarkChannelType> channelTypes, PrintStream out, int targetMessageRatePerSecond, int payloadSize) throws InterruptedException {
        Multimap<BenchmarkChannelType, Histogram> multimap = MultimapBuilder
                .enumKeys(BenchmarkChannelType.class)
                .arrayListValues()
                .build();

        if (iterations < 0) {
            return;
        }

        System.out.println("Running latency benchmark");
        out.format("WarmUp time     : %,4ds\n", warmUpTimeMs / 1000);
        out.format("Measurement time: %,4ds\n", measurementTimeMs / 1000);
        out.format("Iterations: %d\n", iterations);
        out.format("Target message rate (per second): %d\n", targetMessageRatePerSecond);
        out.println();
        for (int i = 0; i < iterations; i++) {
            for (BenchmarkChannelType channelType : channelTypes) {
                try {
                    ChannelAccessor accessor = getAccessorByBenchmarkChannelType(channelType);
                    LatencyBenchmark.Result result = LatencyBenchmark.execute(tickDB, warmUpTimeMs, measurementTimeMs, accessor, targetMessageRatePerSecond, payloadSize);
                    Histogram histogram = result.histogram;
                    multimap.put(channelType, histogram);
                    out.format("Iteration %d of %d for %s\n", i + 1, iterations, channelType.getName());
                    out.println("Running...");
                    out.println("Iteration result:\n");
                    histogram.outputPercentileDistribution(out, 1, 1000.0);
                    out.println();
                    out.format("Actual message rate: %,d (%3.1f%% of requested)\n", result.actualMessageRate, result.actualMessageRate * 100.0 / targetMessageRatePerSecond);
                    out.println();
                } catch (RuntimeException e) {
                    DefaultApplication.printException(e, false);
                }
            }
        }
        out.println("Done");
        out.println();
        synchronized (out) {
            printResults(channelTypes, out, multimap);
        }
    }

    private static void printResults(List<BenchmarkChannelType> channelTypes, PrintStream out, Multimap<BenchmarkChannelType, Histogram> multimap) {

        out.println("# Results for latency benchmark #");

        List<OutputLine> outputLines = ImmutableList.<OutputLine>builder()

                .add(new OutputLine("Min", h -> (double) h.getMinValue()))
                .add(new OutputLine("50%", h -> (double) h.getValueAtPercentile(50)))
                .add(new OutputLine("90%", h -> (double) h.getValueAtPercentile(90)))
                .add(new OutputLine("99%", h -> (double) h.getValueAtPercentile(99)))
                .add(new OutputLine("99.9%", h -> (double) h.getValueAtPercentile(99.9)))
                .add(new OutputLine("99.99%", h -> (double) h.getValueAtPercentile(99.99)))
                .add(new OutputLine("99.999%", h -> (double) h.getValueAtPercentile(99.999)))
                .add(new OutputLine("99.9999%", h -> (double) h.getValueAtPercentile(99.9999)))
                .add(new OutputLine("Max", h -> (double) h.getMaxValue()))
                .add(new OutputLine("Mean", AbstractHistogram::getMean))

                .build();

        out.print(StringUtils.repeat(' ', 11));
        for (BenchmarkChannelType channelType : channelTypes) {
            out.format(" %26s", channelType.getName());
        }
        out.println();

        for (OutputLine outputLine : outputLines) {
            out.format("%8s = ", outputLine.text);
            for (BenchmarkChannelType channelType : channelTypes) {
                Collection<Histogram> results = multimap.get(channelType);
                double avg = avg(results, outputLine.valueGetter) / outputValueUnitScalingRatio;
                out.format(" %16.1f", avg);
            }
            out.println();
        }
        out.println("Table results are in μs (microseconds)");

        out.println();
        out.println();
    }

    private static double avg(Collection<Histogram> results, Function<Histogram, Double> valueGetter) {
        double sum = 0;
        for (Histogram result : results) {
            sum += valueGetter.apply(result);
        }
        return sum / results.size();
    }


    private static ChannelAccessor getAccessorByBenchmarkChannelType(BenchmarkChannelType channelType) {
        switch (channelType) {
            case TOPIC:
                return new TopicAccessor();
            case UDP_SINGLE_PRODUCER_TOPIC:
                return new UdpSingleProducerTopicAccessor();
            case DURABLE_STREAM:
                return new StreamAccessor(StreamScope.DURABLE, ChannelPerformance.LOW_LATENCY);
            case TRANSIENT_STREAM:
                return new StreamAccessor(StreamScope.TRANSIENT, ChannelPerformance.LOW_LATENCY);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static class OutputLine {
        final String text;
        final Function<Histogram, Double> valueGetter;

        private OutputLine(String text, Function<Histogram, Double> valueGetter) {
            this.text = text;
            this.valueGetter = valueGetter;
        }
    }
}
