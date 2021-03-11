package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.gflog.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.util.collections.generated.IntegerEnumeration;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.*;

import javax.annotation.Nullable;

/**
 *  
 */
public final class DataReaderImpl 
    extends BlockAccessorBase 
    implements DataReader, BlockProcessor, SliceListener, IntermittentlyAvailableResource
{
    private static final Log LOG = LogFactory.getLog(DataReaderImpl.class);


    private EntityFilter                        currentFilter;
    private boolean                             forward;
    private boolean                             pqIsLoaded = false;    
    private ABLPQ                               pq;
    private long                                currentTimestamp = Long.MAX_VALUE;
    private long                                limit = Long.MAX_VALUE; // limit timestamp

    private boolean                             movePastTSFEnd = true;

    // GuardedBy("this")
    private DataReaderPrefetcher                prefetcher = null;

    // incoming updated blocks
    private final IntegerToObjectHashMap<DataBlock> waiting = new IntegerToObjectHashMap<>();
    private final IntegerEnumeration                e = waiting.keys();

    private volatile Runnable                   listener;

    private final QuickExecutor.QuickTask       notifier;

    public DataReaderImpl (QuickExecutor exe) {
        notifier = new QuickExecutor.QuickTask (exe) {
            @Override
            public void     run () {
                Runnable consistent = listener;

                if (consistent != null) {
                    consistent.run();
                } else {
                    synchronized (DataReaderImpl.this) {
                        DataReaderImpl.this.notifyAll();
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
        store.removeSliceListener(this);

        pq = null;
        currentFilter = null;
        currentTimestamp = Long.MAX_VALUE;

        synchronized (waiting) {
            waiting.clear();
        }

        clearPrefetched();
        
        super.close ();
    }

    @Override
    public synchronized void    open (
        long                        timestamp,
        boolean                     forward,
        EntityFilter                filter
    )
    {
        if (currentTimeSlice != null) {
            throw new IllegalStateException("Attempt to repeatedly open DataReader");
        }
        this.forward = forward;
        currentFilter = filter;
        currentTimestamp = timestamp;
        pq = new ABLPQ (100, forward);
        
        try {
            clearPrefetched();
            currentTimeSlice = store.checkOutTimeSliceForRead (this, timestamp, currentFilter);
            initPrefetcher();
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
        if (currentTimeSlice != null) {
            throw new IllegalStateException("Attempt to repeatedly open DataReader");
        }
        this.forward = true;
        this.pq = new ABLPQ (100, forward);
        this.movePastTSFEnd = movePastTSFEnd;
        
        try {
            clearPrefetched();
            this.currentTimeSlice = store.checkOutTimeSlice (this, tsref);
            initPrefetcher();
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
        }
    }

    public synchronized void                 reopen(long timestamp) {
        EntityFilter filter = currentFilter;
        close();
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

        if (prefetcher != null) {
            // Discard prefetch data
            prefetcher.clearPrefetched();
        }

        pqIsLoaded = false;
    }

    EntityFilter getCurrentFilter() {
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
   
    @Override
    public void                 checkedOut(TimeSlice slice) {
        clearCurrent();
    }

    @Override
    public synchronized boolean readNext (TSMessageConsumer processor) {
        for (;;) {
            if (currentTimeSlice == null)
                return (false);

            if (!pqIsLoaded) {
                currentTimeSlice.processBlocks(currentFilter, this);
                pqIsLoaded = true;
            }

            AccessorBlockLink next = pq.poll();

            if (next != null) {
                int state;

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

            NextResult result = processSliceEnded(processor);
            if (result != null) {
                switch (result) {
                    case OK:
                        return true;
                    case END_OF_CURSOR:
                        return false;
                }
            }
        }
    }

    /**
     * @return null
     */
    @Nullable
    private NextResult processSliceEnded(TSMessageConsumer processor)  {
        clearLinks();

        if (!movePastTSFEnd)
            return NextResult.END_OF_CURSOR;

        TimeSlice nextSlice;
        try {
            nextSlice = getNextTimeSliceToRead();
        } catch (InterruptedException x) {
            throw new UncheckedInterruptedException(x);
        }

        if (nextSlice == null) {
            // release previous time slice
            clearPrefetched();
            return NextResult.END_OF_CURSOR;

            // check limits for the both modes
        } else if (!sliceMatchesLimit(nextSlice)) {
            // This slice is out of limit. No more data.
            release(nextSlice);
            clearPrefetched();
            return NextResult.END_OF_CURSOR;
        }

        currentTimeSlice = nextSlice;
        return null; // null means "continue to read data from currentTimeSlice"
    }

    /**
     * @return true if the time slice matches set limit
     */
    boolean sliceMatchesLimit(TimeSlice timeSlice) {
        if (limit != Long.MAX_VALUE && limit != Long.MIN_VALUE) {
            if ((forward && timeSlice.getStartTimestamp() >= limit) || (!forward && timeSlice.getLimitTimestamp() > limit)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private TimeSlice getNextTimeSliceToRead() throws InterruptedException {
        assert currentTimeSlice != null;
        try {
            if (prefetcher == null) {
                return getNextTimeSliceToReadNoPrefetch(currentTimeSlice, this, false);
            } else {
                return prefetcher.getNextTimeSliceToReadPrefetched(currentTimeSlice);
            }
        } finally {
            // Contract: if currentTimeSlice is not null, then it's checked out to the reader.
            // So if we release slice then we set currentTimeSlice to null
            // Prefetcher not releases prev slice
            boolean prevSliceReleased = prefetcher == null;
            if (!prevSliceReleased) {
                // We release prev slice only after we got next slice because it may require initializing prefetch from the current slice
                release(currentTimeSlice);
            }
            this.currentTimeSlice = null;
        }
    }

    TimeSlice getNextTimeSliceToReadNoPrefetch(TimeSlice currentTimeSlice, DAPrivate accessor, boolean keepPrevCheckout) throws InterruptedException {
        return currentTimeSlice.getStore().getNextTimeSliceToRead (
                accessor,
                currentTimeSlice,
                currentFilter,  // we can miss updates when filter is enabled
                forward,
                keepPrevCheckout
        );
    }

    private void                release(TimeSlice slice) {
        if (slice != null)
            slice.getStore().checkInTimeSlice(this, slice);
    }

    public void                 setAvailabilityListener (Runnable lnr) {
        this.listener = lnr;
    }

    @Override
    public void                 asyncDataInserted(DataBlock db, int dataOffset, int msgLength, long timestamp) {
        // TODO: optimize performance - do not call parent code

        super.asyncDataInserted(db, dataOffset, msgLength, timestamp);
        notifier.submit();
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
        clearPrefetched();
        initPrefetcher();
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

    private void                initPrefetcher() {
        int prefetchSizeFromStore = getPrefetchSizeFromStore();
        boolean prefetchEnabled = prefetchSizeFromStore > 0;
        if (LOG.isEnabled(LogLevel.DEBUG)) {
            LOG.log(LogLevel.DEBUG)
                    .append("Prefetch enabled: ").append(prefetchEnabled)
                    .append(", prefetchSize=").append(prefetchSizeFromStore).append(")")
                    .commit();
        }
        if (prefetchEnabled) {
            if (prefetcher == null)
                prefetcher = new DataReaderPrefetcher(this, prefetchSizeFromStore);
        } else {
            if (prefetcher != null)
                prefetcher.clearPrefetched();
            prefetcher = null;
        }
    }

    private int                 getPrefetchSizeFromStore() {
        // TODO: Better way to check backing file system options without cast?
        if (this.store instanceof TSRoot) {
            TSRoot root = (TSRoot) this.store;
            AbstractFileSystem system = root.getFileSystem();
            return system.getPrefetchSize();
        }
        return 0;
    }

    private synchronized void   clearPrefetched() {
        if (prefetcher != null) {
            prefetcher.clearPrefetched();
        }
    }
}
