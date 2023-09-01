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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.dataacc.DataReaderImpl;
import com.epam.deltix.qsrv.dtb.store.dataacc.DataWriterImpl;
import com.epam.deltix.qsrv.dtb.store.dataacc.LiveDataReaderImpl;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.runtime.Shutdown;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class PDSImpl implements PersistentDataStore {
    static final Log LOGGER = LogFactory.getLog("deltix.dtb");
//    static {
//        LOGGER.setLevel(LogLevel.DEBUG);
//    }

//    private final MetricsService metrics = MetricsService.getInstance();

    // After thant number of failures across any number of files we trigger Emergency shutdown.
    private static final int FAILURES_TO_TRIGGER_SHUTDOWN = Integer.getInteger("TimeBase.storage.failuresToShutdown", 5);

    // Additional attempts to save a file. Value 2 means that there may be up to 3 failed attempts in total per files.
    // If all attempts failed then Emergency shutdown will be triggered.
    private static final int ATTEMPTS_PER_FILE = Integer.getInteger("TimeBase.storage.attemptsPerFile", 2);

    // Number of Writer Threads
    private static final int NUMBER_OF_WRITERS = Integer.getInteger("TimeBase.storage.writers", 2);
    private static final int ALLOCATED_MEMORY_RATIO = Integer.getInteger("TimeBase.storage.memory.allocationRatio", 30); // 30% of max memory

//    // limit of minimum available memory (500MB default)
//    private static final long MEMORY_LIMIT = Long.getLong("TimeBase.storage.memoryLimit", 1024L * 1024 * 500);

    final Runtime RUNTIME = Runtime.getRuntime();

    private long maxFileSize;

    private boolean                         isStarted = false;
    private boolean                         shutdownInProgress = false;
    private boolean                         isReadOnly = false;
    //private final ObjectArrayList <TSFile>  dirtyFiles = new ObjectArrayList <> ();
    private int                             numWriters = NUMBER_OF_WRITERS;
    private final List<TSFWriterThread>     writers = new ObjectArrayList<>(numWriters);
    private final WriteQueue                queue = new WriteQueue(numWriters);

    private final Object                    cleanLock = new Object ();
    private int                             numDirtyFiles = 0;
    private final QuickExecutor             executor;
    private final QuickExecutor             localExecutor;

    // Key: TSFile.getPathString(), Value: number of failures
    // We do not keep TSFile itself because it will mean preserving a permanent reference to a file object.
    // Instead of this we just keep path to file.
    private final Map<String, AtomicInteger> failuresByFile = new HashMap<>();

    private EmergencyShutdownControl shutdownControl = null;
    private final AtomicInteger failedWriteAttempts = new AtomicInteger(0);

    //private ByteArrayHeap                   allocator;

//    private final AtomicInteger dirtyFilesGauge = metrics.registerGauge(
//        "timebase.storage.num_dirty_files", new AtomicInteger(numDirtyFiles)
//    );
//    private final AtomicInteger activeFilesGauge = metrics.registerGauge(
//        "timebase.storage.num_active_files", new AtomicInteger()
//    );
//    private final AtomicLong ioReadsCount = metrics.registerGauge(
//        "timebase.storage.io.read_count", new AtomicLong()
//    );
//    private final AtomicLong ioWritesCount = metrics.registerGauge(
//        "timebase.storage.io.write_count", new AtomicLong()
//    );

    PDSImpl (QuickExecutor exe) {
        this.executor = exe;
        this.localExecutor = null;
    }

    PDSImpl () {
        this.executor = null;
        this.localExecutor = QuickExecutor.createNewInstance("PDSImpl Executor", null);
        this.localExecutor.reuseInstance();
    }

    //
    //  PersistentDataStore IMPLEMENTATION
    //

    @Override
    public synchronized boolean     isStarted () {
        return (isStarted);
    }

    @Override
    public synchronized void        setReadOnly (boolean readOnly) {
        if (isStarted)
            throw new IllegalStateException ("Already started");

        isReadOnly = readOnly;
    }

    @Override
    public synchronized boolean     isReadOnly () {
        return (isReadOnly);
    }

    @Override
    public synchronized void        setNumWriterThreads (int n) {
        numWriters = n;
    }

    @Override
    public synchronized void        start () {
        if (isStarted)
            throw new IllegalStateException ("Already started");

        for (int ii = 0; ii < numWriters; ii++) {
            TSFWriterThread     wt = new TSFWriterThread (this, ii);

            wt.start ();

            writers.add(wt);
        }

        isStarted = true;
    }

    public synchronized void        startShutdown() {
        shutdownInProgress = true;
    }

    @Override
    public synchronized void        shutdown () {
        isStarted = false;

        for (TSFWriterThread wt : writers)
            wt.interrupt ();

        shutdownInProgress = false;

        if (localExecutor != null)
            localExecutor.shutdownInstance();

        //allocator = null;
    }

    @Override
    public boolean          waitUntilDataStored (int timeout) {

        try {
            assert isStarted();

            long limit =
                    timeout <= 0 ?
                            Long.MAX_VALUE :
                            System.currentTimeMillis() + timeout;

            synchronized (cleanLock) {
                for (;;) {
                    assert numDirtyFiles >= 0 : "should be >=0 " + numDirtyFiles;

                    if (numDirtyFiles <= 0)
                        return (true);

                    long ttw = limit - System.currentTimeMillis();

                    if (ttw <= 0)
                        return (false);

                    try {
                        cleanLock.wait(ttw);
                    } catch (InterruptedException x) {
                        throw new UncheckedInterruptedException(x);
                    }
                }
            }
        } finally {
            assert queue.getUsedMemory() == 0 : "Remains: " + queue.getUsedMemory();
        }
    }

    void             checkWriteQueueLimit (long maxFileSize) {
        boolean logged = false;

        long usedMemory = queue.getUsedMemory();
        if (usedMemory < RUNTIME.maxMemory() * ALLOCATED_MEMORY_RATIO / 100.0)
            return;

        synchronized (cleanLock) {
            for (;;) {

                if (numDirtyFiles == 0)
                    return;

                usedMemory = queue.getUsedMemory();
                if (usedMemory < RUNTIME.maxMemory() * ALLOCATED_MEMORY_RATIO / 100.0)
                    return;

                try {
                    if (!logged) {
                        long freeMemory = RUNTIME.maxMemory() - RUNTIME.totalMemory() + RUNTIME.freeMemory();
                        LOGGER.warn("Low available memory: %s bytes. Write Queue size = %s. Waiting ... ").with(freeMemory).with(numDirtyFiles);
                        logged = true;
                    }

                    cleanLock.wait (5000);
                } catch (InterruptedException x) {
                    throw new UncheckedInterruptedException (x);
                }
            }
        }
    }


    @Override
    public synchronized boolean        waitForShutdown (int timeout) {
        long            limit =
                timeout <= 0 ?
                        Long.MAX_VALUE :
                        System.currentTimeMillis () + timeout;

        for (TSFWriterThread wt : writers) {
            while (wt.isAlive ()) {
                long    ttw = limit - System.currentTimeMillis ();

                if (ttw <= 0)
                    return (false);

                try {
                    wt.join (ttw);
                } catch (InterruptedException x) {
                    throw new UncheckedInterruptedException (x);
                }
            }
        }
        writers.clear();

        int dirtyFiles;
        synchronized (cleanLock) {
            dirtyFiles = numDirtyFiles;
        }

        LOGGER.info("Writers successfully stopped, files in queue = %s").with(dirtyFiles);

        return (true);
    }

    synchronized void                   writerFailed(TSFWriterThread writer) {
        writers.remove(writer);
        if (writers.size() == 0) {
            LOGGER.error("CRITICAL ERROR IN Writer Threads. Timebase will shutdown.");
            triggerEmergencyShutdown();
        }
    }

    private void triggerEmergencyShutdown() {
        EmergencyShutdownControl st = this.shutdownControl;
        if (st != null) {
            st.triggerEmergencyShutdown();
        } else {
            Shutdown.asyncTerminate();
        }
    }

    @Override
    public TSRootFolder createRoot(@Nullable String space, AbstractFileSystem fs, String path) {
        checkIsStarted ();
        checkShutdown ();

        return new TSRootFolder(this, fs, path, space);
    }

    @Override
    public TSRoot createRoot(@Nullable String space, AbstractPath path) {
        checkIsStarted ();
        checkShutdown ();

        return new TSRootFolder(this, path.getFileSystem(), path.getPathString(), space);
    }

    @Override
    public DataWriter       createWriter () {
        checkIsStarted ();
        checkShutdown ();

        return (new DataWriterImpl ());
    }
    
    @Override
    public DataReader       createReader (boolean live) {
        checkIsStarted ();
        checkShutdown ();

        QuickExecutor exe = this.executor != null ? executor : localExecutor;

        return live ? new LiveDataReaderImpl(exe) : new DataReaderImpl (exe);
    }

    @Override
    public void setEmergencyShutdownControl(EmergencyShutdownControl shutdownControl) {
        this.shutdownControl = shutdownControl;
    }

//    public ByteArrayHeap      getHeap() {
//        return allocator;
//    }

    //
    //  PACKAGE INTERFACE
    //
    boolean                    removeFromWriteQueue (TSFile tsf) {

        boolean removed = queue.remove(tsf);

        // file can be processed now by Writer Thread, so it's not contains in dirty files
        if (removed) {
            synchronized (cleanLock) {
                numDirtyFiles--;
                updateDirtyFilesGauge();
                cleanLock.notifyAll();
            }
        }

        return removed;
    }

    void                    fileWasCheckedInClean (TSFile tsf) {
    }

    void                    fileHasFailed (TSFile tsf, Throwable x) {
        if (tsf.isActive()) {
            LOGGER.error().append("Error storing ").append(tsf).append(". Aborting.").append(x).commit();
            boolean discardFile = false;

            synchronized (queue) {
                int failuresPerFile = failuresByFile.computeIfAbsent(tsf.getPathString(), tsFile -> new AtomicInteger(0)).incrementAndGet();
                if (failuresPerFile < ATTEMPTS_PER_FILE) {
                    // Try to process file again
                    if (shutdownInProgress) {
                        // Add it to "bad" file list: files in "bad" list have lower priority
                        queue.addFailed(tsf);
                    } else {
                        // Add it into regular list to avoid a situation when high writer load effectively prevents re-processing the file at all
                        queue.add(tsf);
                    }
                } else {
                    discardFile = true;
                }
            }

            if (discardFile) {
                // Note: we do not call fileProcessed(tsf) on that execution branch:
                // we let TB to consider it queued to avoid adding it to queue again.
                synchronized (cleanLock) {
                    numDirtyFiles--;
                    updateDirtyFilesGauge();
                    cleanLock.notifyAll();
                }

                LOGGER.error("Failed to store %s after %s attempts: %s")
                        .with(tsf.getPathString())
                        .with(ATTEMPTS_PER_FILE)
                        .with(x);;
            }


            // TODO: We can make few additional attempts first
            int attemptsFailed = failedWriteAttempts.incrementAndGet();

            // We use "==" because the counter is atomic and will return FAILURES_TO_TRIGGER_SHUTDOWN exactly once.
            if (attemptsFailed == FAILURES_TO_TRIGGER_SHUTDOWN || discardFile) {
                triggerEmergencyShutdown();
            }

        } else {
            LOGGER.error().append("Error storing ").append(tsf.toShortString()).append(".").append(x != null ? x : "").commit();
            fileProcessed(tsf);
        }
    }

    void                    fileWasStored (TSFile tsf) {
        //
        //  Later optionally cache it... for now, drop.
        //
        //  It is CRITICAL to first unuse the file, as the below notification
        //  is usually the last step on the way to closing the root.
        //
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("%s stored successfully. state = %s").with(tsf.getPathString()).with(tsf.getState());

        TreeOps.unuse (tsf);
        fileProcessed(tsf);
    }

    void                    fileWasDropped (TSFile tsf) {
        // do not call "unuse" here
        fileProcessed(tsf);
    }

    private void            fileProcessed(TSFile tsf) {
        synchronized (queue) {
            tsf.queued = false;
        }

        // change numDirtyFiles at the end of processing, to be able to wait until all data stored
        synchronized (cleanLock) {
            numDirtyFiles--;
            updateDirtyFilesGauge();
            cleanLock.notifyAll();
        }
    }
    
    void                    addToWriteQueue (TSFile tsf) {
        synchronized (this) {
            checkIsStarted ();

            if (writers.size() == 0)
                throw new IllegalStateException("No active writers available");

            for (TSFWriterThread writer : writers) {
                if (writer.isInterrupted())
                    throw new IllegalStateException("Writer thread is interrupted");
            }
            
            if (isReadOnly)
                throw new IllegalStateException ("This PDS is running in read-only mode");
        }

        maxFileSize = Math.max(maxFileSize, tsf.getUncompressedSize());

        assert !queue.contains(tsf);

        synchronized (cleanLock) {
            numDirtyFiles++;
            updateDirtyFilesGauge();
            cleanLock.notifyAll();
        }
        assert !queue.contains(tsf);

        queue.add (tsf);

        if (shutdownInProgress && LOGGER.isInfoEnabled()) {
            LOGGER.info("Added file to be saved during shutdown: %s").with(tsf.getPathString());
        }
    }
    
    TSFile                  getTSFToWrite (int index) throws InterruptedException {
        return queue.poll(index);
    }

    private void            checkIsStarted () throws IllegalStateException {
        if (!isStarted)
            throw new IllegalStateException ("Not started");
    }

    private void            checkShutdown () throws IllegalStateException {
        if (shutdownInProgress)
            throw new IllegalStateException ("Shutdown in progress");
    }

    void fileActivated() {
        //activeFilesGauge.incrementAndGet();
    }

    void fileDeactivated() {
        //activeFilesGauge.decrementAndGet();
    }

    void fileRead() {
        //ioReadsCount.incrementAndGet();
    }

    void fileWritten() {
        //ioWritesCount.incrementAndGet();
    }

    private void updateDirtyFilesGauge() {
        //dirtyFilesGauge.set(numDirtyFiles);
    }
}