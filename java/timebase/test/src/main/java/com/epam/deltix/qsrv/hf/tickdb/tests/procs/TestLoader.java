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
package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.qsrv.hf.tickdb.tests.ShutdownSignal;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.tests.RandomMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.DBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.MessagesMonitor;
import com.epam.deltix.util.cmdline.DefaultApplication;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

public class TestLoader extends DefaultApplication implements Runnable {

    private static final Log LOG = LogFactory.getLog(TestLoader.class);

    public TestLoader(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        String streamKey = getArgValue("-stream", "testStream");
        String dbUrl = getArgValue("-db", "dxtick://localhost:8011");
        int maxRate = getIntArgValue("-rate", 100000);
        int symbols = getIntArgValue("-symbols", 100);
        LOG.info().append("Starting load task on stream ")
                .append(streamKey)
                .append(" with max rate ").append(maxRate)
                .append(".").commit();
        TickDBFactory.setApplicationName("test random loader");
        LoadTask task = new LoadTask("LoadTask", new RandomMessageSource(symbols), dbUrl, streamKey, maxRate);
        try {
            task.run();
        } finally {
            LOG.info().append("Finished load task.").commit();
        }
    }

    public static class LoadTask implements Runnable {

        private final MessageSource<InstrumentMessage> messageSource;
        private final String key;
        private final DBWrapper wrapper;
        private final RateLimiterRegistry registry;
        private final String id;
        private final ShutdownSignal shutdownSignal = new ShutdownSignal();

        LoadTask(String id, MessageSource<InstrumentMessage> messageSource, String url, String key, int maxRate) {
            this.id = id;
            this.messageSource = messageSource;
            this.key = key;
            this.wrapper = new DBWrapper(url);
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .limitForPeriod(maxRate)
                    .build();
            this.registry = RateLimiterRegistry.of(config);
        }

        @Override
        public void run() {
            MessagesMonitor messagesMonitor = new MessagesMonitor(5000, id);
            try {
                messagesMonitor.start();
                while (!shutdownSignal.isSignaled()) {
                    try {
                        runUnchecked(messagesMonitor);
                    } catch (Exception exc) {
                        LOG.error().append(exc).commit();
                    }
                }
            } finally {
                messagesMonitor.stop();
            }
        }

        private void runUnchecked(MessagesMonitor messagesMonitor) {
            RateLimiter limiter = registry.rateLimiter("limiter");
            DXTickStream stream;
            long start = System.currentTimeMillis();
            do {
                stream = wrapper.getDB().getStream(key);
                if (System.currentTimeMillis() - start > 15000 && stream == null) {
                    throw new RuntimeException("Timeout while waiting for stream creation! Stopping loader task.");
                }
            } while (stream == null);
            try (TickLoader loader = stream.createLoader(new LoadingOptions(false))) {
                LOG.info().append("Opened loader to stream ").append(stream.getKey()).append(", starting loading messages.").commit();
                while (messageSource.next()) {
                    if (limiter.acquirePermission()) {
                        loader.send(messageSource.getMessage());
                        messagesMonitor.count();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new TestLoader(args).start();
    }

    public static TestLoader create(String dbUrl, String stream, int loaders) {
        return new TestLoader(new String[]{
                "-db", dbUrl,
                "-stream", stream,
                "-loaders", Integer.toString(loaders)
        });
    }
}