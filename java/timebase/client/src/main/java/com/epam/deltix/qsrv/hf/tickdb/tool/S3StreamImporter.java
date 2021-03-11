package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.s3.S3DataStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class S3StreamImporter implements Runnable {

    private static final Log LOG = LogFactory.getLog(S3StreamImporter.class);
    private static final long LOG_RATE = 50000;

    private final DXTickStream stream;
    private final S3DataStore dataStore;
    private final String[] spaces;
    private final long startTime;
    private final long endTime;
    private final ImportMode importMode;

    public S3StreamImporter(DXTickStream stream, String[] spaces, S3DataStore dataStore, long startTime, long endTime, ImportMode importMode) {
        this.stream = stream;
        this.spaces = spaces;
        this.dataStore = dataStore;
        this.startTime = startTime;
        this.endTime = endTime;
        this.importMode = importMode;
    }

    @Override
    public void run() {
        try {
            logStart();
            ObjectToObjectHashMap<String, S3RawMessageReader> readers = new ObjectToObjectHashMap<>(spaces.length);
            long firstTimestamp = Long.MAX_VALUE;
            long lastTimestamp = -1;
            boolean oldFormat = false;
            for (String space : spaces) {
                try {
                    S3RawMessageReader reader = new S3RawMessageReader(dataStore, S3Utils.getDataKey(stream, space),
                            startTime, endTime, stream.getTypes());
                    readers.put(space, reader);
                    if (reader.isOldFormat()) {
                        oldFormat = true;
                    } else {
                        if (reader.getSpaceMetadata().startTime < firstTimestamp) {
                            firstTimestamp = reader.getSpaceMetadata().startTime;
                        }
                    }
                    if (reader.getLastTimestamp() > lastTimestamp) {
                        lastTimestamp = reader.getLastTimestamp();
                    }
                } catch (IllegalArgumentException exc) {
                    LOG.error().append(exc).commit();
                }
            }
            if (oldFormat) {
                LOG.info().append("Detected old metadata version.").commit();
                firstTimestamp = S3Utils.getMinTimestamp(dataStore,
                        Arrays.stream(spaces)
                                .map(space -> S3Utils.getDataKey(stream, space))
                                .toArray(String[]::new),
                        startTime, endTime, stream.getTypes());
            }
            prepareStream(firstTimestamp, lastTimestamp);
            ExecutorService executor = Executors.newFixedThreadPool(readers.size());
            for (String space : spaces) {
                S3RawMessageReader reader = readers.get(space, null);
                if (reader != null)
                    executor.execute(new SpaceImporter(reader, space));
            }
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                LOG.info("Replication was interrupted");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void logStart() {
        LOG.info().append("Running import in stream ")
                .append(stream.getKey())
                .append(" with import mode ")
                .append(importMode.name())
                .append('.')
                .commit();
        if (isStartTimeDefined()) {
            LOG.info().append("Start time: ")
                    .appendTimestamp(startTime)
                    .commit();
        }
        if (isEndTimeDefined()) {
            LOG.info().append("End time: ")
                    .appendTimestamp(endTime)
                    .commit();
        }
    }

    private static double getRate(long startTime, long endTime, long count) {
        return 1000. * count / (endTime - startTime);
    }

    private void prepareStream(long firstTimestamp, long lastTimestamp) {
        switch (importMode) {
            case REPLACE:
                prepareWithReplaceMode(firstTimestamp, lastTimestamp);
                break;
            case INSERT:
                prepareWithInsertMode(firstTimestamp, lastTimestamp);
                break;
            case APPEND:
                prepareWithAppendMode(firstTimestamp, lastTimestamp);
                break;
        }
    }

    private void prepareWithReplaceMode(long firstTimestamp, long lastTimestamp) {
        TimeStamp start = TimeStamp.fromMilliseconds(isStartTimeDefined() ? startTime: firstTimestamp);
        TimeStamp end = TimeStamp.fromMilliseconds(isEndTimeDefined() ? endTime: lastTimestamp);
        LOG.info().append("Deleting data from stream ")
                .append(stream.getKey())
                .append(", startTime: ").appendTimestamp(start.getTimeStampMs())
                .append(", endTime: ").appendTimestamp(end.getTimeStampMs())
                .commit();
        stream.delete(start, end);
    }

    private void prepareWithInsertMode(long firstTimestamp, long lastTimestamp) {
        // do nothing
    }

    private void prepareWithAppendMode(long firstTimestamp, long lastTimestamp) {
        // do nothing
    }

    private boolean isStartTimeDefined() {
        return startTime != Long.MIN_VALUE;
    }

    private boolean isEndTimeDefined() {
        return endTime != Long.MAX_VALUE;
    }

    public enum ImportMode {
        REPLACE, INSERT, APPEND
    }

    private class SpaceImporter implements Runnable{

        private final S3RawMessageReader reader;
        private final String spaceId;

        private SpaceImporter(S3RawMessageReader reader, String spaceId) {
            this.reader = reader;
            this.spaceId = spaceId;
        }


        private LoadingOptions getOptions() {
            LoadingOptions options = new LoadingOptions(true);
            options.space = spaceId;
            switch (importMode) {
                case REPLACE:
                case INSERT:
                    options.writeMode = LoadingOptions.WriteMode.INSERT;
                    break;
                case APPEND:
                    options.writeMode = LoadingOptions.WriteMode.APPEND;
                    break;
            }
            return options;
        }

        private TickLoader createLoader() {
            return stream.createLoader(getOptions());
        }

        private void logFinish(long count, long importStartTime) {
            LOG.info().append("Space: ").append(spaceId)
                    .append(". Totally imported: ")
                    .append(count)
                    .append('.')
                    .commit();
            LOG.info().append("Space: ").append(spaceId)
                    .append(". Total rate: ")
                    .append(getRate(importStartTime, System.currentTimeMillis(), count))
                    .append(" msg/sec.")
                    .commit();
        }

        private void logProgress(long localCount, long count, long localStartTime) {
            LOG.info().append("Space: ").append(spaceId)
                    .append(". Imported ")
                    .append(localCount)
                    .append(" messages. Totally: ")
                    .append(count)
                    .append('.')
                    .commit();
            LOG.info().append("Space: ").append(spaceId)
                    .append(". Rate: ")
                    .append(getRate(localStartTime, System.currentTimeMillis(), localCount), 3)
                    .append(" msg/sec.")
                    .commit();
        }

        @Override
        public void run() {
            long count = 0;
            long importStartTime = System.currentTimeMillis();
            RawMessage lastParsed = null;
            try {
                try (TickLoader loader = createLoader()) {
                    long localStartTime = System.currentTimeMillis();
                    long localCount = 0;
                    RawMessage raw = reader.read();

                    while (raw != null) {
                        if (raw.getTimeStampMs() >= startTime && raw.getTimeStampMs() <= endTime) {
                            lastParsed = raw;
                            loader.send(raw);
                            count++;
                            localCount++;
                            if (count % LOG_RATE == 0) {
                                logProgress(localCount, count, localStartTime);
                                localStartTime = System.currentTimeMillis();
                                localCount = 0;
                            }
                        } else if (raw.getTimeStampMs() > endTime) {
                            break;
                        }
                        raw = reader.read();
                    }
                }
            } catch (Exception exc) {
                LOG.error().append("Error while import space ")
                        .append(spaceId)
                        .append(":").append(exc)
                        .commit();
                LOG.error().append("Last imported message in space ")
                        .append(spaceId)
                        .append(":").append(lastParsed)
                        .commit();
            }
            logFinish(count, importStartTime);
        }
    }
}
