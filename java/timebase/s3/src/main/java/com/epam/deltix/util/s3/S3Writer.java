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
package com.epam.deltix.util.s3;

import com.amazonaws.util.StringInputStream;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.collections.KeyEntry;
import com.epam.deltix.util.collections.Visitor;
import org.apache.http.client.utils.DateUtils;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.epam.deltix.util.s3.S3DataStore.KEY_DELIMITER;

/**
 * S3Writer - base class for writing data records to S3
 */
public abstract class S3Writer<T> implements Closeable, Flushable {

    public static String METADATA_OBJ_NAME = "METADATA";

    protected static final Log LOG = LogFactory.getLog(S3Writer.class);

    private static final long MS_PER_DAY = TimeUnit.DAYS.toMillis(1);

    private final S3DataStore dataStore;
    private final String dataKey;
    private S3Metadata metadata = null;
    private String userMetadata = null;
    private int maxBatchSize;
    private long maxBatchTime;

    private long batchSize = 0;
    private long batchStartTime = 0;

    private volatile long lastStoredTime = -1;
    private long firstBatchTime = -1;
    private long lastBatchTime = -1;

    private final DateFormat dateFormat;
    private final DateFormat timeFormat;
    private final StringBuilder keyBuilder = new StringBuilder();

    public S3Writer(S3DataStore dataStore, String dataKey, int maxBatchSize, long maxBatchTime) {
        assert dataStore != null && dataKey != null && dataKey.length() > 0 && !dataKey.endsWith(S3DataStore.KEY_DELIMITER);

        this.dataStore = dataStore;
        this.dataKey = dataKey;
        this.maxBatchSize = maxBatchSize;
        this.maxBatchTime = maxBatchTime;

        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        timeFormat = new SimpleDateFormat("HH-mm-ss");
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        LastRecordFinder finder = new LastRecordFinder();
        dataStore.visitObjectKeys(dataKey + KEY_DELIMITER, finder);
        lastStoredTime = finder.lastRecordTime;
    }

    public void setMetadata(S3Metadata metadata) {
        this.metadata = metadata;
    }

    private static boolean sameDay(long timestamp1, long timestamp2) {
        return timestamp1 == timestamp2 || timestamp1/MS_PER_DAY == timestamp2/MS_PER_DAY;
    }

    private boolean isBatchLimitReached() {
        return (maxBatchSize > 0 && batchSize >= maxBatchSize) ||
             (maxBatchTime > 0 && (lastBatchTime - firstBatchTime) >= maxBatchTime);
    }

    /**
     * Writes records to a buffered stream.
     * The batch of records is automatically uploaded to S3 when configured size or time limits are reached
     * @param timestamp Records timestamp in ms
     * @param records Records
     * @return false when duplicate records are ignored
     * @throws IOException in case of remote errors
     */
    public synchronized boolean write(long timestamp, List<T> records) throws IOException {
        // ignore duplicates
        if (timestamp <= lastStoredTime) {
            assert batchSize == 0;
            return false;
        }

        // require records to be grouped at ms boundary
        assert timestamp > lastBatchTime;

        if (batchSize > 0 && !sameDay(timestamp, lastBatchTime)) {
            uploadBatch(); // need to flush since we partition by day
        }

        if (batchSize == 0) {
            batchStartTime = System.currentTimeMillis();
            firstBatchTime = timestamp;
            startBatch();
        }

        for (T record : records)
            writeNextRecord(record);

        lastBatchTime = timestamp;
        batchSize += records.size();

        if (isBatchLimitReached())
            uploadBatch();

        return true;
    }

    protected void startBatch() throws IOException {}

    protected abstract void writeNextRecord(T data) throws IOException;

    protected void finishBatch() throws IOException {}

    protected abstract InputStream getBatchData();

    protected abstract String getDataFormat();

    @Override
    public synchronized void flush() throws IOException {
        if (batchSize > 0) {
            uploadBatch();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        flush();
    }

    // DATA_KEY/date=yyyy-MM-dd/HH-mm-ss_1231231231231.json.gz
    protected String getS3Key() {
        Date batchTime = new Date(lastBatchTime);
        keyBuilder.setLength(0);
        keyBuilder.append(dataKey).append(KEY_DELIMITER);
        keyBuilder.append("date=").append(dateFormat.format(batchTime)).append(KEY_DELIMITER);
        keyBuilder.append(timeFormat.format(batchTime)).append("_").append(lastBatchTime);
        keyBuilder.append(".").append(getDataFormat());
        return keyBuilder.toString();
    }

    protected void uploadBatch() throws IOException {
        String keyName = getS3Key();
        try {
            String md = (metadata != null) ? metadata.getUserMetadata() : null;
            if (md != null && !md.equals(userMetadata)) {
                String mdKey = dataKey + KEY_DELIMITER + METADATA_OBJ_NAME;
                dataStore.upload(mdKey, new StringInputStream(md), null);
                userMetadata = md;
            }
            finishBatch();
            dataStore.upload(keyName, getBatchData(), null);
        }
        catch (Exception ex) {
            String msg = "Batch " + keyName + " upload failed: " + ex.getMessage();
            LOG.error(msg);
            throw new IOException(msg, ex);
        }
        double batchTime = (System.currentTimeMillis() - batchStartTime)/1000.0;
        LOG.info("Uploaded %s records in %s sec (%s records per sec) to %s").with(batchSize).with(batchTime).with((int) (batchSize/batchTime)).with(keyName);

        lastStoredTime = lastBatchTime;

        batchSize = 0;
        firstBatchTime = -1;
        lastBatchTime = -1;
    }

    /**
     * @return NanoTime of the last message uploaded ot S3 or -1
     */
    public long getLastStoredTime() {
        return lastStoredTime;
    }

    public long getFirstBatchTime() {
        return firstBatchTime;
    }

    public long getLastBatchTime() {
        return lastBatchTime;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public long getMaxBatchTime() {
        return maxBatchTime;
    }

    public long getMaxBatchSize() {
        return maxBatchSize;
    }

    private class LastRecordFinder implements Visitor<String> {
        private long lastRecordTime = -1;
        private String extension = "." + getDataFormat();

        public boolean visit(String key) {
            if (key.endsWith(extension)) {
                int timestampEnd = key.length() - extension.length();
                int timestampStart = key.lastIndexOf("_", timestampEnd-1) + 1;
                if (timestampStart > 0) {
                    try {
                        long timestamp = Long.parseLong(key.substring(timestampStart, timestampEnd));
                        if (timestamp > lastRecordTime) {
                            lastRecordTime = timestamp;
                        }
                    } catch (NumberFormatException ex) {
                        LOG.warn("Unexpected S3 object key format: %s").with(key);
                    }
                }
                else {
                    LOG.warn("Unexpected S3 object key format: %s").with(key);
                }
            }
            return true;
        }
    }
}