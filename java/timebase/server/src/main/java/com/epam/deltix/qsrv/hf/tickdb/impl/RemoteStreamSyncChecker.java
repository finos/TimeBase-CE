package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2PathImpl;
import com.epam.deltix.qsrv.dtb.fs.lock.atomicfs.AtomicFsLockManager;
import com.epam.deltix.qsrv.dtb.fs.lock.atomicfs.FsLock;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.hf.tickdb.impl.remotestreams.StreamChangeObserver;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.AffinityThreadFactoryBuilder;
import com.epam.deltix.util.text.SimpleStringCodec;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Checks for stream updates on remote file streams.
 *
 * @author Alexei Osipov
 */
public class RemoteStreamSyncChecker {
    private static final long SYNC_INTERVAL_MS = TimeUnit.SECONDS.toMillis(Integer.getInteger("TimeBase.remoteStreams.syncInterval", 10));

    // After this time change log will expire and disappear
    private static final long CHANGE_LOG_LIVE_TIME = TimeUnit.SECONDS.toMillis(Integer.getInteger("TimeBase.remoteStreams.changeLogExpiration", 5*60));

    private static final Charset CHARSET = StandardCharsets.ISO_8859_1; // Latin-1

    private static final Log LOGGER = LogFactory.getLog(RemoteStreamSyncChecker.class);

    private final AbstractPath changeLogPath;

    @Nullable
    private final AffinityConfig affinityConfig;
    private ScheduledExecutorService executorService;

    private long lastCheckLocalTimestamp = Long.MIN_VALUE;
    private long lastObservedRemoteModificationTimestamp = Long.MIN_VALUE;
    private String changeLogUid = null;
    private long lastProcessedChangeLogLine = 0;

    private State state = State.NOT_STARTED;

    private ScheduledFuture<?> periodicTask;

    private StreamChangeObserver changeObserver;

    private final Object changeLogAccessLock = new Object();

    public RemoteStreamSyncChecker(AbstractPath remoteStreamMetaRootPath, @Nullable AffinityConfig affinityConfig, StreamChangeObserver changeObserver) {
        this.changeLogPath = remoteStreamMetaRootPath.append(".change_log");
        this.changeLogPath.setCacheMetadata(false);
        this.affinityConfig = affinityConfig;
        this.changeObserver = changeObserver;
    }

    public synchronized void start() {
        if (state !=  State.NOT_STARTED) {
            throw new IllegalStateException();
        }

        ThreadFactory threadFactory = new AffinityThreadFactoryBuilder(affinityConfig)
                .setNameFormat("RemoteStreamChecker")
                .build();
        this.executorService = Executors.newScheduledThreadPool(1, threadFactory);
        this.periodicTask = this.executorService.scheduleWithFixedDelay(new CheckerRunnable(), 0, SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);
        this.state = State.STARTED;
    }

    public synchronized void stop(boolean awaitTermination) {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
        executorService.shutdown();
        periodicTask.cancel(true);
        if (awaitTermination) {
            try {
                if (!executorService.awaitTermination(20, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out to stop RemoteStreamChecker");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Was interrupted during attempt to stop RemoteStreamChecker", e);
            }
        }
        state = State.STOPPED;
    }

    /**
     * Checks synchronization log and applies all pending changes (if any).
     *
     * Use this method to ensure that there no pending changes for a stream after you got a lock on it.
     */
    public void syncChangesNow() {
        new CheckerRunnable().run();
    }

    public void writeChangeToLogStreamCreate(String streamKey) {
        writeChangeToLog(StreamOperation.CREATE, streamKey);
    }

    public void writeChangeToLogStreamDelete(String streamKey) {
        writeChangeToLog(StreamOperation.DELETE, streamKey);
    }

    public void writeChangeToLogStreamRename(String oldStreamKey, String newStreamKey) {
        writeChangeToLog(StreamOperation.RENAME, oldStreamKey, newStreamKey);
    }

    public void writeChangeToLogStreamUpdate(String streamKey, EnumSet<TickStreamPropertiesEnum> changes) {
        if (changes.isEmpty()) {
            return;
        }
        String serializedChangeSet = changeSetToString(changes);
        writeChangeToLog(StreamOperation.UPDATE, streamKey, serializedChangeSet);
    }

    private void writeChangeToLog(StreamOperation operation, String... args) {
        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
            LOGGER.debug("Going to write an operation to sync log: %s %s").with(operation.name()).with(Arrays.asList(args));
        }
        int attemptNumber = 0;

        while (attemptNumber < 5) {
            attemptNumber++;
            try {
                synchronized (changeLogAccessLock) {
                    if (tryWriteChangeToLog(operation, args)) {
                        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                            LOGGER.debug("Successfully written operation to sync log: %s %s").with(operation.name()).with(Arrays.asList(args));
                        }
                        return;
                    } else {
                        LOGGER.debug().append("Sync attempt #").append(attemptNumber).append(" failed. Going to retry.").commit();
                    }
                }
            } catch (IOException e) {
                LOGGER.warn().append("IOException during sync").append(e).commit();
            } catch (AtomicFsLockManager.TimedOut e) {
                LOGGER.warn().append("Timed out during attempt to get lock on history file").commit();
            } catch (Exception e) {
                LOGGER.error().append("Unexpected exception during sync").append(e).commit();
            }
        }

        throw new RuntimeException("Failed to perform stream sync");
    }

    private boolean tryWriteChangeToLog(StreamOperation operation, String[] args) throws IOException, InterruptedException, AtomicFsLockManager.TimedOut, AtomicFsLockManager.LockExpiredException {
        synchronized (changeLogAccessLock) {
            AbstractPath lockName = AtomicFsLockManager.getLockName(changeLogPath);
            FsLock fsLock = AtomicFsLockManager.acquire(lockName, 1, TimeUnit.MINUTES);

            // Note: we have lock so we don't need to perform extra checks
            if (!changeLogPath.exists()) {
                createEmptyLog();
            }
            // We must apply missing changes from log before writing own change
            scanChangeLog();
            writeChangeToLogInternal(operation, args);
            lastProcessedChangeLogLine ++; // We don't want to process own line
            setExpirationTime(changeLogPath);
            AtomicFsLockManager.release(fsLock);
            return true;
        }
    }

    private void setExpirationTime(AbstractPath changeLogPath) throws IOException {
        if (changeLogPath instanceof Azure2PathImpl) {
            ((Azure2PathImpl) changeLogPath).setExpireAfter(CHANGE_LOG_LIVE_TIME);
        } else {
            throw new NotImplementedException("Expiration time is not implemented for " + changeLogPath.getClass().getName());
        }
    }

    /**
     * Writes operation to log.
     */
    private void writeChangeToLogInternal(StreamOperation operation, String[] args) throws IOException {
        try (OutputStream out = changeLogPath.openOutputForAppend(); Writer w = new OutputStreamWriter(out, CHARSET)) {
            w.append(Long.toString(System.currentTimeMillis()));
            w.append(' ');
            w.append(getHostName());
            w.append(' ');
            w.append(operation.name());

            for (String arg : args) {
                w.append(' ');
                w.append(SimpleStringCodec.DEFAULT_INSTANCE.encode(arg));
            }

            w.append('\n');
            w.flush();
        }
    }

    private String changeSetToString(EnumSet<TickStreamPropertiesEnum> changes) {
        StringBuilder builder = new StringBuilder();
        for (TickStreamPropertiesEnum change : changes) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(change.getPropertyId());
        }
        return builder.toString();
    }

    private EnumSet<TickStreamPropertiesEnum> stringToChangeSet(String serializedChanges) {
        String[] split = serializedChanges.split(",");
        EnumSet<TickStreamPropertiesEnum> changes = EnumSet.noneOf(TickStreamPropertiesEnum.class);
        for (String val : split) {
            changes.add(TickStreamPropertiesEnum.getValueByPropertyId(Integer.valueOf(val)));
        }
        return changes;
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName().replace(' ', '_');
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }


    private enum State {
        NOT_STARTED,
        STARTED,
        STOPPED
    }



    private void scanChangeLog() throws IOException {
        try (InputStream is = changeLogPath.openInput(0); BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String fileUid = r.readLine();
            if (fileUid == null) {
                throw new IllegalStateException("Empty change log");
            }
            if (!fileUid.equals(changeLogUid)) {
                changeLogUid = fileUid;
            }

            int lineNumber = 0;
            String line;
            while ((line = r.readLine()) != null) {
                lineNumber ++;
                if (lineNumber <= lastProcessedChangeLogLine) {
                    // This line already processed
                    continue;
                }

                ChangeLogRecord record = parseLine(line);
                String operation = record.operation;
                String[] args = record.args;
                if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                    LOGGER.debug().append("Got operation to sync: ").append(operation).append(" ").append(Arrays.asList(args)).commit();
                }

                if (StreamOperation.CREATE.name().equals(operation)) {
                    changeObserver.streamAdded(args[0]);
                } else if (StreamOperation.DELETE.name().equals(operation)) {
                    changeObserver.streamRemoved(args[0]);
                } else if (StreamOperation.RENAME.name().equals(operation)) {
                    changeObserver.streamRenamed(args[0], args[1]);
                } else if (StreamOperation.UPDATE.name().equals(operation)) {
                    EnumSet<TickStreamPropertiesEnum> changeSet = stringToChangeSet(args[1]);
                    changeObserver.streamUpdated(args[0], changeSet);
                } else {
                    LOGGER.error().append("Unknown stream operation: ").append(operation).commit();
                }

                lastProcessedChangeLogLine = lineNumber;
            }
            LOGGER.debug().append("Sync log scan completed").commit();
        } catch (FileNotFoundException e) {
            LOGGER.debug().append("Sync log scan skipped due to missing change log file").commit();
        }
    }



    private boolean tryCheck() throws IOException {
        long checkStartTime = System.currentTimeMillis();

        long modificationTime;
        try {
            modificationTime = changeLogPath.getModificationTime();
        } catch (FileNotFoundException e) {
            // Log file does not exist. This means no recent changes.
            lastCheckLocalTimestamp = checkStartTime;
            return true;
        }

        if (modificationTime > lastObservedRemoteModificationTimestamp) {
            // History file is newer than what we seen before.
            scanChangeLog();
        }
        lastObservedRemoteModificationTimestamp = modificationTime;

        lastCheckLocalTimestamp = checkStartTime;
        return true;
    }

    private void createEmptyLogWithLock() throws IOException {
        AbstractPath lockName = AtomicFsLockManager.getLockName(changeLogPath);
        Optional<FsLock> fsLock = AtomicFsLockManager.tryAcquire(lockName);
        if (fsLock.isPresent()) {
            createEmptyLog();
            try {
                AtomicFsLockManager.release(fsLock.get());
            } catch (AtomicFsLockManager.LockExpiredException e) {
                LOGGER.warn().append("Failed to release lock").append(e).commit();
            }
        }
    }

    private void createEmptyLog() throws IOException {
        String historyFileUid = RandomStringUtils.randomAlphanumeric(32);
        try (OutputStream out = changeLogPath.openOutput(0); Writer w = new OutputStreamWriter(out, CHARSET)) {
            w.append(historyFileUid);
            w.append('\n');
            w.flush();
        }
    }

    private class CheckerRunnable implements Runnable {

        //private final AbstractPath changeLogPath = remoteStreamMetaRootPath.append(".change_signal");

        @Override
        public void run() {
            int attemptNumber = 0;
            try {
                while (attemptNumber < 5) {
                    attemptNumber++;
                    synchronized (changeLogAccessLock) {
                        if (tryCheck()) {
                            LOGGER.debug().append("Executed sync").commit();
                            return;
                        } else {
                            LOGGER.debug().append("Sync attempt #").append(attemptNumber).append(" failed. Going to retry.").commit();
                        }
                    }
                }
                LOGGER.info().append("Failed to perform stream sync").commit();
            } catch (IOException e) {
                LOGGER.warn().append("IOException during sync").append(e).commit();
            } catch (Exception e) {
                LOGGER.error().append("Unexpected exception during sync").append(e).commit();
            }
        }
    }

    private static ChangeLogRecord parseLine(String line) {
        String[] parts = line.split(" ");
        if (parts.length < 4) {
            // Incomplete file or wrong format
            throw new IllegalArgumentException();
        } else {
            String[] args = new String[parts.length - 3];
            for (int i = 0; i < args.length; i++) {
                args[i] = SimpleStringCodec.DEFAULT_INSTANCE.decode(parts[i + 3]);
            }
            return new ChangeLogRecord(Long.parseLong(parts[0]), parts[1], parts[2], args);
        }
    }

    private static StreamOperation getStreamOperationFromName(String part) {
        return StreamOperation.valueOf(part);
    }


    private static class ChangeLogRecord {
        final long timestamp;
        final String hostName;
        final String operation;
        final String[] args;

        public ChangeLogRecord(long timestamp, String hostName, String operation, String[] args) {
            this.timestamp = timestamp;
            this.hostName = hostName;
            this.operation = operation;
            this.args = args;
        }
    }

    private enum StreamOperation {
        CREATE,
        DELETE,
        RENAME,
        UPDATE
    }
}
