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

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class ThroughputBenchmarkManager {
    public static void execute(RemoteTickDB tickDB, long warmUpTimeMs, long measurementTimeMs, int iterations, List<BenchmarkChannelType> channelTypes, PrintStream out, int payloadSize) throws InterruptedException {
        Multimap<BenchmarkChannelType, Long> multimap = MultimapBuilder
                .enumKeys(BenchmarkChannelType.class)
                .arrayListValues()
                .build();

        if (iterations < 0) {
            return;
        }

        System.out.println("Running throughput benchmark");
        out.format("WarmUp time     : %,4ds\n", warmUpTimeMs / 1000);
        out.format("Measurement time: %,4ds\n", measurementTimeMs / 1000);
        out.format("Iterations: %d\n", iterations);

        out.println("Running...");
        for (int i = 0; i < iterations; i++) {
            for (BenchmarkChannelType channelType : channelTypes) {
                try {
                    ChannelAccessor accessor = getAccessorByBenchmarkChannelType(channelType);
                    long result = ThroughputBenchmark.execute(tickDB, warmUpTimeMs, measurementTimeMs, accessor, payloadSize);
                    multimap.put(channelType, result);
                    out.format("Iteration %d for %s: %,12d msg/s\n", i + 1, channelType.getName(), result);
                } catch (RuntimeException e) {
                    DefaultApplication.printException(e, false);
                }
            }
        }
        out.println("Done");
        out.println();
        printResults(channelTypes, out, multimap);
    }

    private static void printResults(List<BenchmarkChannelType> channelTypes, PrintStream out, Multimap<BenchmarkChannelType, Long> multimap) {
        out.println("# Results for live throughput benchmark #");
        for (BenchmarkChannelType channelType : channelTypes) {
            Collection<Long> results = multimap.get(channelType);
            if (results.isEmpty()) {
                out.format(" %26s: No data\n", channelType.getName());
            } else {
                long min = Collections.min(results);
                long avg = avg(results);
                long max = Collections.max(results);
                out.format(" %26s: %,12d msg/s (min) %,12d msg/s (avg) %,12d msg/s (max)\n", channelType.getName(), min, avg, max);
            }
        }
        out.println();
    }

    private static Long avg(Collection<Long> collection) {
        long sum = 0;
        for (Long val : collection) {
            sum += val;
        }
        return sum / collection.size();
    }

    private static ChannelAccessor getAccessorByBenchmarkChannelType(BenchmarkChannelType channelType) {
        switch (channelType) {
            case TOPIC:
                return new TopicAccessor();
            case UDP_SINGLE_PRODUCER_TOPIC:
                return new UdpSingleProducerTopicAccessor();
            case DURABLE_STREAM:
                return new StreamAccessor(StreamScope.DURABLE, ChannelPerformance.MIN_CPU_USAGE);
            case TRANSIENT_STREAM:
                return new StreamAccessor(StreamScope.TRANSIENT, ChannelPerformance.MIN_CPU_USAGE);
            default:
                throw new IllegalArgumentException();
        }
    }
}
