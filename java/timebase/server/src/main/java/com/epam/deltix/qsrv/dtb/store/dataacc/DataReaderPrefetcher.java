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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prefetches timeslice data for {@link DataReaderImpl} using dedicated thread pool.
 * This may significantly increase performance if data is located at remote file system (like AzureDL).
 *
 * Implementation detail: all access to instance of this class is supposed to be guarded by synchronization on {@link DataReaderImpl}.
 *
 * This implementation not starts prefetching till reader tries to access the <b>second</b> slice of data (first call to {@link #getNextTimeSliceToReadPrefetched}.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DataReaderPrefetcher {
    private static final Log LOG = LogFactory.getLog(DataReaderPrefetcher.class);

    // Queue with future slices.
    // Contract: all slices in queue are checked out to "prefetchAccessor".
    // Contract: whoever removes entries from the queue is responsible to check them in.
    private final Deque<PrefetchHolder> prefetchSliceQueue = new ArrayDeque<>();

    // True if we started to prefetch and initialized nextTimeSliceToPrefetch.
    private boolean started = false;

    // Has null value if we not started yet or already completed entire stream.
    // Contract: if not null then this slice is checked out to "prefetchAccessor".
    // Contract: whoever sets this field to null is responsible to check it in.
    protected TimeSlice nextTimeSliceToPrefetch = null;

    // Is used to tell running prefetch tasks that they should gracefully stop.
    private AtomicBoolean prefetchCancelFlag = null;

    private final DAPrivate prefetchAccessor; // Stub data accessor that is used to checkout time slices before transferring them to reader
    private final DataReaderImpl reader; // Reader that uses this prefetcher

    private final int maxPrefetchSize; // Maximum number for prefetched slices for this client.

    private static final ExecutorService prefetchExecutor = createPrefetchThreadPoolExecutor();

    // Global number of permitted prefetched slices (across all DataReader-s).
    private static final int totalPrefetchMax = Integer.getInteger("TimeBase.dataReader.prefetch.shared.totalPrefetchMax", 20);
    // Current number of prefetched slices (across all DataReader-s).
    private static final AtomicInteger prefetchLeases = new AtomicInteger(0);
    private static final AtomicInteger totalSlicesInAllQueues = new AtomicInteger(0);

    @Nonnull
    private static ExecutorService createPrefetchThreadPoolExecutor() {
        int corePoolSize = Integer.getInteger("TimeBase.dataReader.prefetch.shared.corePoolSize", 10); // Number of threads to use by default.
        int maximumPoolSize = Integer.getInteger("TimeBase.dataReader.prefetch.shared.maximumPoolSize", 50); // Max number of threads to run (in case of full queue).
        int keepAliveSeconds = Integer.getInteger("TimeBase.dataReader.prefetch.shared.keepAliveSeconds", 60); // Keep alive time for threads.
        int queueSize = Integer.getInteger("TimeBase.dataReader.prefetch.shared.queueSize", 100); // Task queue size.

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveSeconds, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("datareader-prefetch-%d").setDaemon(true).build()
        );
        executor.allowCoreThreadTimeOut(true);

        return executor;
    }

    public DataReaderPrefetcher(DataReaderImpl reader, int prefetchSize) {
        this.reader = reader;
        this.maxPrefetchSize = prefetchSize;
        this.prefetchAccessor = new PrefetchAccessor(this.reader);
    }


    private void startFromSlice(TimeSlice timeSliceCheckedOutToReader) throws InterruptedException {
        assert nextTimeSliceToPrefetch == null;
        TimeSlice prevSliceCheckedOutToPrefetcher = reader.store.checkOutTimeSlice(prefetchAccessor, timeSliceCheckedOutToReader);
        nextTimeSliceToPrefetch = reader.getNextTimeSliceToReadNoPrefetch(prevSliceCheckedOutToPrefetcher, prefetchAccessor, false);
    }

    /**
     * @return time slice (checked out to reader) or null
     * @param currentTimeSlice slice to start from (not included)
     */
    @Nullable
    public TimeSlice getNextTimeSliceToReadPrefetched(TimeSlice currentTimeSlice) throws InterruptedException {
        if (!started) {
            startFromSlice(currentTimeSlice);
            started = true;
        }
        addSlicesToPrefetchQueue();
        if (LOG.isEnabled(LogLevel.TRACE)) {
            LOG.trace()
                    .append("totalSlicesInAllQueues=").append(totalSlicesInAllQueues.get())
                    .append(" queuedCount=").append(prefetchSliceQueue.size())
                    .append(" prefetcher=").append(this.toString())
                    .commit();
        }

        if (prefetchSliceQueue.isEmpty()) {
            // No more slices left. This mean end of stream.
            return null;
        }
        PrefetchHolder prefetchHolder = prefetchSliceQueue.poll();
        totalSlicesInAllQueues.decrementAndGet();
        try {
            TimeSlice prefetchedSlice;
            try {
                if (prefetchHolder.sliceFuture != null) {
                    // Wait for prefetch to complete
                    prefetchedSlice = prefetchHolder.sliceFuture.get();
                    assert prefetchedSlice == prefetchHolder.slice;
                    decrementUsedPrefetchSlots();
                } else {
                    // No prefetch for this slice. Just use it.
                    prefetchedSlice = prefetchHolder.slice;
                }
            } catch (CancellationException e) {
                // Prefetch was aborted. Just use this slice.
                // Note: this case probably means that there is attempt to reads from DataReader after it was closed.
                prefetchedSlice = prefetchHolder.slice;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                }
                throw new RuntimeException(e);
            }

            // Change accessor of the slice from "prefetchAccessor" to "reader"
            return reader.store.checkOutTimeSlice(reader, prefetchedSlice);
        } finally {
            releasePrefetchedSlice(prefetchHolder.slice);
        }
    }

    private void addSlicesToPrefetchQueue() throws InterruptedException {
        assert started;
        if (nextTimeSliceToPrefetch == null) {
            // We reached end of stream (by prefetch). No more slices to prefetch.
            return;
        }

        int slicesToAdd = chooseNumberOfSlicesToAdd();
        int slicesAdded = 0;

        if (slicesToAdd > 0) {
            final AtomicBoolean cancelFlag = initAndGetCancelFlag();
            EntityFilter filter = reader.getCurrentFilter();

            while (slicesToAdd > 0 && nextTimeSliceToPrefetch != null) {
                boolean hasPrefetchLease = incrementUsedPrefetchSlots();
                if (!hasPrefetchLease) {
                    if (slicesAdded > 0 || !prefetchSliceQueue.isEmpty()) {
                        // We already have at least one new slice to work on. Do not add new slices for now.
                        break;
                    }
                }

                final TimeSlice next = nextTimeSliceToPrefetch;
                assert next != null;
                nextTimeSliceToPrefetch = reader.getNextTimeSliceToReadNoPrefetch(nextTimeSliceToPrefetch, prefetchAccessor, true);
                if (!reader.sliceMatchesLimit(next)) {
                    // We reached end or hit the limit. No more data.
                    releasePrefetchedSlice(nextTimeSliceToPrefetch);
                    nextTimeSliceToPrefetch = null;
                    break;
                }

                Future<TimeSlice> prefetchTaskFuture;
                // Add prefetch task
                try {
                    prefetchTaskFuture = prefetchExecutor.submit(new Callable<TimeSlice>() {
                        @Override
                        public TimeSlice call() {
                            if (!cancelFlag.get()) {
                                //System.out.println("Starting to read slice from thread: " + Thread.currentThread().getName());
                                next.processBlocks(filter, null);
                            }

                            return next;
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // Out of prefetching threads.
                    // Let reader to fetch data in his own thread when he reach this block.
                    prefetchTaskFuture = null;
                }

                prefetchSliceQueue.add(new PrefetchHolder(next, prefetchTaskFuture));
                totalSlicesInAllQueues.incrementAndGet();

                slicesToAdd--;
                slicesAdded++;

                if (prefetchTaskFuture == null) {
                    // That means the prefetch task was not created. Stop for now.
                    // No point in trying to prefetch till there are free threads or slots.
                    break;
                }
            }
        }
    }

    @Nonnull
    private AtomicBoolean initAndGetCancelFlag() {
        if (this.prefetchCancelFlag == null) {
            this.prefetchCancelFlag = new AtomicBoolean(false);
        }
        return this.prefetchCancelFlag;
    }

    /**
     * @return true if successfully got lease for new prefetch slot
     */
    private boolean incrementUsedPrefetchSlots() {
        int newValue = prefetchLeases.incrementAndGet();
        if (newValue > totalPrefetchMax) {
            // We got above the limit. Rollback.
            prefetchLeases.decrementAndGet();
            return false;
        } else {
            return true;
        }
    }

    private void decrementUsedPrefetchSlots() {
        prefetchLeases.decrementAndGet();
    }

    /**
     * Determines how many slices should be added to prefetch queue at current iteration.
     *
     * @return non-negative integer
     */
    private int chooseNumberOfSlicesToAdd() {
        int queuedCount = prefetchSliceQueue.size();
        int completedCount = 0;
        int completedHeadCount = 0;
        boolean seenIncomplete = false;
        for (PrefetchHolder prefetchHolder : prefetchSliceQueue) {
            if (prefetchHolder.sliceFuture != null && prefetchHolder.sliceFuture.isDone()) {
                completedCount += 1;
                if (!seenIncomplete) {
                    completedHeadCount += 1;
                }
            } else {
                seenIncomplete = true;
            }
        }
        int incompleteDownloads = queuedCount - completedCount;
        int freeHardPrefetchSlots = maxPrefetchSize - queuedCount;
        //int freeSoftPrefetchSlots = targetPrefetchSize - queuedCount;
        //int freePrefetchThreads = maxIncompleteTasksPerReader - incompleteDownloads;

        assert freeHardPrefetchSlots >= 1; // We an get to this method only after we processed a block so there always at least one slot at buffer.


        int slicesToAdd = freeHardPrefetchSlots;
        if (completedHeadCount > 0) {
            // Limit number of new tasks per iteration if we have prepared blocks
            // Note: there a chance that actually we have some non-prefetched blocks. So we should not make this number too low.
            slicesToAdd = Math.min(slicesToAdd, 8);
        }

        // Get rid of negative values
        slicesToAdd = Math.max(slicesToAdd, 0);

        if (slicesToAdd < 0 && queuedCount == 0) {
            // Special case: we not plan to add slices but queue is empty. Empty queue is treated as finished stream. And that is not what we want.
            // So we ensure that at least one slice will be added
            // Note: that should not happen normally:
            slicesToAdd = 1;
        }
        assert !(queuedCount == 0 && slicesToAdd == 0);

        if (LOG.isEnabled(LogLevel.DEBUG)) {

            LOG.debug()
                    .append("queuedCount=").append(queuedCount)
                    .append(" completedCount=").append(completedCount)
                    .append(" completedHeadCount=").append(completedHeadCount)
                    .append(" incompleteDownloads=").append(incompleteDownloads)
                    .append(" freePrefetchSlots=").append(freeHardPrefetchSlots)
                    .append(" slicesToAdd=").append(slicesToAdd)
                    .commit();
        }

        return slicesToAdd;
    }

    public void clearPrefetched() {
        assert !(!started && prefetchCancelFlag != null);
        assert !(!started && nextTimeSliceToPrefetch != null);
        if (nextTimeSliceToPrefetch != null) {
            releasePrefetchedSlice(nextTimeSliceToPrefetch);
            nextTimeSliceToPrefetch = null;
        }

        if (prefetchCancelFlag != null) {
            // Prevent not stated prefetch tasks from running.
            prefetchCancelFlag.set(true);
            prefetchCancelFlag = null;
        }

        started = false;

        try {
            PrefetchHolder prefetchHolder;
            while ((prefetchHolder = prefetchSliceQueue.peekFirst()) != null) {
                TimeSlice slice;
                if (prefetchHolder.sliceFuture != null) {
                    // We have to await for tasks to complete because we must check them in.
                    // This will block till download for a block is completed.
                    slice = prefetchHolder.sliceFuture.get();
                } else {
                    slice = prefetchHolder.slice;
                }
                releasePrefetchedSlice(slice);
                prefetchSliceQueue.poll();
                totalSlicesInAllQueues.decrementAndGet();
            }
            assert prefetchSliceQueue.isEmpty();
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (!prefetchSliceQueue.isEmpty()) {
                // We failed to complete tasks and check-in slices try to cancel tasks
                for (PrefetchHolder prefetchHolder : prefetchSliceQueue) {
                    if (prefetchHolder.sliceFuture != null) {
                        if (prefetchHolder.sliceFuture.cancel(false) || prefetchHolder.sliceFuture.isDone()) {
                            // Cancellation was successful. Now we can check-in the slice.
                            releasePrefetchedSlice(prefetchHolder.slice);
                        }
                    }
                }
                totalSlicesInAllQueues.addAndGet(-prefetchSliceQueue.size());
                prefetchSliceQueue.clear();
            }
        }

    }

    private void releasePrefetchedSlice(TimeSlice prefetchedSlice) {
        prefetchedSlice.getStore().checkInTimeSlice(prefetchAccessor, prefetchedSlice);
    }

    /**
     * Stub class that stubs {@link DAPrivate} to use it as TimeSlice owner for checkouts.
     */
    private static final class PrefetchAccessor implements DAPrivate {
        private final String parentName;

        public PrefetchAccessor(DataReaderImpl dataReader) {
            this.parentName = dataReader.toString();
        }

        @Override
        public long getCurrentTimestamp() {
            return 0;
        }

        @Override
        public void asyncDataInserted(DataBlock db, int dataOffset, int msgLength, long timestamp) {

        }

        @Override
        public void asyncDataDropped(DataBlock db, int dataOffset, int msgLength, long timestamp) {

        }

        @Override
        public void checkedOut(TimeSlice slice) {

        }

        @Override
        public String toString() {
            return super.toString() + " for " + parentName;
        }
    }

    private static final class PrefetchHolder {
        final TimeSlice slice; // Time slice
        final Future<TimeSlice> sliceFuture; // Future that completes when slice prefetch completes. Result must be same as in "slice" field

        private PrefetchHolder(TimeSlice slice, @Nullable Future<TimeSlice> sliceFuture) {
            this.slice = slice;
            this.sliceFuture = sliceFuture;
        }
    }
}
