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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.s3.S3DataStore;
import com.epam.deltix.util.s3.S3Writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class S3StreamReplicator implements Runnable {
    private static final Log LOG = LogFactory.getLog(S3StreamReplicator.class);

    // Safe period away from live messages when reading from TB
    private static final long OFFSET_INTERVAL = TimeUnit.SECONDS.toMillis(10);

    // Minimum batch of messages (in time) that we want to process in single iteration of replicator.
    private static final long ACCUMULATION_INTERVAL  = TimeUnit.MINUTES.toMillis(3);

    // Interval with which data is purged from timebase when retention period is specified
    private static final long PURGE_INTERVAL = TimeUnit.DAYS.toMillis(1);

    private DXTickStream msgStream;
    private S3DataStore dataStore;
    private boolean spacesSupport = true; // stream supports spaces

    private boolean live;
    private long retentionPeriod;
    private long purgeDelay = 0;
    private long monitorInterval;

    private Map<String,SpaceReplicator> replicators = new HashMap<>();
    private List<Thread> threads = new ArrayList<>();
    private Exception error = null;

    public S3StreamReplicator(DXTickStream msgStream, S3DataStore dataStore, boolean live, int retentionPeriod, int purgeOffset, long monitorInterval) {
        assert monitorInterval >= 1000;

        this.msgStream = msgStream;
        this.dataStore = dataStore;
        this.live = live;
        this.retentionPeriod = (retentionPeriod >= 0) ? TimeUnit.DAYS.toMillis(retentionPeriod) : -1;
        this.purgeDelay = PURGE_INTERVAL + TimeUnit.DAYS.toMillis(purgeOffset);
        this.monitorInterval = monitorInterval;

        String[] spaceIds = msgStream.listSpaces();
        if (spaceIds == null || spaceIds.length == 0) {
            LOG.warn("\"%s\" stream does not support spaces").with(msgStream.getKey());
            spacesSupport = false;
            spaceIds = new String[]{ null };
        }

        for (int i = 0; i < spaceIds.length; i++) {
            replicators.put(spaceIds[i], new SpaceReplicator(spaceIds[i]));
        }
    }

    private void startReplicator(SpaceReplicator replicator) {
        synchronized (threads) {
            LOG.info("Starting replicator for " + replicator.dataKey);
            Thread t = new Thread(replicator);
            threads.add(t);
            t.start();
        }
    }

    private Thread getReplicatorThread(int i) {
        synchronized (threads) {
            return i < threads.size() ? threads.get(i) : null;
        }
    }

    public void run() {
        for (SpaceReplicator replicator : replicators.values()) {
            startReplicator(replicator);
        }

        ScheduledExecutorService tasks = Executors.newScheduledThreadPool(2);
        if (live && retentionPeriod >= 0)
            tasks.scheduleWithFixedDelay(this::purgeOldData, purgeDelay, PURGE_INTERVAL, TimeUnit.MILLISECONDS);
        if (spacesSupport)
            tasks.scheduleWithFixedDelay(this::monitorSpaces, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);

        try {
            Thread replicatorThread;
            for (int i = 0; (replicatorThread = getReplicatorThread(i)) != null; i++) {
                replicatorThread.join();
            }
        } catch (InterruptedException ex) {
            LOG.info(msgStream.getKey() + " replication was interrupted");
            throw new RuntimeException(msgStream.getKey() + " replication was interrupted");
        }

        tasks.shutdown();
        purgeOldData(); // we get here in non-live mode only
    }

    private void purgeOldData() {
        for (SpaceReplicator replicator : replicators.values()) {
            replicator.purgeOldData(retentionPeriod);
        }
    }

    private void monitorSpaces() {
        String[] spaceIds = msgStream.listSpaces();
        if (spaceIds != null && spaceIds.length > replicators.size()) {
            for (String spaceId : spaceIds) {
                if (!replicators.containsKey(spaceId)) {
                    SpaceReplicator replicator = new SpaceReplicator(spaceId);
                    startReplicator(replicator);
                    replicators.put(spaceId, replicator);
                }
            }
        }
    }

    private void setError(Exception ex) {
        this.error = ex;
    }

    private class SpaceReplicator implements Runnable {
        private final String spaceId;
        private final String dataKey;
        private final S3Writer<String> writer;

        private final JSONRawMessagePrinter msgPrinter = new JSONRawMessagePrinter();
        private final StringBuilder msgBuilder = new StringBuilder();

        private long firstTimestamp = -1;

        private long deadline;

        public SpaceReplicator(String spaceId) {
            this.spaceId = spaceId;
            this.dataKey = S3Utils.getDataKey(msgStream, spaceId);
            this.writer = dataStore.createWriter(this.dataKey);
            this.writer.setMetadata(this::getUserMetadata);
        }

        private String getUserMetadata() {
           return spaceId == null ? null: S3Utils.INSTANCE.serializeMetadata(msgStream, spaceId, firstTimestamp);
        }

        public long getLastStoredTime() {
            return this.writer.getLastStoredTime();
        }

        private void replicate() throws IOException {
            long lastMessageTimestamp = writer.getLastStoredTime();
            SelectionOptions options = new SelectionOptions(true, false);
            options.withSpaces(spaceId);

            try (TickCursor cursor = msgStream.select(lastMessageTimestamp + 1, options)) {
                List<String> records = new ArrayList<>();
                RawMessage msg = null;
                long count = 0;
                updateDeadline();

                while (error == null) {
                    msg = nextMessage(cursor, lastMessageTimestamp);

                    // buffer records at ms boundary
                    if ((msg == null || lastMessageTimestamp != msg.getTimeStampMs()) && records.size() > 0) {
                        if (writer.write(lastMessageTimestamp, records))
                            count += records.size();
                        records.clear();
                    }

                    if (msg != null) {
                        if (firstTimestamp == -1) {
                            firstTimestamp = msg.getTimeStampMs();
                            writer.setMetadata(this::getUserMetadata);
                        }
                        msgBuilder.setLength(0);
                        msgPrinter.append(msg, msgBuilder);
                        records.add(msgBuilder.toString());
                        lastMessageTimestamp = msg.getTimeStampMs();
                    }
                    else if (!live) {
                        // null msg in batched mode means we are done
                        writer.flush();
                        if (count > 0)
                            LOG.info("Successfully uploaded %s \"%s\" messages to S3").with(count).with(dataKey);
                        else
                            LOG.info("No new \"%s\" messages").with(dataKey);
                        break;
                    }
                }

                if (error != null) {
                    LOG.warn("Replication of \"" + dataKey + "\" is interrupted due to error: " + error.getMessage());
                    writer.flush();
                }
            }
        }

        private void updateDeadline() {
            this.deadline = System.currentTimeMillis() - OFFSET_INTERVAL;
        }

        /**
         * Will return null only for non-live replication
         */
        private RawMessage nextMessage(TickCursor cursor, long lastMessageTimestamp) throws IOException {
            RawMessage message = cursor.next() ? (RawMessage) cursor.getMessage() : null;

            while (message == null || message.getTimeStampMs() > deadline) {
                if (live) {
                    message = waitForBatchedMessage(cursor, lastMessageTimestamp);
                } else {
                    return null;
                }
            }

            return message;
        }

        private RawMessage waitForBatchedMessage(TickCursor cursor, long lastTimestamp) throws IOException {
            assert live;

            // check if it is time to flush before waiting for more messages again
            if (writer.getBatchSize() > 0 && writer.getMaxBatchTime() > 0 && (deadline - writer.getFirstBatchTime() >= writer.getMaxBatchTime())) {
                writer.flush();
            }

            long nextDeadline = deadline + ACCUMULATION_INTERVAL;
            long timeToWait = nextDeadline - System.currentTimeMillis();
            if (timeToWait > 0) {
                LOG.info("Waiting for more \"%s\" (spaceId=%s) messages. Wait time: %s ms").with(dataKey).with(spaceId).with(timeToWait);
                try {
                    Thread.sleep(timeToWait);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); // Restore interruption flag
                    throw new UncheckedInterruptedException(ex);
                }
            } else {
                LOG.info("Replication is late for \"%s\" (spaceId=%s) by %s ms").with(dataKey).with(spaceId).with(-timeToWait);
            }
            updateDeadline();

            cursor.reset(lastTimestamp+1);
            return cursor.next() ? (RawMessage) cursor.getMessage() : null;
        }

        protected void purgeOldData(long retentionPeriod) {
            if (retentionPeriod < 0) return;

            long purgeTime = getLastStoredTime() - retentionPeriod;
            if (purgeTime > 0) {
                try {
                    LOG.info("Purging %s stream space %s at %s").with(msgStream.getKey()).with(spaceId).with(purgeTime);
                    if (spaceId != null)
                        msgStream.purge(purgeTime, spaceId);
                    else
                        msgStream.purge(purgeTime);
                } catch (Exception ex) {
                    LOG.error("Failed to purge %s stream space %s at %s. Error: %s").with(msgStream.getKey()).with(spaceId).with(purgeTime).with(ex);
                }
            }
        }

        @Override
        public void run() {
            try {
                replicate();
            } catch (Exception ex) {
                LOG.error().append("Failed to upload records from \"").append(msgStream.getKey()).append("\" stream to " + dataKey + " in S3: ").append(ex).commit();
                setError(ex);
            }
        }
    }
}

