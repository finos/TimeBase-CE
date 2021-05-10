package com.epam.deltix.util.s3;

import com.amazonaws.util.StringUtils;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.lang.Util;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class S3Reader<T> implements Closeable, AutoCloseable {

    protected static final Log LOG = LogFactory.getLog(S3Reader.class);

    private S3DataStore dataStore;
    private List<String> batchKeys;
    private int batchIndex = -1;

    private String userMetadata = null;
    private DataOutputStream out = new DataOutputStream();

    public S3Reader(S3DataStore dataStore, String dataKey, long startTime, long endTime) throws IOException {
        this.dataStore = dataStore;

        // get keys with dataKey prefix sorted by name and filtered by timestamp since it is
        // in this form <stream>/<partition>/<date>_<timestamp>.json.gz
        String keyPrefix = dataKey.endsWith(getDataFormat()) || dataKey.endsWith(S3DataStore.KEY_DELIMITER) ? dataKey : dataKey + S3DataStore.KEY_DELIMITER;
        String keySuffix = "." + getDataFormat();
        this.batchKeys = filterAndSort(dataStore.getObjectKeys(keyPrefix, keySuffix), startTime, endTime);
        Collections.sort(this.batchKeys);
        if (this.batchKeys.size() == 0)
            throw new IllegalArgumentException("No data found under " + keyPrefix);

        String mdKey = keyPrefix + S3Writer.METADATA_OBJ_NAME;
        if (dataStore.objectExists(mdKey)) {
            dataStore.download(mdKey, out);
            userMetadata = new String(out.toByteArray(), StringUtils.UTF8);
        }
        out.reset();
    }

    public S3Reader(S3DataStore dataStore, String dataKey) throws IOException {
        this(dataStore, dataKey, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public String getUserMetadata() {
        return userMetadata;
    }

    public synchronized T read() throws IOException {
        if (batchIndex >= batchKeys.size())
            return null;

        T record = readNextRecord();

        while (record == null) {
            // download the next batch and readNextRecord
            batchIndex++;
            if (batchIndex >= batchKeys.size())
                break;

            out.reset();
            dataStore.download(batchKeys.get(batchIndex), out);
            startBatch(out.getData());

            record = readNextRecord();
        }
        return record;
    }

    /**
     * Filters object keys according to time interval
     *
     * @param keys      list of keys in format DATA_KEY/date=yyyy-MM-dd/HH-mm-ss_1231231231231.json.gz
     * @param startTime start time
     * @param endTime   end time
     * @return filtered values list
     */
    private List<String> filterAndSort(List<String> keys, long startTime, long endTime) {
        final ObjectArrayList<Pair<Long, String>> list = keys.stream()
                .map(s -> Pair.of(extractTimestamp(s), s))
                .filter(p -> p.getLeft() >= startTime)
                .sorted(Comparator.comparingLong(Pair::getLeft))
                .collect(Collectors.toCollection(ObjectArrayList::new));
        final ObjectArrayList<String> result = new ObjectArrayList<>();
        if (list.size() > 0)
            result.add(list.get(0).getRight());
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i - 1).getLeft() < endTime)
                result.add(list.get(i).getRight());
        }
        return result;
    }

    public long getLastTimestamp() {
        return extractTimestamp(batchKeys.get(batchKeys.size() - 1));
    }

    /**
     * Extracts timestamp from key
     * @param key string in format DATA_KEY/date=yyyy-MM-dd/HH-mm-ss_1231231231231.json.gz
     * @return extracted timestamp
     */
    protected long extractTimestamp(String key) {
        return Long.parseLong(key.substring(key.lastIndexOf('_') + 1, key.lastIndexOf("." + getDataFormat())));
    }

    protected abstract void startBatch(InputStream batchData) throws IOException;

    protected abstract T readNextRecord() throws IOException;

    protected abstract String getDataFormat();

    protected static class DataOutputStream extends ByteArrayOutputStream {
        protected InputStream getData() {
            return new ByteArrayInputStream(super.buf, 0, size());
        }
    }

    @Override
    public void close() throws IOException {
        Util.close(out);
    }
}
