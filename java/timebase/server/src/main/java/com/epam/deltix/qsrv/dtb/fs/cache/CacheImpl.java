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
package com.epam.deltix.qsrv.dtb.fs.cache;

import com.google.common.util.concurrent.Striped;
import com.epam.deltix.gflog.api.*;
import com.epam.deltix.qsrv.dtb.fs.alloc.BinaryBuddyHeapManager;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;
import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.collections.CharSequenceToObjectMapQuick;
import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimerRunner;
import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/** Thread-safe
 *
 * Cache entries may exist in one of three states:
 *
 * a) Free - listed in vacant entries list, userCounter=0, pathString = null (no content)
 * b) Reusable - listed in vacant entries, userCount=0, pathString != null, registered in entriesByPath (has valid content)
 * c) Used - not listed in vacant entries, userCount > 0, pathString !=null, registered in entriesByPath (has valid content)
 */
class CacheImpl implements Cache {

    private static final int STRIPPED_SIZE = 2048;

    private static final boolean DEFRAG = true;
    private final Object lock = new Object();

    static final Log LOG = LogFactory.getLog(CacheImpl.class);

    /** Cache of entries. Some of them could be currently checked out and some not (in latter case they will be also in vacant list) */
    @GuardedBy("lock")
    private final CharSequenceToObjectMapQuick<CacheEntryImpl> entriesByPath; // assert entry.pathString is not null

    private final Striped<Lock> fileLocks = Striped.lock(STRIPPED_SIZE);

    private final HeapManager heap;

    /**
     * Pool of cache entries that may or may not have content. Ordered by most recently used entries going last.
     *
     * Entries that have re-usable content have their {@link CacheEntry#getPathString()} set to not null.
     * Entries that do not have pathString do not have any data buffer (it is returned to the heap).
     *
     * NOTE: all entries in this list have *zero* reference counter.
     */
    @GuardedBy("lock")
    private final QuickList <CacheEntryImpl> vacant = new QuickList <> (); // assert entry.pathString is null and entry.userCount == 0

    public static CacheImpl create (int size, int maxEntrySize) {

        long roundedMaxEntrySize = BinaryBuddyHeapManager.getBlockSize((long) maxEntrySize * size);
        long maxBlockSize = roundedMaxEntrySize;
        if (maxBlockSize > Integer.MAX_VALUE)
            throw new IllegalArgumentException(new StringBuilder().append("Max block size is too large: ").append(maxBlockSize).toString());
        int minBlockSize = (int)(roundedMaxEntrySize >> 8);

        return new CacheImpl(size, minBlockSize, (int) maxBlockSize);
    }

    public CacheImpl (int size, int minBlockSize, int maxBlockSize) {
        entriesByPath = new CharSequenceToObjectMapQuick<>(size);

        LOG.log(LogLevel.INFO).append("Allocating ").append(maxBlockSize >> 20).append("Mb for FileSystem cache").commit();
        heap = new BinaryBuddyHeapManager(minBlockSize, maxBlockSize);

        // pre-allocate
        for (int i = 0; i < size; i++)
            vacant.linkFirst(new CacheEntryImpl());
        scheduleExpirationJob();
    }

    @Override
    public CacheEntry checkOut(CacheEntryLoader cacheEntryLoader) {

        final String pathString = cacheEntryLoader.getPathString();
        CacheEntryImpl result;

        Lock fileLock = fileLocks.get(pathString);
        fileLock.lock();
        try {
            synchronized (lock) {
                result = entriesByPath.get(pathString, null);
                if (result != null) {
                    if (result.userCounter == 0) {
                        assert vacant.contains(result);
                        result.unlink();
                    } else {
                        assert !vacant.contains(result);
                    }

                    result.userCounter++;
                    assert result.pathString != null;
                } else { // Find vacant entry to hold new content
                    result = takeFirstVacant();
                    if (result != null) {
                        assert result.userCounter == 0;
                        assert result.pathString == null;
                    }
                }
            }

            if (result != null && result.pathString == null) {
                if (LOG.isEnabled(LogLevel.DEBUG))
                    LOG.log(LogLevel.DEBUG).append("Cache Miss(").append(pathString).append(')').commit();

                loadCacheEntry(cacheEntryLoader, result);

                synchronized (lock) {
                    if (result.pathString != null) { // path indicates a successful load
                        CacheEntryImpl winner = entriesByPath.putAndGetIfEmpty(pathString, result);
                        if (winner != result) { // another thread got ahead of us and loaded the same file
                            result.pathString = null;
                            heap.deallocate(result.getBuffer());
                            assert result.userCounter == 0;
                            vacant.linkFirst(result);

                            if (winner.userCounter == 0) {
                                assert vacant.contains(winner);
                                winner.unlink();
                            }

                            result = winner;
                        }
                        result.userCounter++;
                    } else { // load failure => discard entry
                        assert result.userCounter == 0;
                        vacant.linkFirst(result);
                        result = null;
                    }
                }
            }
        } finally {
            fileLock.unlock();
        }
        assert result == null || pathString.equals(result.getPathString());
        ///LOG.level(Level.INFO).append("CheckOut(").append(pathString).append(")=").append(result).append(" THREAD:"+Thread.currentThread().getId()).commit();
        return result;
    }

    @Override
    public void checkIn(CacheEntry entry) {
        CacheEntryImpl e = (CacheEntryImpl)entry;
        synchronized (lock) {
            e.userCounter --;
            if (e.userCounter < 0)
                throw new IllegalStateException("Cache entry has been checked in more than once: " + e.pathString);
            if (e.userCounter == 0) {
                assert ! vacant.contains(e);
                vacant.linkLast(e);
                if (DEFRAG)
                    heap.defragment(e.getBuffer());
            }
        }
        ///LOG.level(Level.INFO).append("CheckIn(").append(e.getPathString()).append("=").append(e).append(" THREAD:"+Thread.currentThread().getId()).commit();
    }

    private CacheEntryImpl takeFirstVacant() {
        CacheEntryImpl result = vacant.getFirst();
        if (result != null) {
            assert result.userCounter == 0;
            result.unlink();
            if (result.pathString != null) {
                entriesByPath.remove(result.pathString);
                result.pathString = null;
                heap.deallocate(result.getBuffer());
            }
        }
        return result;
    }

    @Override
    public void invalidate(String pathString) {
        synchronized (lock) {
            final CacheEntryImpl entry = entriesByPath.remove(pathString, null);
            if (entry != null) {
                assert entry.pathString != null;
                entry.pathString = "\0renamed\0";
                if (entry.userCounter == 0) { //NOTE: Otherwise is checked out by one or several readers - invalidate only affects new readers
                    assert vacant.contains(entry);
                    entry.unlink();
                    vacant.linkFirst(entry);
                    entry.pathString = null;
                    heap.deallocate(entry.getBuffer());
                }
            }
        }
        ///LOG.level(Level.INFO).append("Invalidate(").append(pathString).append(')').commit();
    }

    @Override
    public void rename(String fromPathString, String toPathString) {
        synchronized (lock) {
            invalidate(toPathString);

            CacheEntryImpl entry = entriesByPath.remove(fromPathString, null);
            if (entry != null) {
                entry.pathString = toPathString;
                boolean isNew = entriesByPath.put(toPathString, entry);
                assert isNew; // ensured by invalidate() call above
            }
        }
        ///LOG.level(Level.INFO).append("Rename(").append(fromPathString).append(',').append(toPathString).append(')').commit();
    }

    @Override
    public CacheEntry alloc(long size) {
        if (size < Integer.MAX_VALUE) {
            synchronized (lock) {
                CacheEntryImpl result = takeFirstVacant();
                if (result != null) {
                    assert result.userCounter == 0;
                    assert result.pathString == null;
                    if (heap.allocate((int) size, result.getBuffer())) {
                        return result;
                    } else {
                        vacant.linkFirst(result);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void update(String pathString, CacheEntry cacheEntry) {
        CacheEntryImpl e = (CacheEntryImpl) cacheEntry;

        synchronized(lock) {
            assert e.userCounter == 0;
            assert e.pathString == null;

            e.pathString = pathString;
            e.readTimestamp = System.currentTimeMillis();

            invalidate(pathString);
            entriesByPath.put(pathString, e);
            vacant.linkLast(e);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            for (CacheEntryImpl entry : entriesByPath) {
                if (entry.userCounter == 0) {
                    assert vacant.contains(entry);
                } else {
                    vacant.linkFirst(entry);
                }
                if (entry.pathString != null) {
                    entry.pathString = null;
                    heap.deallocate(entry.getBuffer());
                }
            }
            entriesByPath.clear();
        }
    }

    /** Successful load is indicated by assignment of pathString to given entry */
    private void loadCacheEntry(CacheEntryLoader cacheEntryLoader, CacheEntryImpl result) {
        // If we are here "result" is not shared with any other thread
        assert ! Thread.holdsLock(lock);
        assert result != null && result.pathString == null : "Cache entry has no current content";
        assert result.userCounter == 0 : "Cache entry is already used: " + result;

        final String pathString = cacheEntryLoader.getPathString();
        final long fileLength = cacheEntryLoader.length();
        if (fileLength <= heap.getHeapSize()) {

            if (allocate((int) fileLength, result.getBuffer())) {
                try {
                    cacheEntryLoader.load(result); // Make sure this operation is called outside of any lock
                    result.pathString = pathString;
                    result.readTimestamp = System.currentTimeMillis();
                } catch (IOException e) {
                    LOG.log(LogLevel.ERROR).append("Can't load file \"").append(pathString).append("\" - skipping cache").commit();
                    result.pathString = null;
                    heap.deallocate(result.getBuffer());
                }
            } else {
                //TODO: Out of space in heap: sacrifice more CacheEntries
                if (LOG.isEnabled(LogLevel.DEBUG))
                    LOG.log(LogLevel.DEBUG).append("Cache is out of space!!!").commit();
                assert result.pathString == null;
            }
        } else {
            LOG.log(LogLevel.INFO).append("File is too large to cache: \"").append(pathString).append("\" - skipping cache").commit();
            assert result.pathString == null;
        }
    }

    private boolean allocate(int size, ByteArray result) {
        synchronized (lock) {
            CacheEntryImpl entry = vacant.getFirst();
            do {
                if (heap.allocate(size, result))
                    return true;

                // out of space => sacrifice next vacant entry
                while (entry != null) {
                    if (entry.pathString != null) {
                        entriesByPath.remove(entry.pathString);
                        entry.pathString = null;
                        heap.deallocate(entry.getBuffer());
                        break; // try again
                    }
                    entry = entry.next();
                }
            } while (entry != null);
        }
        return false; // no space available
    }

    /* Format <entry-key>:<usage-counter> */
    private void toString (StringBuilder sb) {
        synchronized (lock) {
            Set<CacheEntryImpl> unprocessed = new HashSet<> (entriesByPath.size());
            entriesByPath.copyTo(unprocessed);

            CacheEntryImpl entry = vacant.getFirst();
            while (entry != null) {
                if (sb.length() > 0)
                    sb.append('\n');
                entry.toString(sb);
                unprocessed.remove(entry);
                entry = entry.next();
            }
            for (CacheEntryImpl e : unprocessed) {
                if (sb.length() > 0)
                    sb.append('\n');
                e.toString(sb);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    String heapToString() {
        return heap.toString();
    }

    /** Buffer with reference counter */
    private static final class CacheEntryImpl extends QuickList.Entry<CacheEntryImpl> implements CacheEntry, Loggable {
        // GuardedBy: CacheEntryImpl.lock
        private int userCounter; // Counts number of current checkouts. Zero implies that this entry in vacant list
        private final ByteArray data = new ByteArray();


        /** Filename of currently loaded buffer or <code>null</code> if this entry is vacant */
        private String pathString; // null implies no data buffer is held
        private long readTimestamp;

        @Override
        public ByteArray getBuffer() {
            return data;
        }

        @Override
        public String getPathString() {
            return pathString;
        }

        @Override
        public void appendTo(AppendableEntry entry) {
            entry.append(userCounter);
        }

        void toString(StringBuilder sb) {
            sb.append(pathString).append(" [").append(userCounter).append(']');
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }
    }

    private void scheduleExpirationJob() {
        //TODO: Configuration
        long expirationJobInterval = TimeUnit.MINUTES.toMillis(1);
        long maxCacheEntryLifetime = TimeUnit.MINUTES.toMillis(15);
        GlobalTimer.INSTANCE.schedule(new EntryExpirationJob(maxCacheEntryLifetime), expirationJobInterval, expirationJobInterval); //TODO: Increase initial interval
    }

    private class EntryExpirationJob extends TimerRunner {

        private final long maxCacheEntryLifetime;
        private final StringBuilder sb = new StringBuilder();
        private final int rootFolderNameLength;

        private EntryExpirationJob(long maxCacheEntryLifetime, String rootFolder) {
            this.maxCacheEntryLifetime = maxCacheEntryLifetime;
            this.rootFolderNameLength = rootFolder.length() + 1;
        }

        private EntryExpirationJob(long maxCacheEntryLifetime) {
            this.maxCacheEntryLifetime = maxCacheEntryLifetime;
            this.rootFolderNameLength = 1;
        }

        @Override
        protected void runInternal() throws Exception {
            int releasedEntriesCount = cleanup(System.currentTimeMillis() - maxCacheEntryLifetime);
            if (releasedEntriesCount > 0)
                LOG.log(LogLevel.DEBUG).append("Cache cleaner released ").append(releasedEntriesCount).append(" entries").commit();
            LOG.log(LogLevel.DEBUG).append("Cache stats: ").append(heap).commit();

            sb.setLength(0);
            synchronized (lock) {
                Iterator<CharSequence> keys = entriesByPath.keyIterator();
                while (keys.hasNext()) {
                    if (sb.length() > 0)
                        sb.append(", ");
                    CharSequence pathString = keys.next();
                    if (pathString.length() > rootFolderNameLength)
                        sb.append(pathString, rootFolderNameLength, pathString.length());
                    else
                        sb.append(pathString);
                }
            }
            LOG.log(LogLevel.DEBUG).append("Cache content: ").append(sb.toString()).commit();
        }
    }

    /** @return number of cache entries released */
    private int cleanup (long readThreashold) {
        int releasedEntriesCount = 0;

        synchronized (lock) {
            CacheEntryImpl entry = vacant.getFirst();
            while (entry != null) {
                if (readThreashold > 0 && entry.readTimestamp < readThreashold)
                    break;

                if (entry.pathString != null) { // this will essentially hide it from users
                    entriesByPath.remove(entry.pathString);

                    entry.pathString = null;
                    heap.deallocate(entry.getBuffer());
                    releasedEntriesCount++;

                }

                entry = entry.next();
            }
        }
        return releasedEntriesCount;
    }

}
