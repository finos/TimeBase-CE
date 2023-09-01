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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.util.collections.generated.IntegerEnumeration;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.*;

/**
 *
 */
public final class LiveDataReaderImpl
        extends BlockAccessorBase
        implements DataReader, BlockProcessor, SliceListener, IntermittentlyAvailableResource
{
    private EntityFilter                        currentFilter;
    private boolean                             forward;
    private boolean                             pqIsLoaded = false;
    private ABLPQ                               pq;
    private long                                currentTimestamp = Long.MAX_VALUE;
    private long                                limit = Long.MAX_VALUE; // limit timestamp

    // incoming updated blocks
    private final IntegerToObjectHashMap<DataBlock> waiting = new IntegerToObjectHashMap<>();
    private final IntegerEnumeration                e = waiting.keys();

    private volatile Runnable                   listener;
    private final QuickExecutor.QuickTask       notifier;

    public LiveDataReaderImpl(QuickExecutor exe) {
        notifier =  new QuickExecutor.QuickTask (exe) {
            @Override
            public void     run () {
                Runnable consistent = listener;

                if (consistent != null) {
                    consistent.run();
                } else {
                    synchronized (LiveDataReaderImpl.this) {
                        LiveDataReaderImpl.this.notifyAll();
                    }
                }
            }
        };
    }

    //
    //  DataReader IMPLEMENTATION
    //
    @Override
    public synchronized void    close () {
        closeInternal();
        currentFilter = null;
    }

    @Override
    public void                 park() {
        closeInternal();
    }

    protected void    closeInternal () {
        // already closed
        if (pq == null)
            return;

        store.removeSliceListener(this);

        pq = null;
        currentTimestamp = Long.MAX_VALUE;

        synchronized (waiting) {
            waiting.clear();
        }

        super.close ();
    }

    @Override
    public synchronized void    open (
            long                        timestamp,
            boolean                     forward,
            EntityFilter                filter
    )
    {
        if (currentTimeSlice != null)
            throw new IllegalStateException("DataReader is already opened.");

        this.forward = forward;
        currentFilter = filter;
        currentTimestamp = timestamp;
        pq = new ABLPQ (100, forward);

        try {
            store.addSliceListener(this);

            currentTimeSlice = store.checkOutTimeSliceForRead (this, timestamp, currentFilter);
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException (x);
        }
    }

    @Override
    public synchronized void    open (
            TSRef                       tsref,
            long                        timestamp,
            boolean                     movePastTSFEnd,
            EntityFilter                filter
    )
    {
        assert movePastTSFEnd;

        if (currentTimeSlice != null)
            throw new IllegalStateException("DataReader is already opened.");

        this.forward = true;
        this.pq = new ABLPQ (100, forward);

        try {
            this.currentTimeSlice = store.checkOutTimeSlice (this, tsref);
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException (x);
        }

        this.currentFilter = filter;
        this.currentTimestamp = timestamp;
    }

    public void                 process (DataBlock block, long timestamp) {

        AccessorBlockLink link = find(block.getEntity());

        if (link == null) {
            link = getBlockLink(block.getEntity(), block);
            if (forward)
                link.forward(timestamp);
            else
                link.forwardToLast(timestamp);
        }

        if (!link.queued) {
            if (forward && !link.atEnd()) {
                pq.offer(link);
            } else if (!forward) {
                pq.offer(link);
            }
        } else {
            assert pq.contains(link);
        }
    }

    public synchronized void                 reopen(long timestamp) {
        EntityFilter filter = currentFilter;
        closeInternal();
        open(timestamp, forward, filter);
    }

    @Override
    public synchronized void                setFilter(EntityFilter filter) {
        currentFilter = filter;

        if (currentFilter.restrictAll()) {
            if (pq != null)
                pq.clear();

            clearLinks();
        } else {

            // cleanup queue
        }

        pqIsLoaded = false;
    }

    EntityFilter        getCurrentFilter() {
        return currentFilter;
    }

    @Override
    public DataBlock                        allocate() {
        return new DataBlock();
    }

    @Override
    public void                             process(DataBlock block) {
//        long time = newSubscriptionTime;
//
//        if (time == TimeConstants.USE_CURRENT_TIME)
//            time = TimeKeeper.currentTime;
//        else if (time == TimeConstants.USE_CURSOR_TIME)
//            time = currentTimestamp;

        long from = currentFilter.acceptFrom(block.getEntity());
        process(block, from);
    }

    @Override
    public void complete() {
        // do nothing
    }

    @Override
    public void                 setLimitTimestamp(long timestamp) {
        limit = timestamp;
    }

    @Override
    public long                 getStartTimestamp() {
        return currentTimeSlice != null ? currentTimeSlice.getStartTimestamp() : currentTimestamp;
    }

    @Override
    public long                 getEndTimestamp() {
        return currentTimeSlice != null ? currentTimeSlice.getLimitTimestamp() : currentTimestamp;
    }

    private boolean             endOfHistoricalData (TSMessageConsumer processor) {
        if (processor.processRealTime(currentTimestamp))
            return true;

//        if (currentTimeSlice != null && currentTimeSlice.isCheckoutOnly(this)) {
//            release(currentTimeSlice);
//            currentTimeSlice = null;
//        }

        if (listener != null)
            throw UnavailableResourceException.INSTANCE;

        try {
            wait();
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }

        return false;
    }

    @Override
    public void                 checkedOut(TimeSlice slice) {
        clearCurrent();
    }

    @Override
    public synchronized boolean readNext (TSMessageConsumer processor) {
        for (;;) {

            if (currentTimeSlice == null) {

                if (pq == null) // is closed?
                    return false;

                try {
                    //  filter should be empty to not miss any symbol updates,
                    //    for example we subscribed to "A", but next slice contains only "B" at the moment
                    currentTimeSlice = store.checkOutTimeSliceForRead(this, currentTimestamp, null);
                    pqIsLoaded = false;
                } catch (InterruptedException e) {
                    throw new UncheckedInterruptedException (e);
                }
            }

            if (currentTimeSlice == null) {
                if (endOfHistoricalData(processor))
                    return true;
                else
                    continue;
            }

            try {
                if (!pqIsLoaded) {
                    currentTimeSlice.processBlocks(currentFilter, this);
                    pqIsLoaded = true;
                }
            } catch (Exception ex) {
                // in case of race conditions currentTimeSlice can be deleted here
                DataReaderImpl.LOG.warn("Skipping error while reading slice: %s").with(ex);
            }

            // process waiting blocks (for live mode)
            for (;;) {
                DataBlock block = pollWaiting();

                if (block == null)
                    break;

                if (currentFilter.accept(block.getEntity()))
                    process(block);
            }

            AccessorBlockLink next = pq.poll();

            if (next != null) {
                int state;

                // TSFile already closed
                if (!next.isActive())
                    continue;

                if (forward)
                    state = next.readMessageForward(processor);
                else
                    state = next.readMessageReverse(processor);

                if (NextState.hasMore(state)) {
                    //if (!pqIsLoaded && currentFilter.accept(next.getEntity()))
                    pq.offer(next);
                }

                if (!NextState.hasCurrent(state))
                    continue;

                return true;
            }

            TimeSlice nextSlice;
            try {
                nextSlice = currentTimeSlice.getStore().getNextTimeSliceToRead (
                        this,
                        currentTimeSlice,
                        null,
                        forward,
                        true
                );
            } catch (InterruptedException x) {
                throw new UncheckedInterruptedException(x);
            }

            if (nextSlice == null) {
                if (endOfHistoricalData(processor))
                    return true;
                else
                    continue;
            } else {
                release(currentTimeSlice);

                if (limit != Long.MAX_VALUE && limit != Long.MIN_VALUE) {
                    // check limits for the both modes
                    if ((forward && nextSlice.getStartTimestamp() >= limit) || (!forward && nextSlice.getLimitTimestamp() > limit)) {
                        release(nextSlice);
                        return (false);
                    }
                }
            }

            currentTimeSlice = nextSlice;
        }
    }

    private void                release(TimeSlice slice) {
        if (slice != null) {
            clearCurrent();
            slice.getStore().checkInTimeSlice(this, slice);
            clearBuffers();
        }
    }

    public void                 setAvailabilityListener (Runnable lnr) {
        this.listener = lnr;
    }

    @Override
    public void                 asyncDataInserted(DataBlock db, int dataOffset, int msgLength, long timestamp) {
        // TODO: optimize performance - do not call parent code

        super.asyncDataInserted(db, dataOffset, msgLength, timestamp);

        offerWaiting(db);

        notifier.submit();
    }

    private void                offerWaiting(DataBlock db) {
        synchronized (waiting) {
            waiting.put(db.getEntity(), db);
        }
    }

    private DataBlock           pollWaiting() {
        synchronized (waiting) {
            e.reset();

            if (e.hasMoreElements())
                return waiting.remove(e.nextIntElement(), null);
        }

        return null;
    }

    private void              clearCurrent() {

        synchronized (this) {
            pqIsLoaded = false;
        }

        synchronized (waiting) {
            waiting.clear();
        }

        clearLinks();
    }

    @Override
    public synchronized void associate(TimeSlice slice) {
        super.associate(slice);
    }

    // SliceListener IMPLEMENTATION

    @Override
    public void                 checkoutForInsert(TimeSlice slice) {
        notifier.submit();
    }

    @Override
    public void                 checkoutForRead(TimeSlice slice) {

    }

    //
    //  DAPrivate IMPLEMENTATION
    //
    @Override
    public long                 getCurrentTimestamp () {
        return (currentTimestamp);
    }
}