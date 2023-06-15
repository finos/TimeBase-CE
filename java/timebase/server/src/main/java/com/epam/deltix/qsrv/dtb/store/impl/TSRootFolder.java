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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.qsrv.dtb.store.codecs.*;
import com.epam.deltix.qsrv.dtb.store.dataacc.*;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.tickdb.impl.PDStreamSpaceIndexManager;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.concurrent.*;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.time.GMT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.epam.deltix.qsrv.dtb.store.impl.TreeOps.*;
import static com.epam.deltix.qsrv.dtb.store.impl.TreeOps.getNextFile;

/**
 *
 */
final class TSRootFolder extends TSFolder implements TSRoot, TimeSliceStore {

    // LOCK Order: structure lock -> this

    private final static int COMPRESSION_RATIO = 3;

    private volatile boolean isOpen = false;
    private volatile boolean readOnly;
    private int maxFileSize = MAX_FILE_SIZE_DEF;
    private int maxFolderSize = MAX_FOLDER_SIZE_DEF;
    private String compression = COMPRESSION_DEF;
    private boolean configIsDirty;
    private final AbstractFileSystem fs;
    private final String path;
    private String space;
    private int spaceIndex = PDStreamSpaceIndexManager.NO_INDEX;
    private final PDSImpl cache;
    private final SymbolRegistryImpl symRegistry = new SymbolRegistryImpl();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    // indicates that we have active writers now, affects time range retrieval
    private final AtomicInteger writing = new AtomicInteger(0);

    // global structure index
    private final AtomicLong sequence = new AtomicLong(0);

    TSRootFolder(PDSImpl cache, AbstractFileSystem fs, String path, @Nullable String space) {
        super();

        this.path = path;
        this.fs = fs;
        this.cache = cache;
        this.space = space;
    }

    //
    //  TSRoot IMPLEMENTATION
    //
    @Override
    public PersistentDataStore getStore() {
        return (cache);
    }

    @Override
    public AbstractFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public void open(boolean readOnly) {
        this.isOpen = true;
        this.readOnly = readOnly;

        acquireSharedLock();

        try {
            AbstractPath path = getPath();

            symRegistry.load(path);
            loadProperties(path);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException("Failed to read symbols from [" + path + "]", iox);
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public boolean          isOpen() {
        return isOpen;
    }

    public void delete() {

        acquireWriteLock();
        try {
            isOpen = false;

            super.format();
            FSUtils.removeRecursive(getPath(), true, null);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public void format() {
        isOpen = true;
        readOnly = false;

        acquireWriteLock();

        try {
            super.format();
            FSUtils.removeRecursive(getPath(), false, null);

            use(this);

            AbstractPath path = getPath();
            path.makeFolderRecursive();

            initNew();
            storeDirtyData();
            symRegistry.format(path);

            configIsDirty = true;
            storePropertiesIfDirty(path);

            sequence.set(0);

            writingStarted();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {

            try {
                unuse(this);
            } catch (Exception ex) {
                LOGGER.warn("Error while formatting: %s").with(ex);
                // ignore
            }
            releaseWriteLock();
        }
    }

    @Override
    public long             getVersion() {
        return sequence.get();
    }

    @Override
    public void             setVersion(long version) {
        sequence.set(version);
    }

    @Override
    public SymbolRegistryImpl getSymbolRegistry() {
        return (symRegistry);
    }

    @Override
    public void close() {

        synchronized (this) {

            if (!isOpen)
                return;

            assert writing.get() == 0;

            if (!isActive()) {
                storeRegistry();
                symRegistry.close();
                isOpen = false;
                return;
            }
        }

        //
        //  If we are here, we have active children. This is a problem.
        //  Nothing is closed. Instead, throw a meaningful exception.
        //
        acquireSharedLock();

        try {
            ArrayList<TSFolderEntry> activeChildren = new ArrayList<>();

            findActiveChildren(activeChildren);

            StringBuilder err = new StringBuilder("Nodes active at close-time:");

            for (TSFolderEntry tsf : activeChildren) {
                err.append("\n    ");
                err.append(tsf.toString()).append(": ").append(tsf.useCount);
                if (tsf instanceof TSFile) {
                    DAPrivate[] snapshot = ((TSFile) tsf).getCheckouts();

                    for (int i = 0; snapshot != null && i < snapshot.length; i++) {
                        if (snapshot[i] != null)
                            err.append(" ").append(snapshot[i]);
                    }
                }
            }

            throw new IllegalStateException(err.toString());
        } finally {
            releaseSharedLock();
        }
    }

    private void storeRegistry() {
        try {
            symRegistry.storeIfDirty(getPath());
        } catch (IOException e) {
            LOGGER.warn().append("Error storing symbols in [").append(this).append("]: ").append(e).commit();
        }
    }

    @Override
    public synchronized void forceClose() {
        if (!isOpen)
            return;

        if (isActive())
            LOGGER.warn().append("FORCE-Closing ").append(this).append(" while in active state").commit();

        storeRegistry();
        symRegistry.close();
        isOpen = false;
    }

    @Override
    public synchronized int getMaxFolderSize() {
        return (maxFolderSize);
    }

    @Override
    public synchronized int getMaxFileSize() {
        if (compression == null)
            return maxFileSize;

        if ((long) maxFileSize * (long) COMPRESSION_RATIO > Integer.MAX_VALUE)
            return maxFileSize;

        return (maxFileSize * COMPRESSION_RATIO);
    }

    @Override
    public synchronized void setMaxFileSize(int numBytes) {
        if (numBytes != maxFileSize) {
            if (numBytes < MAX_FILE_SIZE_LOW || numBytes > MAX_FILE_SIZE_HIGH)
                throw new IllegalArgumentException("Illegal maxFileSize: " + numBytes);

            maxFileSize = numBytes;
            configIsDirty = true;
        }
    }

    @Override
    public synchronized void setMaxFolderSize(int numTimeSlices) {
        if (numTimeSlices != maxFolderSize) {
            if (numTimeSlices < MAX_FOLDER_SIZE_LOW || numTimeSlices > MAX_FOLDER_SIZE_HIGH)
                throw new IllegalArgumentException("Illegal numTimeSlices: " + numTimeSlices);

            maxFolderSize = numTimeSlices;
            configIsDirty = true;
        }
    }

    @Override
    public synchronized String getCompression() {
        return compression;
    }

    @Override
    public synchronized void setCompression(String compression) {
        if (this.compression == null || !this.compression.equalsIgnoreCase(compression)) {
            this.compression = compression;
            configIsDirty = true;
        }
    }

    public BlockCompressor createCompressor(ByteArrayList buffer) {
        if (compression == null || compression.isEmpty())
            return null;

        return BlockCompressorFactory.createCompressor(compression, buffer);
    }

    public BlockDecompressor createDecompressor(byte compressionCode) {
        return BlockCompressorFactory.createDecompressor(compressionCode);
    }

    @Override
    public void getTimeRange(int id, TimeRange out) {
        TimeRange range = symRegistry.getTimeRange(id);

        if (range != null && writing.intValue() == 0) {
            out.from = range.from;
            out.to = range.to;
        } else {

            acquireSharedLock();
            try {
                out.from = TreeOps.getFromTimestamp(this, id);
                out.to = TreeOps.getToTimestamp(this, id);
                symRegistry.setTimeRange(id, new TimeRange(out));
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                releaseSharedLock();
            }
        }
    }

    @Override
    public void getTimeRange(TimeRange out) {

        TimeRange range = symRegistry.getTimeRange();

        if (range != null && writing.intValue() == 0) {
            out.from = range.from;
            out.to = range.to;
        } else {
            acquireSharedLock();

            try {
                TSFile first = TreeOps.getFirstFile(this, EntityFilter.ALL);
                if (first != null) {
                    out.from = first.getFromTimestamp();
                    unuse(first);
                }

                TSFile last = TreeOps.getLastFile(this, EntityFilter.ALL);
                if (last != null) {
                    out.to = last.getToTimestamp();
                    unuse(last);
                }

            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                releaseSharedLock();
            }
        }
    }

    @Override
    public TimeInterval[] getTimeRanges(int[] ids) {
        ArrayList<EntityTimeRange> out = new ArrayList<EntityTimeRange>();
        ArrayList<EntityTimeRange> ask = new ArrayList<EntityTimeRange>();

        for (int id : ids) {
            if (id != -1) {
                EntityTimeRange e = new EntityTimeRange(id);
                TimeRange r = symRegistry.getTimeRange(id);
                if (r != null && writing.intValue() == 0) {
                    e.to = r.to;
                    e.from = r.from;
                } else {
                    ask.add(e);
                }
                out.add(e);
            } else {
                out.add(null);
            }
        }

        if (ask.size() > 0) {
            acquireSharedLock();
            try {
                TreeOps.getFromTimestamp(this, ask);
                TreeOps.getToTimestamp(this, ask);
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                releaseSharedLock();
            }

            if (writing.intValue() == 0) {
                // apply changes
                for (EntityTimeRange e : out) {
                    if (e != null)
                        symRegistry.setTimeRange(e.entity, new TimeRange(e));
                }
            }
        }

        return out.toArray(new TimeInterval[out.size()]);
    }

    @Override
    public void selectTimeSlices(
            TimeRange range,
            EntityFilter filter,
            Collection<TSRef> addTo
    ) {
        acquireSharedLock();

        long from = range != null ? range.from : Long.MIN_VALUE;
        long to = range != null ? range.to : Long.MAX_VALUE;

        try {
            TSFile tsf = findTSFForRead(this, from);

            for (; ; ) {
                if (tsf == null)
                    return;

                TSFile next;

                try {
                    if (tsf.getStartTimestamp() > to)
                        break;

                    if (filter == null || tsf.hasDataFor(filter))
                        addTo.add(new TSRefImpl(tsf));

                    next = getNextFile(tsf, null);
                } finally {
                    unuse(tsf);
                }

                tsf = next;
            }
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public void iterate(TimeRange range, EntityFilter filter, TimeSliceIterator it) {

        sequence.incrementAndGet();
        boolean writingStarted = false;

        try {
            long from = range != null ? range.from : Long.MIN_VALUE;
            long to = range != null ? range.to : Long.MAX_VALUE;

            boolean locked = false;
            TSFile tsf;

            try {
                locked = acquireSharedLock();
                tsf = findTSFForRead(this, from);
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                if (locked)
                    releaseSharedLock();
            }

            for (;;) {
                locked = false;

                if (tsf == null)
                    return;

                TSFile next;

                try {
                    if (tsf.getStartTimestamp() > to)
                        break;

                    // should be out of structure lock
                    getCache().checkWriteQueueLimit(getMaxFileSize());

                    locked = acquireWriteLock();

                    if (!writingStarted)
                        writingStarted();

                    writingStarted = true;

                    if (filter == null || tsf.hasDataFor(filter)) {

                        DataAccessorBase accessor = (DataAccessorBase) it.getAccessor();

                        safeCheckOut(tsf, accessor);
                        accessor.associate(tsf);
                        try {
                            it.process(tsf);
                            if (tsf.isEmpty() && !tsf.isFirst())
                                tsf.drop();
                        } finally {
                            tsf.checkedInBy(accessor);
                            accessor.associate((TimeSlice) null);
                        }
                    }
                    next = getNextFile(tsf, null);

                } finally {
                    release(tsf, locked);

                    if (locked)
                        releaseWriteLock();
                }

                tsf = next;
            }

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    private void release(TSFile file, boolean hasLock) {
        if (hasLock) {
            unuse(file);
        } else {
            boolean locked = false;
            try {
                locked = acquireSharedLock();
                unuse(file);
            } finally {
                if (locked)
                    releaseSharedLock();
            }
        }
    }

    public boolean drop(TimeRange range) {

        long from = range != null ? range.from : Long.MIN_VALUE;
        long to = range != null ? range.to : Long.MAX_VALUE;

        boolean changed = false;

        sequence.incrementAndGet();
        writingStarted();

        try {
            boolean locked = false;
            TSFile tsf;

            try {
                locked = acquireSharedLock();
                tsf = findTSFForRead(this, from);
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                if (locked)
                    releaseSharedLock();
            }

            for (;;) {
                locked = false;
                TSFile next;

                if (tsf == null)
                    return false;

                try {
                    long timestamp = tsf.getStartTimestamp();

                    if (timestamp >= to)
                        break;

                    locked = acquireWriteLock();

                    next = getNextFile(tsf, null);

                    if (!tsf.isFirst()) { // do not touch first file

                        if (next != null && next.getStartTimestamp() <= to) {
                            if (timestamp > from) {
                                changed = true;
                                tsf.drop();
                            }
                        } else if (next == null) {
                            // tsf is last file, we can load into memory to check actual time range
                            if (timestamp > from && tsf.getToTimestamp() < to) {
                                changed = true;
                                tsf.drop();
                            }
                        }
                    }
                } finally {
                    release(tsf, locked);

                    if (locked)
                        releaseWriteLock();
                }

                tsf = next;
            }

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
        return changed;
    }

    @Override
    public TSRef associate(String path) {
        acquireSharedLock();

        try {
            TSFile tsf =
                    findTSFForRead(this, Long.MIN_VALUE);

            for (; ; ) {
                if (tsf == null)
                    return null;

                TSFile next;

                try {
                    if (path.equals(tsf.getPathString()))
                        return new TSRefImpl(tsf);

                    next = getNextFile(tsf, null);
                } finally {
                    unuse(tsf);
                }

                tsf = next;
            }
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseSharedLock();
        }
    }

    //
    //  TimeSliceStore IMPLEMENTATION
    //    
    @Override
    public TimeSlice checkOutTimeSliceForRead(
            DAPrivate accessor,
            long timestamp,
            EntityFilter filter
    ) {
        // check that we have enough memory
        root.getCache().checkWriteQueueLimit(root.getMaxFileSize());

        acquireSharedLock();

        try {
            // do not subtract 1 from MIN_VALUE...
            TSFile tsf = findTSFForRead(this, timestamp);

            if (tsf == null)
                return (null);

            safeCheckOut(tsf, accessor);

            return (tsf);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public void checkInTimeSlice(
            DAPrivate accessor,
            TimeSlice slice
    ) {
        TSFile tsf = (TSFile) slice;

        acquireSharedLock();
        try {
            if (tsf.checkedInBy(accessor)) {
                unuse(tsf);

                if (accessor instanceof DataWriter) {
                    symRegistry.clearRange();
                    writing.decrementAndGet();
                }
            }
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public TimeSlice checkOutTimeSlice(
            DAPrivate accessor,
            TSRef tsref
    ) {
        acquireSharedLock();

        try {
            TSFile tsf = findTSFByRef(this, (TSRefImpl) tsref);

            if (tsf == null)
                return (null);

            safeCheckOut(tsf, accessor);

            return (tsf);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public TimeSlice checkOutTimeSlice(
            DAPrivate accessor,
            TimeSlice slice
    ) {
        acquireSharedLock();

        try {
            TSFile tsf = (TSFile) slice;
            use(tsf);
            safeCheckOut(tsf, accessor);
            return tsf;
        } finally {
            releaseSharedLock();
        }
    }

    @Override
    public TimeSlice checkOutTimeSliceForInsert(
            DAPrivate accessor,
            long timestamp
    ) {
        acquireWriteLock();

        TSFile tsf;
        try {
            // do not subtract 1 from MIN_VALUE...
            tsf = findTSFForInsert(this, timestamp);

            safeCheckOut(tsf, accessor);

            tsf.hasDataFor(EntityFilter.ALL); // make sure that all data is loaded
            writing.incrementAndGet();

            if (writing.get() == 1)
                writingStarted();

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseWriteLock();
        }

        // out of write lock
        onSliceCheckout(tsf);
        return (tsf);
    }

    public void         writingStarted() {
        AbstractPath tmp = root.getPath().append(TSNames.LOCK_NAME);

        if (!tmp.exists()) {
            try (OutputStream os = new BufferedOutputStream(tmp.openOutput(0))) {
                DataOutputStream out = new DataOutputStream(os);
                out.writeLong(System.currentTimeMillis());
            } catch (IOException ex) {
                LOGGER.warn(ex.getMessage());
            }
        }
    }

    @Nullable
    @Override
    public TimeSlice getNextTimeSliceToRead(
            DAPrivate accessor,
            TimeSlice previous,
            EntityFilter filter,
            boolean forward,
            boolean keepPrevCheckout
    )
            throws InterruptedException {
        TSFile prevTSF = (TSFile) previous;

        acquireSharedLock();

        try {
            TSFile next =
                    forward ? getNextFile(prevTSF, filter) : getPreviousFile(prevTSF, filter);

            if (keepPrevCheckout) {
                if (next != null)
                    next.checkOutTo(accessor);
            } else {
                safeSwitch(prevTSF, next, accessor); // Note: "next" may be null
            }

            return (next);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            releaseSharedLock();
        }
    }

    boolean     hasNextSlice(TimeSlice current, EntityFilter filter, boolean forward) {
        TSFile prevTSF = (TSFile) current;

        acquireSharedLock();

        try {
            TSFile next =
                    forward ? getNextFile(prevTSF, filter) : getPreviousFile(prevTSF, filter);

            return (next != null);
        } catch (IOException iox) {
            return false;
        } finally {
            releaseSharedLock();
        }
    }

    TSFile split(long nstime, TSFile file, DAPrivate accessor) throws IOException {

        assert rwl.isWriteLockedByCurrentThread();

        sequence.incrementAndGet(); // increment global sequence
        writingStarted();

        insertNotify(file.getParent());

        long middle = file.getSplitTime((DataAccessorBase) accessor);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append("Splitting ").append(this).append(" to time ").append(GMT.formatNanos(middle)).commit();

        TSFile after = getNextFile(file, null);

        //
        //  Note: insertNotify() may change the value of prevTSF.getParent ()
        //  It is critical to re-get it!
        //
        final TSFile next = file.getParent().createFileAfter(file, middle);

        if (after != null) {
            long time = after.getStartTimestamp();
            if (TreeOps.isValid(time))
                next.limitTimestamp = time;

            unuse(after);
        }

        use(next);    // lock parent before checking in prev. TSF!

        file.split(middle, next, (DataAccessorBase) accessor);

        if (nstime > middle) {
            safeSwitch(file, next, accessor);
            return next;
        } else {
            next.blockGoesDirty(null);
            next.checkIn();
        }

        return file;
    }

    public final ArrayList<SliceListener> sliceListeners = new ArrayList<SliceListener>(5);

    @Override
    public void addSliceListener(SliceListener listener) {
        synchronized (sliceListeners) {
            sliceListeners.add(listener);
        }
    }

    @Override
    public void removeSliceListener(SliceListener listener) {
        synchronized (sliceListeners) {
            sliceListeners.remove(listener);
        }
    }

    void onSliceCheckout(TSFile file) {
        synchronized (sliceListeners) {
            for (int i = 0; i < sliceListeners.size(); i++)
                sliceListeners.get(i).checkoutForInsert(file);
        }
    }

    //
    //  Package interface
    //
    TSFile getNextTimeSliceToWrite (
            long timestamp,
            DAPrivate accessor,
            TSFile prevTSF
    ) throws IOException {

        //cache.checkWriteQueueLimit();

        // should hold write lock here
        sequence.incrementAndGet();

        TSFile next;

        insertNotify(prevTSF.getParent());
        //
        //  Note: insertNotify() may change the value of prevTSF.getParent ()
        //  It is critical to re-get it!

        next = getNextFile(prevTSF, null);

        if (next == null) {
            next = prevTSF.getParent().createFileAfter(prevTSF, timestamp);
            use(next);    // lock parent before checking in prev. TSF!
            prevTSF.limitTimestamp = timestamp;
        } else {
            // assign limit time if next file exists
            long ts = getLimitTimestamp(next);

            if (TreeOps.isValid(ts))
                next.limitTimestamp = ts;

            // check that next file contains given timestamp
            // if not - search for the file
            if (next.limitTimestamp < timestamp) {
                TSFile prev = next;
                next = findTSFForInsert(prevTSF.root, timestamp);
                unuse(prev);
            }

            //  if next file is empty - set start time
            if (next != null && next.getStartTimestamp() == Long.MIN_VALUE)
                next.setStartTimestamp(timestamp);
        }

        safeSwitch(prevTSF, next, accessor);

        // out of write lock
        onSliceCheckout(next);

        return (next);
    }

    //
    //  Package interface
    //
    TSFile getPreviousTimeSliceToWrite(
            long timestamp,
            DAPrivate accessor,
            TSFile prevTSF
    )
            throws IOException {

        // we can wait here
        //cache.checkWriteQueueLimit();

        sequence.incrementAndGet();

        TSFile next;

        next = findTSFForInsert(this, timestamp);

        assert next != null;

        safeSwitch(prevTSF, next, accessor);

        // out of write lock
        onSliceCheckout(next);

        return (next);
    }

    //
    //  Root Behaviors
    //
    @Override
    TSFolderEntry getNextSibling() {
        return (null);
    }

    @Override
    TSFolderEntry getPreviousSibling() {
        return (null);
    }

    public AbstractFileSystem getFS() {
        return (fs);
    }

    @Override
    public String getPathString() {
        return (path);
    }

//    @Override
//    public long             getLimitTimestamp () {
//        return (Long.MAX_VALUE);
//    }

    PDSImpl getCache() {
        return (cache);
    }

    boolean tryAcquireSharedLock() {
        boolean result = (rwl.readLock().tryLock());
        if (result)
            logLockInfo(true, "Acquired");
        return result;
    }

    void logLockInfo(boolean shared, String op) {
//        String name = shared ? " shared lock on " : " WRITE lock on ";
//
//        LOGGER.level(Level.DEBUG).append("Thread ").append(Thread.currentThread().getName()).append(" ").append(op).append(name).
//                append(getPathString()).append(" (").append(rwl.toString()).append(new Exception()).
//                commit();
    }

    boolean isWriteLockedByCurrentThread() {
        return rwl.isWriteLockedByCurrentThread();
    }

    boolean acquireSharedLock() {
        if (!isOpen)
            throw new IllegalStateException(this + " is not open");

        if (LOGGER.isDebugEnabled())
            logLockInfo(true, "Acquiring");

        try {
            rwl.readLock().lockInterruptibly();
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException(x);
        }

        if (LOGGER.isDebugEnabled())
            logLockInfo(true, "Acquired");

        return true;
    }

    void releaseSharedLock() {
        rwl.readLock().unlock();

        if (LOGGER.isDebugEnabled())
            logLockInfo(true, "Released");
    }

    boolean acquireWriteLock() {
        if (!isOpen)
            throw new IllegalStateException(this + " is not open");

        if (readOnly)
            throw new IllegalStateException(this + " is open in read-only mode");

        if (LOGGER.isDebugEnabled())
            logLockInfo(false, "Acquiring");

        assert rwl.getReadHoldCount() == 0;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug(rwl + " has read locks: " + rwl.getReadHoldCount());

        try {
            symRegistry.clearRange(); // clear ranges - temp fix

            rwl.writeLock().lockInterruptibly();
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException(x);
        }

        if (LOGGER.isDebugEnabled())
            logLockInfo(false, "Acquired");

        return true;
    }

    void releaseWriteLock() {
        rwl.writeLock().unlock();

        if (LOGGER.isDebugEnabled())
            logLockInfo(false, "Released");
    }

    boolean currentThreadHoldsAnyLock() {
        return rwl.isWriteLockedByCurrentThread() ||
                rwl.getReadHoldCount() > 0;
    }

    boolean currentThreadHoldsWriteLock() {
        return rwl.isWriteLockedByCurrentThread();
    }

    void storeAdditionalDirtyData() throws IOException {
        AbstractPath path = getPath();

        symRegistry.storeIfDirty(path);
        storePropertiesIfDirty(path);
    }
    //
    //  INTERNALS
    //

    /**
     * Check out the TSF to the specified accessor, and in case of any failure
     * whatsoever, un-use the TSF.
     */
    private static void safeCheckOut(TSFile tsf, DAPrivate accessor) {
        boolean success = false;

        try {
            tsf.checkOutTo(accessor);
            success = true;
        } finally {
            if (!success)
                unuse(tsf);
        }
    }

    /**
     * Check in old TSF, and check out a new TSF to the specified accessor,
     * and in case of any failure whatsoever, un-use the new TSF.
     */
    private static void safeSwitch(
            @Nonnull TSFile oldTSF,
            @Nullable TSFile newTSF,
            @Nonnull DAPrivate accessor
    ) {
        boolean success = false;

        try {
            if (oldTSF.checkedInBy(accessor))
                unuse(oldTSF);

            if (newTSF != null)
                newTSF.checkOutTo(accessor);

            success = true;
        } finally {
            if (!success && newTSF != null)
                unuse(newTSF);
        }
    }

    private void loadProperties(AbstractPath folder)
            throws IOException {
        AbstractPath fp = folder.append(TSNames.ROOT_PROPS_NAME);

        try (InputStream is = BufferedStreamUtil.wrapWithBuffered(fp.openInput(0))) {
            Properties props = new Properties();

            props.load(is);

            if (props.containsKey("maxFileSize"))
                setMaxFileSize(Integer.parseInt(props.getProperty("maxFileSize")));

            if (props.containsKey("maxFolderSize"))
                setMaxFolderSize(Integer.parseInt(props.getProperty("maxFolderSize")));

            if (props.containsKey("compression"))
                setCompression(props.getProperty("compression"));
        } catch (FileNotFoundException x) {
            // Ignore for compatibility. Will take this out later.
        }

        configIsDirty = false;
    }

    private void storePropertiesIfDirty(AbstractPath folder)
            throws IOException {
        if (!configIsDirty)
            return;

        AbstractPath tmp = TreeOps.makeTempPath(folder, TSNames.ROOT_PROPS_NAME);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append("Storing root properties").commit();

        try (OutputStream os = tmp.openOutput(0)) {
            SortedProperties props = new SortedProperties();

            props.setProperty("maxFileSize", String.valueOf(maxFileSize));
            props.setProperty("maxFolderSize", String.valueOf(maxFolderSize));
            props.setProperty("compression", compression == null ? "" : compression);

            props.store(os, null);
        }

        TreeOps.finalize(tmp);

        configIsDirty = false;
    }

    @Override
    public void setSpaceIndex(int index) {
        this.spaceIndex = index;
    }

    @Override
    public int getSpaceIndex() {
        return this.spaceIndex;
    }

    @Nullable
    @Override
    public String getSpace() {
        return space;
    }

    @Override
    public void setSpace(String name) {
        space = name;
    }

    public long getSequence() {
        return sequence.get();
    }
}