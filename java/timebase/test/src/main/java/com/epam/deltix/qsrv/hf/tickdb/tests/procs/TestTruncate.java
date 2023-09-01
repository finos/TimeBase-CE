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
package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.DBWrapper;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestTruncate extends DefaultApplication {

    private static final Log LOG = LogFactory.getLog(TestTruncate.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private String streamKey;

    public TestTruncate(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        long truncatePeriod = getLongArgValue("-truncatePeriod", 2 * 60 * 1000);
        long truncateInterval = getLongArgValue("-truncateInterval", 10 * 60 * 1000);
        int truncateSymbols = getIntArgValue("-symbols", 10);
        String streamKey = getArgValue("-stream", "testStream");
        String dbUrl = getArgValue("-db", "dxtick://localhost:8011");
        try (DBWrapper wrapper = new DBWrapper(dbUrl)) {
            try {
                this.streamKey = streamKey;
                scheduledExecutorService.scheduleAtFixedRate(new TruncateTask(wrapper, truncateInterval, truncateSymbols), truncatePeriod, truncatePeriod, TimeUnit.MILLISECONDS);
                scheduledExecutorService.awaitTermination(10, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class TruncateTask implements Runnable {

        private final DBWrapper wrapper;
        private final long truncateInterval;
        private final int symbols;

        TruncateTask(DBWrapper wrapper, long truncateInterval, int symbols) {
            this.wrapper = wrapper;
            this.truncateInterval = truncateInterval;
            this.symbols = symbols;
        }

        @Override
        public void run() {
            long from = System.currentTimeMillis() - truncateInterval;
            DXTickStream stream = wrapper.getDB().getStream(streamKey);
            IdentityKey[] instrumentIdentities = TestUtils.selectRandomSymbols(stream, symbols);
            LOG.info().append("Truncate from ")
                    .appendTimestamp(from)
                    .append(" identities: ")
                    .append(Arrays.toString(instrumentIdentities))
                    .commit();
            stream.truncate(from, instrumentIdentities);
        }
    }

    public static void main(String[] args) {
        new TestTruncate(args).start();
    }

    public static TestTruncate create(String dbUrl, String stream, long truncatePeriod, long truncateInterval, int symbols) {
        return new TestTruncate(new String[]{
                "-db", dbUrl,
                "-stream", stream,
                "-truncatePeriod", Long.toString(truncatePeriod),
                "-truncateInterval", Long.toString(truncateInterval)
        });
    }
}