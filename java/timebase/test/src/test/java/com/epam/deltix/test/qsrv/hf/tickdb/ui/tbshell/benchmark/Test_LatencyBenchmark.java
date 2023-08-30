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
package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.LatencyBenchmark;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.ChannelAccessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.StreamAccessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.TopicAccessor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(Long.class)
public class Test_LatencyBenchmark extends TDBRunnerBase {

    private static final int WARM_UP_TIME_MS = 10_000;
    private static final int MEASUREMENT_TIME_MS = 30_000;

    @Test
    public void testDurable() throws Exception {
        testWithAccessor("Durable stream", new StreamAccessor(StreamScope.DURABLE, ChannelPerformance.LOW_LATENCY));
    }

    @Test
    public void testTransient() throws Exception {
        testWithAccessor("Transient stream", new StreamAccessor(StreamScope.TRANSIENT, ChannelPerformance.LOW_LATENCY));
    }

    @Test
    public void testDurableHighThroughput() throws Exception {
        testWithAccessor("Durable stream HighThroughput", new StreamAccessor(StreamScope.DURABLE, ChannelPerformance.LATENCY_CRITICAL));
    }

    @Test
    public void testTransientHighThroughput() throws Exception {
        testWithAccessor("Transient stream HighThroughput", new StreamAccessor(StreamScope.TRANSIENT, ChannelPerformance.LATENCY_CRITICAL));
    }

    @Test
    public void testTopic() throws Exception {
        testWithAccessor("Topic", new TopicAccessor());
    }


    private void testWithAccessor(String name, ChannelAccessor accessor) throws InterruptedException {
        DXTickDB tickDb = getTickDb();
        int targetMessageRatePerSecond = 1_000_000;
        LatencyBenchmark.Result result = LatencyBenchmark.execute((RemoteTickDB) tickDb, WARM_UP_TIME_MS, MEASUREMENT_TIME_MS, accessor, targetMessageRatePerSecond, 0);
        System.out.println();
        System.out.format("Actual message rate: %,d (%3.1f%% of requested)\n", result.actualMessageRate, result.actualMessageRate * 100.0 / targetMessageRatePerSecond);
        System.out.println();
        result.histogram.outputPercentileDistribution(System.out, 1, 1000.0);
        System.out.println();
        Assert.assertTrue("Measurement count must be positive", result.histogram.getTotalCount() > 0);
    }

    private void printResult(String key, long topicResult) {
        System.out.format(key + " speed: %,d\n", topicResult);
    }
}