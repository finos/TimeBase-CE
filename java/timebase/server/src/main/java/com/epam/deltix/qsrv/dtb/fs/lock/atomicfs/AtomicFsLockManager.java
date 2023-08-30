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
package com.epam.deltix.qsrv.dtb.fs.lock.atomicfs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2PathImpl;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Simple implementation of cross-process lock based on atomicy of file system operations.
 * <p>
 * This lock will not work if File System does not guaranties atomicity of operations.
 * <p>
 * WARNING: This lock manager does not support lock refresh and not applicable for operations that is longer than LOCK_TIME_OUT_MS.
 *
 * @author Alexei Osipov
 */
public class AtomicFsLockManager {
    private static final long LOCK_TIME_OUT_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long TIME_JITTER_MS = TimeUnit.SECONDS.toMillis(1);

    private static final Charset CHARSET = StandardCharsets.ISO_8859_1; // Latin-1

    private static final String jvmKey = RandomStringUtils.randomAlphanumeric(32);

    private static final Log    LOGGER = LogFactory.getLog(AtomicFsLockManager.class);

    private AtomicFsLockManager() {
    }

    /**
     * Generates lock file name for specified resource that you want to lock.
     */
    public static AbstractPath getLockName(AbstractPath resourcePath) {
        return resourcePath.getParentPath().append(resourcePath.getName() + ".lock");
    }

    public static FsLock acquire(AbstractPath targetPath) throws InterruptedException {
        try {
            return acquire(targetPath, 0, TimeUnit.SECONDS);
        } catch (TimedOut timedOut) {
            // Should never happen
            throw new AssertionError("Timed out without timeout");
        }
    }

    public static FsLock acquire(AbstractPath targetPath, int timeout, TimeUnit timeUnit) throws TimedOut, InterruptedException {
        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
            LOGGER.debug().append("Getting lock ").append(targetPath.getPathString()).commit();
        }
        targetPath.setCacheMetadata(false);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeUnit.toMillis(timeout);
        IdleStrategy idleStrategy = new SleepingIdleStrategy(TimeUnit.SECONDS.toNanos(1));

        String lockKey = RandomStringUtils.randomAlphanumeric(32);
        AbstractPath tempPath = targetPath.getFileSystem().createPath(targetPath.getParentPath(), targetPath.getName() + "_" + lockKey + ".tmp.lock");
        tempPath.setCacheMetadata(false);

        boolean firstAttempt = true;
        boolean haveToWait = false;
        long currentTime = startTime;
        while (true) {
            if (!firstAttempt) {
                currentTime = System.currentTimeMillis();
            }
            if (timeout != 0 && !firstAttempt && currentTime > endTime) {
                throw new TimedOut();
            }
            if (haveToWait) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                idleStrategy.idle();
            }
            haveToWait = true;
            firstAttempt = false;

            if (exists(targetPath)) {
                if (tryRemoveLock(targetPath)) {
                    // After the deletion attempt (successful or not) we start from fresh
                    haveToWait = false;
                }
                //continue;
            } else {
                // There is no lock yet. Try to create it.

                try {
                    // We might have previously created file
                    if (exists(tempPath)) {
                        tempPath.deleteExisting();
                    }
                } catch (IOException e) {
                    continue;
                }

                try {
                    writeLockData(tempPath, lockKey, currentTime);
                } catch (IOException e) {
                    // We failed to create temp file. Don't try to cleanup now. Leave this to next attempt.
                    continue;
                }
                // At this point we have filled temp file

                try {
                    tempPath.moveTo(targetPath);

                    // Success
                    if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                        LOGGER.debug().append("Got lock ").append(targetPath.getPathString()).commit();
                    }
                    return new FsLock(targetPath, lockKey, currentTime);
                } catch (IOException e) {
                    // Ignore
                }

                if (exists(tempPath)) {
                    // We just failed to get lock (somebody was faster or there was just some exception)
                    // Delete temp file
                    try {
                        tempPath.deleteExisting();
                    } catch (IOException e) {
                        // ignore
                    }
                    continue;
                }

                // There is no more temp file. Probably move was successful but we just got wrong reply.
                if (exists(targetPath)) {
                    // There is a lock. We have to check it it ours.

                    LockFileData lockFileData;
                    try {
                        lockFileData = getLockFileData(targetPath);
                    } catch (IOException e) {
                        continue;
                    }
                    if (lockKey.equals(lockFileData.getLockKey())) {
                        // This is our lock. We were successful (just missed result).
                        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                            LOGGER.debug().append("Got lock ").append(targetPath.getPathString()).commit();
                        }
                        return new FsLock(targetPath, lockKey, currentTime);
                    } else {
                        // TODO: Describe cases that may lead to this result. And handle them if possible.
                        throw new IllegalStateException("Unexpected lock state");
                    }
                } else {
                    // There is no temp file and no lock. Somebody deleted our data.
                    // Option 1: somebody deleted parent path.
                    // Option 2: we are in corrupted state
                    // TODO: Implement better handling
                    throw new IllegalStateException("Unexpected lock state");
                }
            }
        }

    }

    private static boolean tryRemoveLock(AbstractPath path) {
        LockFileData lockFileData;
        try {
            lockFileData = getLockFileData(path);
        } catch (IOException e) {
            // Failed to read file content
            return false;
        }

        if (lockFileData.getJmvKey().equals(jvmKey)) {
            if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                LOGGER.debug().append("Attempting to remove lock that was created by this JVM.").append(path.getPathString()).commit();
            }
        }

        if (System.currentTimeMillis() - lockFileData.getTimestamp() > LOCK_TIME_OUT_MS + TIME_JITTER_MS) {
            // The lock is expired.
            // We can try to delete this file.
            // However we want to avoid race condition scenario when multiple clients concurrently delete same file.
            // So we crate a new lock that wil guard this delete operation.
            // TODO: Adding suffixes may lead to long file names in case of recursive call. It may be better to use counter in name like ".1.unlock", ".2.unlock" etc.
            AbstractPath tempLockPath = path.getFileSystem().createPath(path.getPathString() + ".unlock");
            Optional<FsLock> tempLock;
            try {
                tempLock = tryAcquire(tempLockPath);
            } catch (IOException e) {
                return false;
            }
            if (tempLock.isPresent()) {
                try {
                    if (exists(path)) {
                        path.deleteExisting();
                        return true;
                    } else {
                        // Somebody else deleted the lock
                        return true;
                    }
                } catch (IOException e) {
                    return false;
                } finally {
                    try {
                        release(tempLock.get());
                    } catch (LockExpiredException ignored) {
                        // We can't do much if lock expired here.
                        // In general, this should not happen because we have one operation.
                    }
                }
            } else {
                return false;
            }
        } else {
            // That's lock it not expired yet.
            return false;
        }
    }

    public static Optional<FsLock> tryAcquire(AbstractPath targetPath) throws IOException {
        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
            LOGGER.debug().append("Trying to get lock ").append(targetPath.getPathString()).commit();
        }
        targetPath.setCacheMetadata(false);
        if (exists(targetPath)) {
            // Already acquired by somebody
            if (!tryRemoveLock(targetPath)) {
                return Optional.empty();
            }
        }

        // There is no lock atm


        String lockKey = RandomStringUtils.randomAlphanumeric(32);
        long timestamp = System.currentTimeMillis();

        AbstractPath tempPath = targetPath.getFileSystem().createPath(targetPath.getParentPath(), targetPath.getName() + "_" + lockKey + ".lock");
        tempPath.setCacheMetadata(false);

        writeLockData(tempPath, lockKey, timestamp);
        // At this point we have filled temp file

        try {
            tempPath.moveTo(targetPath);

            // Success
            if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                LOGGER.debug().append("Got lock ").append(targetPath.getPathString()).commit();
            }
            return Optional.of(new FsLock(targetPath, lockKey, timestamp));
        } catch (IOException e) {
            // Ignore
        }

        if (exists(tempPath)) {
            // We just failed to get lock (somebody was faster or there was just some exception)
            // Delete temp file
            tempPath.deleteExisting();
            if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                LOGGER.debug().append("Lock attempt failed ").append(targetPath.getPathString()).commit();
            }
            return Optional.empty();
        }

        // There is no more temp file. Probably move was successful but we just got wrong reply.
        if (exists(targetPath)) {
            // There is a lock. We have to check it it ours.

            LockFileData lockFileData = getLockFileData(targetPath);
            if (lockKey.equals(lockFileData.getLockKey())) {
                // This is our lock. We were successful.
                if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                    LOGGER.debug().append("Got lock ").append(targetPath.getPathString()).commit();
                }
                return Optional.of(new FsLock(targetPath, lockKey, timestamp));
            } else {
                // TODO: Describe cases that may lead to this result. And handle them if possible.
                throw new IllegalStateException("Unexpected lock state");
            }
        } else {
            // There is no temp file and no lock. Somebody deleted our data.
            // Option 1: somebody deleted parent path.
            // Option 2: we are in corrupted state
            // TODO: Implement better handling
            throw new IllegalStateException("Unexpected lock state");
        }
    }

    private static void writeLockData(AbstractPath path, String lockKey, long timestamp) throws IOException {
        try (OutputStream os = path.openOutput(0); Writer w = new OutputStreamWriter(os, CHARSET)) {
            w.append(Long.toString(timestamp));
            w.append('\n');
            w.append(lockKey);
            w.append('\n');
            w.append(jvmKey);
            w.append('\n');
            w.append(getHostName());
            w.flush();
        }
    }

    public static void release(FsLock lock) throws LockExpiredException {
        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
            LOGGER.debug().append("Releasing lock ").append(lock.getTargetPath().getPathString()).commit();
        }

        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            long currentTime = System.currentTimeMillis();
            boolean possiblyExpired = currentTime - lock.getTimestamp() > LOCK_TIME_OUT_MS - TIME_JITTER_MS;

            AbstractPath path = lock.getTargetPath();
            if (!exists(path)) {
                if (possiblyExpired) {
                    // Out lock is expired so somebody else legally deleted it
                    throw new LockExpiredException();
                }
                throw new IllegalStateException("Unexpected lock state");
            }

            // So lock file still exist
            LockFileData lockFileData;
            try {
                lockFileData = getLockFileData(path);
            } catch (IOException e) {
                continue;
            }
            if (lock.getLockKey().equals(lockFileData.getLockKey())) {
                // That's our lock.
                try {
                    // Note: there is a race condition here. TODO: Should we do that in two step fashion?
                    path.deleteExisting();
                    if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                        LOGGER.debug().append("Released lock ").append(lock.getTargetPath().getPathString()).commit();
                    }
                    return;
                } catch (IOException e) {
                    // ignore
                }
            } else {
                // That's somebody else's lock
                if (possiblyExpired) {
                    // Out lock is expired so somebody else legally taken it
                    throw new LockExpiredException();
                } else {
                    throw new LockCorruptionDetected("Somebody claimed our lock before timeout expiration");
                }
            }
        }
        if (LOGGER.isEnabled(LogLevel.DEBUG)) {
            LOGGER.debug().append("Failed to release lock after ").append(attempts).append(" attempts: ").append(lock.getTargetPath().getPathString()).commit();
        }
    }

    public static void releaseSilent(FsLock lock) {
        try {
            release(lock);
        } catch (LockExpiredException ignore) {
        }
    }

    @Nonnull
    private static LockFileData getLockFileData(AbstractPath path) throws IOException {
        try (InputStream inputStream = path.openInput(0)) {
            String fileContent = IOUtils.toString(inputStream, CHARSET);
            return parseLockData(fileContent);
        }
    }

    private static LockFileData parseLockData(String fileContent) {
        String[] parts = fileContent.split("\n");
        if (parts.length < 4) {
            // Incomplete file or wrong format
            throw new IllegalArgumentException();
        } else {
            return new LockFileData(Long.parseLong(parts[0]), parts[1], parts[2], parts[3]);
        }
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    private static boolean exists(AbstractPath path) {
        if (path instanceof Azure2PathImpl) {
            // Azure2PathImpl caches exists status. We have to workaround that.
            return ((Azure2PathImpl) path).existsIgnoreCached();
        } else {
            return path.exists();
        }
    }

    private static class LockFileData {
        private final long timestamp;
        private final String lockKey;
        private final String jmvKey;
        private final String hostname;

        public LockFileData(long timestamp, String lockKey, String jmvKey, String hostname) {
            this.timestamp = timestamp;
            this.lockKey = lockKey;
            this.jmvKey = jmvKey;
            this.hostname = hostname;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getLockKey() {
            return lockKey;
        }

        public String getJmvKey() {
            return jmvKey;
        }

        public String getHostname() {
            return hostname;
        }
    }

    public static class LockExpiredException extends Exception {
    }

    private static class LockCorruptionDetected extends RuntimeException {
        public LockCorruptionDetected(String msg) {
            super(msg);
        }
    }

    public static class TimedOut extends Throwable {
    }
}