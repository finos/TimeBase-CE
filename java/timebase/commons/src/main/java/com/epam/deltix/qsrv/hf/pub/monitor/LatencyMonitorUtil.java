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
package com.epam.deltix.qsrv.hf.pub.monitor;

import com.epam.deltix.qsrv.hf.server.common.util.SimpleStatistics;
import com.epam.deltix.util.lang.Util;

public final class LatencyMonitorUtil {

    private static final int MOVING_FRAME_SIZE = Util.getIntSystemProperty("ExecutionServer.latencyMonitor.movingFrameSize", 100, 1, Integer.MAX_VALUE);

    public static long adjustLatency(long latency, SimpleStatistics latencyStats) {
        long adjustedLatency = latencyStats.getTotal() > 0 ? (long) (latency - latencyStats.getMovingAvg()) : 0;
        latencyStats.add(latency);
        return adjustedLatency;
    }

    public static SimpleStatistics createStats(LatencyMetric metric) {
        return new SimpleStatistics(MOVING_FRAME_SIZE);
    }

    public static long getSkipSignalCount(LatencyMetric metric) {
        int defaultSkipCount = metric.ordinal() < LatencyMetric.M5.ordinal() ? 1000 : 10;
        return Util.getIntSystemProperty("ExecutionServer.latencyMonitor." + metric.name() + ".warmupSkipCount", defaultSkipCount, 0, Integer.MAX_VALUE);
    }
}