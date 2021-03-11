package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.*;
import com.epam.deltix.qsrv.dtb.store.dataacc.*;
import com.epam.deltix.qsrv.dtb.store.pub.AbstractSingleEntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.ListEntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.SingleEntityFilter;
import com.epam.deltix.qsrv.hf.blocks.ObjectPool;
import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectHashSet;

import java.io.*;

import static com.epam.deltix.qsrv.dtb.store.impl.TSFState.*;

/**
 *  Implementation of a time slice file. 
 *  All variables of this class are guarded by "this".
 */
final class TSFile extends TSFolderEntry implements TimeSlice {

    //private static final boolean            DEBUG_VERIFY_FILE_AFTER_STORE = false;

    private static final ObjectPool<FileInput> inputs = new ObjectPool<FileInput>(10, 100) {
        @Override
        protected FileInput newItem() {
            return new FileInput();
        }
    };

    static final int                        FILE_FORMAT_VERSION = 3;

    private static final int                FILE_HEADER_SIZE = 14;

    private static int                      computeIndexBlockSize (
        boolean  compressed,
        int numEntities,
        int formatVersion
    )
    {
        return (numEntities * (compressed ? 28 : DataBlockStub.SIZE_ON_DISK) + (formatVersion >= 3 ? FILE_HEADER_SIZE : 10));
    }

    private static final int                INITIAL_SIZE = FILE_HEADER_SIZE;

    private enum ExpansionStrategy {
        MOVE_BACKWARD,
        MOVE_FORWARD,
        SPLIT
    }

    private TSFState                            state = null;

    // Indicates that file belongs to the write queue. Guarded by PDSImpl.
    // Added to resolve race condition when file processed by WriterThread and does not belongs to queue.

    volatile boolean                            queued;

    /**
     *  Does this file exists on disk.
     *  Guarded by root structure lock: it is checked only by the holder of
     *  an exclusive structure lock, and modified only by the holder of a
     *  shared structure lock.
     */
    private boolean                             isNew;
    private int                                 formatVersion;
    private boolean                             compressedOnDisk;
    private byte                                compressionCode;
    private int                                 uncompressedSize = -1;

    private ObjectArrayList <DataBlockInfo>     dbs = null;

    private ObjectHashSet <DAPrivate>           checkouts = null;

    /**
     *  Inclusive end of the range of all data in this TSF.
     */
    private long                                lastTimestamp;

    /*
        Timestamp of the next slice
     */
    long                                        limitTimestamp = Long.MAX_VALUE;

    private final ThreadLocal<SingleEntityFilter>     sef = new ThreadLocal<>();

    //    @GuardedBy("this")
    private BlockDecompressor                   decompressor;

    TSFile (TSFolder parent, long version, int id, long startTimestamp, boolean isNew) {
        super (parent, id, startTimestamp);

        this.isNew = isNew;
        this.version = version;
    }

    //
    //  TimeSlice IMPLEMENTATION
    //     
    @Override
    public TimeSliceStore           getStore () {
        return (root);
    }

    @Override
    public void                     checkIn (DAPrivate accessor) {
        if (!checkedInBy (accessor))
            return;

        root.acquireSharedLock ();

        try {
            TreeOps.unuse (this);
        } finally {
            root.releaseSharedLock ();
        }
    }

    @Override
    public long                 getLimitTimestamp() {
        return limitTimestamp;
    }

    // fix for split
    void                            checkIn () {
        synchronized (this) {
            if (checkouts == null || checkouts.isEmpty()) {
                PDSImpl cache = root.getCache();

                if (state == TSFState.DIRTY_CHECKED_OUT) {
                    //PDSImpl.LOGGER.log (Level.FINE, this + ": " + state + "->" + TSFState.DIRTY_QUEUED_FOR_WRITE);

                    state = DIRTY_QUEUED_FOR_WRITE;
                    addToWriteQueue(cache);

                } else if (state == DIRTY_QUEUED_FOR_WRITE) {
                    LOGGER.warn().append(this).append(": ").append(state).append(" -> ").append(TSFState.CLEAN_CACHED).commit();
                } else {
                    //LOGGER.log (Level.FINE, this + ": " + state + "->" + TSFState.CLEAN_CACHED);

                    state = TSFState.CLEAN_CACHED;
                    cache.fileWasCheckedInClean(this);
                }
            }
        }

        root.acquireSharedLock ();

        try {
            TreeOps.unuse (this);
        } finally {
            root.releaseSharedLock ();
        }
    }

    public synchronized TSFState    getState() {
        return state;
    }

    @Override
    public synchronized void        blockGoesDirty (DataBlock db) {
        //PDSImpl.LOGGER.log (Level.FINE, this + ": " + state + "->" + TSFState.DIRTY_CHECKED_OUT);

        assert state != DIRTY_QUEUED_FOR_WRITE;

        state = TSFState.DIRTY_CHECKED_OUT;
    }

    DAPrivate []    checkoutsSnapshot = null;

    @Override
    public void        dataInserted (
        DAPrivate accessor,
        DataBlock db,
        int dataOffset,
        int msgLength,
        long timestamp
    )
    {
        DAPrivate[] snapshot = getCheckouts();

        boolean checkedOut = false;

        for (int ii = 0, size = snapshot.length; ii < size; ii++) {
            DAPrivate   acc = snapshot [ii];

            if (accessor == acc)
                checkedOut = true;
            else if (acc != null)
                acc.asyncDataInserted(db, dataOffset, msgLength, timestamp);
        }

        //assert checkedOut;
    }

    @Override
    public void             dataDropped(DAPrivate accessor, DataBlock db, int dataOffset, int length, long timestamp) {
        assert isCheckedOutTo (accessor);

        DAPrivate[] snapshot = getCheckouts();

        for (int ii = 0, size = snapshot.length; ii < size; ii++) {
            DAPrivate   acc = snapshot [ii];

            if (acc != null && acc != accessor)
                acc.asyncDataDropped(db, dataOffset, length, timestamp);
        }
    }

    public IntegerArrayList getEntities() throws IOException {
        ensureIndexAndDataLoaded(null, null);

        synchronized (this) {
            IntegerArrayList list = new IntegerArrayList();

            int numEntities = dbs.size();
            for (int pos = 0; pos < numEntities; pos++) {
                DataBlockInfo info = dbs.getObjectNoRangeCheck(pos);
                if (info.getDataLength() > 0)
                    list.add(info.getEntity());
            }

            return list;
        }
    }

    DAPrivate[]             getCheckouts() {
        return checkoutsSnapshot;
    }

    boolean                 hasDataFor (EntityFilter filter)
        throws IOException
    {
        ensureIndexAndDataLoaded (filter, null);

        synchronized (this) {

            if (filter instanceof AbstractSingleEntityFilter)
                return (find(((AbstractSingleEntityFilter) filter).getSingleEntity()) >= 0);

            int numEntities = dbs.size();
            for (int pos = 0; pos < numEntities; pos++) {
                DataBlockInfo test = dbs.getObjectNoRangeCheck(pos);

                if (filter.accept(test.getEntity()))
                    return (test.getDataLength() > 0);
            }
        }

        return (false);
    }

    boolean                 hasDataFor (int entity)
            throws IOException
    {
        ensureIndexAndDataLoaded (null, null);

        synchronized (this) {
            int pos = find(entity);

            return pos >= 0 && dbs.getObjectNoRangeCheck(pos).getDataLength() > 0;
        }
    }

    @Override
    public DataBlock                getBlock (int entity, boolean create) {
        DataBlock db;

        ensureIndexAndDataLoadedCatchIOX (single (entity), null);

        boolean             justCreated = false;

        synchronized (this) {

            db = getOrLoadBlock (entity);

            if (db == null) {
                if (!create)
                    return (null);

                int                 pos = find (entity);

                assert pos < 0;

                pos = -pos - 1;

                db = new DataBlock ();
                db.initNew (this, entity);
                dbs.add (pos, db);

//                System.out.print("created block: " + db);
//                new Exception().printStackTrace(System.out);

                uncompressedSize += DataBlockStub.SIZE_ON_DISK;

                justCreated = true;
            } else if (create) {
                // usually called by data writer
                justCreated = db.getDataLength() == 0;
            }
        }

        if (justCreated) {
            root.acquireSharedLock();

            try {
                TreeOps.propagateDataAddition (this, entity);
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                root.releaseSharedLock();
            }
        }

        return (db);
    }

    @Override
    public void        processBlocks (EntityFilter filter, BlockProcessor bp) {
        if (filter == null)
            filter = EntityFilter.ALL;

        ensureIndexAndDataLoadedCatchIOX (filter, bp);
    }

    @Override
    public void                     insertNotify (
        EntityFilter                    loadHint,
        DAPrivate                       accessor,
        long                            timestamp,
        int                             addlLength
    )
        throws SwitchTimeSliceException
    {
        if (!isIndexLoaded())
            ensureIndexAndDataLoadedCatchIOX (loadHint, null);

        ExpansionStrategy   strategy = checkInsert (timestamp, addlLength);

        if (strategy == null)
            return;

        root.acquireWriteLock();

        try {
            strategy = checkInsert (timestamp, addlLength);

            if (strategy == null)
                return;

            switch (strategy) {
                case MOVE_FORWARD: {
                    TSFile next;

                    try {
                        next = root.getNextTimeSliceToWrite(timestamp, accessor, this);
                        next.ensureIndexAndDataLoaded(EntityFilter.ALL, null);
                    } catch (IOException iox) {
                        throw new com.epam.deltix.util.io.UncheckedIOException(iox);
                    }

                    throw new SwitchTimeSliceException(next);
                }

                case MOVE_BACKWARD: {
                    TSFile previous;

                    try {
                        previous = root.getPreviousTimeSliceToWrite(timestamp, accessor, this);
                        previous.ensureIndexAndDataLoaded(EntityFilter.ALL, null);
                    } catch (IOException iox) {
                        throw new com.epam.deltix.util.io.UncheckedIOException(iox);
                    }

                    throw new SwitchTimeSliceException(previous);
                }

                case SPLIT:
                    TSFile next;
                    try {
                        next = root.split(timestamp, this, accessor);
                    } catch (IOException iox) {
                        throw new com.epam.deltix.util.io.UncheckedIOException(iox);
                    }
                    if (next != this)
                        throw new SwitchTimeSliceException(next);

                    break;

                default:
                    throw new IllegalStateException("Unknown strategy: " + strategy);
            }
        } finally {
            root.releaseWriteLock();
        }
    }

    private  void                    invalidateTime() {
        assert Thread.holdsLock(this);

        int entities = dbs.size();

        long time0 = Long.MAX_VALUE;
        long time1 = Long.MIN_VALUE;

        for (int pos = 0; pos < entities; pos++) {
            DataBlockInfo info = dbs.getObjectNoRangeCheck(pos);
            if (info.getDataLength() > 0) {
                time0 = Math.min(time0, info.getStartTime());
                time1 = Math.max(time1, info.getEndTime());
            }
        }

        setLastTimestamp(time1);
        updateStartTimestamp(time0 != Long.MAX_VALUE ? time0 : Long.MIN_VALUE);
    }

    public boolean                    isEmpty() {
        int entities = dbs.size();
        long dataSize = 0;

        for (int pos = 0; pos < entities; pos++) {
            DataBlockInfo info = dbs.getObjectNoRangeCheck(pos);
            dataSize += info.getDataLength();
        }

        return dataSize == 0;
    }

    void                            split(final long nstime, final TSFile next, final DataAccessorBase accessor) throws IOException {

        assertCheckedOutTo(accessor);

        ensureIndexAndDataLoadedCatchIOX(EntityFilter.ALL, new AbstractBlockProcessor() {
            long lastTime = Long.MIN_VALUE;
            long firstTime = Long.MAX_VALUE;

            @Override
            public void process(DataBlock block) {
                int entity = block.getEntity();

                if (block.getDataLength() > 0) {
                    AccessorBlockLink link = accessor.getBlockLink(entity, block);

                    // check that we can actually split this block
                    if (link.hasMoreTime(nstime)) {
                        DataBlock to = next.getBlock(entity, true);

                        long free = link.split(nstime, to);
                        assert free > 0;

                        uncompressedSize -= free;
                        next.uncompressedSize += free;

                        if (link.isEmpty()) {
                            TreeOps.propagateDataDeletion(TSFile.this, block.getEntity());
                        }
                    }

                    if (!link.isEmpty()) {
                        lastTime = Math.max(lastTime, link.getEndTime());
                        firstTime = Math.min(firstTime, link.getStartTime());
                    }
                }
            }

            @Override
            public void complete() {
                limitTimestamp = nstime;
                setLastTimestamp(lastTime);
                updateStartTimestamp(firstTime != Long.MAX_VALUE ? firstTime : Long.MIN_VALUE);
            }
        });

        // TODO: correct split
        synchronized (next){
            next.invalidateTime();
        }
    }

    long                            getSplitTime(final DataAccessorBase accessor) {

        assertCheckedOutTo(accessor);

        // make histogram distribution (file size to time)

        long startTime = getStartTimestamp();
        if (startTime == Long.MIN_VALUE)
            startTime = getFromTimestamp();

        final long[] times = new long[9];

        final double range = (lastTimestamp - startTime);
        long cut = (long) (range / (times.length + 1));

        times[0] = startTime + cut;
        for (int i = 1; i < times.length; i++)
            times[i] = times[i - 1] + cut;

        final long[] sizes = new long[times.length];

        ensureIndexAndDataLoadedCatchIOX(EntityFilter.ALL, new AbstractBlockProcessor() {

            @Override
            public void process(DataBlock block) {
                AccessorBlockLink link = accessor.getBlockLink(block.getEntity(), block);

                if (!link.isEmpty()) {
                    for (int i = 0; i < times.length; i++)
                        sizes[i] += link.find(times[i]);
                }
            }

            @Override
            public void complete() {
            }
        });

        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] > uncompressedSize / 2)
                return times[i];
        }
        return times[times.length - 1];
    }

    @Override
    public  void       truncate(final long timestamp, int entity, final DataAccessorBase accessor) {

        assertCheckedOutTo(accessor);

        root.acquireSharedLock();

        try {
            //  make sure that we have index loaded and entity data
            //  otherwise we break lock ordering (structure lock -> TSFile)

            ensureIndexAndDataLoadedCatchIOX(new SingleEntityFilter(entity), new AbstractBlockProcessor() {

                @Override
                public void process(DataBlock block) {

                    // synchronized by TSFile
                    AccessorBlockLink link = accessor.getBlockLink(block.getEntity(), block);

                    if (!link.isEmpty()) {
                        uncompressedSize -= link.truncate(timestamp);

                        if (link.isEmpty())
                            TreeOps.propagateDataDeletion(TSFile.this, block.getEntity());
                        else if (lastTimestamp < link.getEndTime())
                            setLastTimestamp(link.getEndTime());
                    }
                }

                @Override
                public void complete() {
                }
            });
        } finally {
            root.releaseSharedLock();
        }
    }

    private void        setLastTimestamp(long timestamp) {
        assert timestamp != Long.MAX_VALUE;

        lastTimestamp = timestamp;
    }

    public void       cut(final long[] range, final int[] entities, final DataAccessorBase accessor) {
        assertCheckedOutTo(accessor);

        ensureIndexAndDataLoadedCatchIOX(new ListEntityFilter(entities), new AbstractBlockProcessor() {

            @Override
            public void process(DataBlock block) {

                // synchronized by TSFile
                AccessorBlockLink link = accessor.getBlockLink(block.getEntity(), block);
                if (!link.isEmpty()) {
                    uncompressedSize -= link.cut(range[0], range[1]);

                    if (link.isEmpty())
                        TreeOps.propagateDataDeletion(TSFile.this, block.getEntity());
                }
            }

            @Override
            public void complete() {
                // not have all blocks loaded, so validate all
                invalidateTime();
            }
        });
    }

    public  void       cut (final long startTime, final long endTime, final DataAccessorBase accessor) {

        assertCheckedOutTo(accessor);

        ensureIndexAndDataLoadedCatchIOX(EntityFilter.ALL, new AbstractBlockProcessor() {
            private long end = Long.MIN_VALUE;
            private long start = Long.MAX_VALUE;

            @Override
            public void         process(final DataBlock block) {

                AccessorBlockLink link = accessor.getBlockLink(block.getEntity(), block);

                if (!link.isEmpty()) {
                    uncompressedSize -= link.cut(startTime, endTime);

                    if (link.isEmpty()) {
                        TreeOps.propagateDataDeletion(TSFile.this, block.getEntity());
                    } else {
                        end = Math.max(end, link.getEndTime());
                        start = Math.min(start, link.getStartTime());
                    }
                }
            }

            @Override
            public void         complete() {
                if (end != Long.MAX_VALUE)
                    setLastTimestamp(end);

                updateStartTimestamp(start != Long.MAX_VALUE ? start : Long.MIN_VALUE);
            }
        });
    }

    private void                             updateStartTimestamp(long ts) {
        if (setStartTimestamp(ts) && getIdxInParent() == 0)
            TreeOps.setStartTimestamp(getParent(), ts);
    }

    long                            getFromTimestamp (int entity) {
        // ensure that we have index
        ensureIndexAndDataLoadedCatchIOX(null, null);

        synchronized (this) {
            int pos = find(entity);
            if (pos >= 0)
                return dbs.getObjectNoRangeCheck(pos).getStartTime();

            return Long.MAX_VALUE;
        }
    }

    long                            getFromTimestamp () {
        // ensure that we have index
        ensureIndexAndDataLoadedCatchIOX(null, null);

        long start = Long.MAX_VALUE;

        synchronized (this) {

            for (int pos = 0; pos < dbs.size(); pos++) {
                long time = dbs.getObjectNoRangeCheck(pos).getStartTime();
                if (time != Long.MIN_VALUE)
                    start = Math.min(time, start);
            }
        }

        return start;
    }

    long                            getToTimestamp (int entity) {
        // ensure that we have index
        ensureIndexAndDataLoadedCatchIOX(null, null);

        synchronized (this) {
            int pos = find(entity);
            if (pos >= 0)
                return dbs.getObjectNoRangeCheck(pos).getEndTime();

            return Long.MIN_VALUE;
        }
    }

    long                            getToTimestamp () {
        // ensure that we have index
        ensureIndexAndDataLoadedCatchIOX(null, null);

        long start = Long.MIN_VALUE;

        synchronized (this) {

            for (int pos = 0; pos < dbs.size(); pos++) {
                long time = dbs.getObjectNoRangeCheck(pos).getEndTime();
                if (time != Long.MAX_VALUE)
                    start = Math.max(time, start);
            }
        }

        return start;
    }
    //
    //  INTERFACE WITH PDS
    // 
    @Override
    String                          getNormalName () {
        return (TSNames.buildFileName (getId ()));
    }

    @Override
    boolean                         exists () {
        assert root.currentThreadHoldsWriteLock ();

        return (!isNew);
    }

    @Override
    synchronized void                            activate () {
        assert Thread.holdsLock(this);

        super.activate ();

        //PDSImpl.LOGGER.log(Level.INFO, this + ": " + state + "->" + TSFState.CLEAN_CACHED);

        state = TSFState.CLEAN_CACHED;

        if (isNew) {
            lastTimestamp = Long.MIN_VALUE;
            uncompressedSize = INITIAL_SIZE;
            dbs = new ObjectArrayList<> ();
        }
    }

    @Override
    void                drop() {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append(this).append(": dropping").commit();

        getParent().buildCache();
        getParent().dropChild(this);

        synchronized (this) {
            dropped = true;

            if (state == CLEAN_CACHED) {
                state = DIRTY_QUEUED_FOR_WRITE;
                addToWriteQueue(root.getCache());
            } else if (state == CLEAN_CHECKED_OUT) {
                state = DIRTY_CHECKED_OUT;
            }
        }
    }

    @Override
    synchronized void                deactivate () {

        if (dbs != null) {
            for (int pos = 0, numEntities = dbs.size(); pos < numEntities; pos++)
                dbs.getObjectNoRangeCheck(pos).clear();
            dbs.clear();
        }

        dbs = null;
        checkouts = null;
        uncompressedSize = -1;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append(this).append(": ").append(state).append("-> null").commit(); //append(new Exception()).commit();

        state = null;
        //lastTimestamp = Long.MAX_VALUE;

        super.deactivate();
    }

    synchronized void               checkOutTo (DAPrivate accessor) {

        if (dropped)
            throw new IllegalStateException(this + " is dropped.");

        if (checkouts == null)
            checkouts = new ObjectHashSet <> ();

        boolean         check = checkouts.add (accessor);

        if (!check)
            throw new IllegalArgumentException (
                this + " is ALREADY checked out to " + accessor
            );

        // call event before creating snapshot, because events will fire, if snapshot contains accessor
        accessor.checkedOut(this);

        // prepare snapshot
        if (checkoutsSnapshot == null)
            checkoutsSnapshot = new DAPrivate[checkouts.size()];

        checkoutsSnapshot = checkouts.toArray (checkoutsSnapshot);

        switch (state) {
            case CLEAN_CACHED:
                //PDSImpl.LOGGER.log (Level.INFO, this + ": " + state + "->" + TSFState.CLEAN_CHECKED_OUT);

                state = TSFState.CLEAN_CHECKED_OUT;
                break;

            case DIRTY_QUEUED_FOR_WRITE:

                // if file still in write queue - unuse it
                if (root.getCache ().removeFromWriteQueue (this)) {
                    useLess();
                }
                //else {
                //    PDSImpl.LOGGER.log(Level.WARN, this + ": " + state + "->" + TSFState.DIRTY_CHECKED_OUT + (dropped ? "(deleted)" : ""));
                //}

                state = TSFState.DIRTY_CHECKED_OUT;
                break;
        }
    }

    boolean                   store (BlockCompressor compressor) throws IOException {
        ensureIndexAndDataLoaded (EntityFilter.ALL, null);

        return storeInternal(compressor);
    }

    private synchronized boolean        storeInternal (BlockCompressor compressor)
        throws IOException
    {
        assert root.currentThreadHoldsAnyLock ();

        // file can be checked out again - reject storing
        if (state != DIRTY_QUEUED_FOR_WRITE)
            return false;

        int             numEntities = dbs.size ();

        compressedOnDisk = (compressor != null);

        int             indexSize = computeIndexBlockSize (compressedOnDisk, numEntities, FILE_FORMAT_VERSION);
        int             sizeOnDisk;
        int []          compLengths;
        ByteArrayList   compressedData = null;

        if (compressedOnDisk) {
            compressedData = compressor.getReusableBuffer();
            compressedData.setSize(0);
            compLengths = new int [numEntities];

            for (int ii = 0; ii < numEntities; ii++) {
                final DataBlock       db = (DataBlock) dbs.getObjectNoRangeCheck (ii);

                if (db.getDataLength() == 0)
                    compLengths[ii] = 0;
                else {
                    ByteArray data = db.getData();
                    compLengths[ii] = compressor.deflate(data.getArray(), data.getOffset(), db.getDataLength(), compressedData);
                }
            }

            sizeOnDisk = indexSize + compressedData.size ();
        }
        else {
            compLengths = null;
            sizeOnDisk = uncompressedSize;
        }

        formatVersion = FILE_FORMAT_VERSION;

        //
        //  Rebuild the offsets array to correspond to the data on disk.
        //  Must get a shared structure lock to prevent folder changes.
        //
        AbstractPath    tmp = TreeOps.makeTempPath (getParent ().getPath (), getNormalName ());

        try (OutputStream os = new BufferedOutputStream (tmp.openOutput (sizeOnDisk))) {
            DataOutputStream        dos = new DataOutputStream (os);

            dos.writeShort (formatVersion);
            dos.writeLong (getVersion());

            int                     flags = numEntities;

            if (compressedOnDisk)
                flags = TSFFormat.setAlgorithmCode(flags, compressor.code());

            dos.writeInt (flags);

            int                     offset = indexSize;

            for (int ii = 0; ii < numEntities; ii++) {
                DataBlock           db = (DataBlock) dbs.getObjectNoRangeCheck (ii);

                dos.writeInt (db.getEntity ());
                dos.writeInt (db.getDataLength ());

                if (compressedOnDisk)
                    dos.writeInt (compLengths [ii]);

                dos.writeLong (db.getStartTime ());
                dos.writeLong (db.getEndTime ());

                offset += db.getDataLength ();
            }

            if (compressedOnDisk) {
                os.write(compressedData.getInternalBuffer(), 0, compressedData.size());
            } else {

                uncompressedSize = offset;

//                if (uncompressedSize != offset)
//                    throw new IllegalStateException (
//                        "uncompressedSize=" + uncompressedSize + "; idx+sum(lengths)=" + offset
//                    );
                //
                //  Writing large blocks to BufferedOutputStream is efficient.
                //  Use BufferedOutputStream to buffer smaller blocks.
                //
                for (int ii = 0; ii < numEntities; ii++)
                    ((DataBlock) dbs.getObjectNoRangeCheck (ii)).store (os);
            }
        }

        TreeOps.finalize (tmp);

        isNew = false;

//        if (DEBUG_VERIFY_FILE_AFTER_STORE) {
//            TSFVerifier tsfv = new TSFVerifier ();
//
//            tsfv.setFile (getPath ());
//
//            try {
//                tsfv.verifyAll ();
//            } catch (Throwable x) {
//                System.exit (1);
//            }
//        }
        return true;
    }
    //
    //  INTERNALS
    //
    private EntityFilter            single (int entity) {
        SingleEntityFilter filter = sef.get();

        if (filter == null)
            sef.set(filter = new SingleEntityFilter (entity));
        else
            filter.entity = entity;

        return (filter);
    }

    synchronized boolean               checkedInBy (DAPrivate accessor) {
        if (state == null)
            return false;

        assertCheckedOutTo (accessor);

        boolean         check = checkouts.remove (accessor);

        if (!check)
            throw new IllegalArgumentException (
                this + " is not checked out to " + accessor
            );

        checkoutsSnapshot = checkouts.toArray (checkoutsSnapshot);

        if (checkouts.isEmpty ()) {
            PDSImpl     cache = root.getCache ();

            if (state == TSFState.DIRTY_CHECKED_OUT) {
                //PDSImpl.LOGGER.log (Level.FINE, this + ": " + state + "->" + TSFState.DIRTY_QUEUED_FOR_WRITE);

                state = DIRTY_QUEUED_FOR_WRITE;
                addToWriteQueue(cache);

            } else if (state == DIRTY_QUEUED_FOR_WRITE) {
                LOGGER.warn().append(this).append(": ").append(state).append(" -> ").append(TSFState.CLEAN_CACHED).commit();
            } else {
                state = TSFState.CLEAN_CACHED;
                cache.fileWasCheckedInClean(this);
            }
        }

        return true;
    }

    private void    addToWriteQueue(PDSImpl cache) {
        assert Thread.holdsLock(this);

        if (!queued) {
            useMore(); // account for queue referencing this TSF
            cache.addToWriteQueue(this);
        }
    }

    @Override
    synchronized void               destroy(boolean force) {
        super.destroy(force);

        state = null;
    }

//    private boolean                 isCheckedOut () {
//        return (
//            state == TSFState.CLEAN_CHECKED_OUT ||
//            state == TSFState.DIRTY_CHECKED_OUT
//        );
//    }

    private synchronized ExpansionStrategy  checkInsert (
        long                                    timestamp,
        int                                     addlLength
    )
    {
        // check start timestamp
        if (timestamp < getStartTimestamp())
            return ExpansionStrategy.MOVE_BACKWARD;

        // check limit timestamp
        if (timestamp >= limitTimestamp)
            return (ExpansionStrategy.MOVE_FORWARD);

        int             newSize = uncompressedSize + addlLength;
        int             maxSize = root.getMaxFileSize ();

        //
        if (timestamp > lastTimestamp && limitTimestamp == Long.MAX_VALUE) {
            if (newSize < maxSize && newSize > maxSize - maxSize / 10) // more that 90% max size
                return (ExpansionStrategy.MOVE_FORWARD);
        }

        //
        //  never create files smaller than max/2, even if next message takes 
        //  the size over maxSize.
        //
        if (newSize <= maxSize || uncompressedSize < maxSize / 2 || newSize - maxSize < maxSize / 10) {
            uncompressedSize = newSize;

            if (timestamp > lastTimestamp)
                setLastTimestamp(timestamp);

            return (null);
        }

        // allow overhead, if timestamp == last
        if (lastTimestamp == getStartTimestamp()) {
            uncompressedSize = newSize;
            return (null);
        }
        
        return (ExpansionStrategy.SPLIT);
    }

    private void                     openOrSeek (FileInput in, int toOffset)
        throws IOException
    {
        if (!in.isOpened()) {
            //
            //  To read anything, we must lock the
            //  folder tree from modification
            //

            if (LOGGER.isDebugEnabled())
                LOGGER.debug().append("opening ").append(this).commit();

            root.acquireSharedLock();
            try {
                in.open(getPath(), toOffset);
            } catch (IOException ex) {
                root.releaseSharedLock();
                throw ex;
            }
        } else {
            // Already open
            long skip = toOffset - in.getOffsetInFile();
            long reopenOnSeekThreshold = root.getFS().getReopenOnSeekThreshold();

            // Check if the "hole" is too big to use "seek"
            if (skip > reopenOnSeekThreshold) {
                // Reopen the stream
                in.close();
                in.open(getPath(), toOffset);
            } else {
                // Use "skip"
                in.seek(toOffset);
            }
        }
    }

    private void                    close(FileInput in) {
        if (in != null && in.close()) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug().append("closing ").append(getPathString()).commit();

            root.releaseSharedLock();
        }
    }
    
    private DataBlock               processEntity (
        FileInput                       in,
        int                             pos,
        DataBlockInfo                   dbi,
        BlockProcessor                  bp
    ) 
        throws IOException         
    {

        assert Thread.holdsLock(this);

        DataBlock                   db;

        if (dbi instanceof DataBlock)
            db = (DataBlock) dbi;
        else {        
            DataBlockStub       dbx = (DataBlockStub) dbi;
            
            openOrSeek (in, dbx.getOffsetInFile ());

            if (bp != null)
                db = bp.allocate(); //TODO: @ALLOCATION
            else
                db = new DataBlock();

            db.init (this, dbi.getEntity (), dbi.getDataLength (), dbi.getStartTime (), dbi.getEndTime ());

            if (compressedOnDisk && decompressor == null)
                decompressor = root.createDecompressor(compressionCode);

            in.read(db, dbx.getLengthOnDisk(), decompressor);
            dbs.set (pos, db);
        }

        if (bp != null)
            bp.process(db);
        
        return (db);
    }
    
    private DataBlock               getOrLoadBlock (int entity) {
        return (ensureIndexAndDataLoadedCatchIOX (single (entity), null));
    }
            
    private DataBlock               ensureIndexAndDataLoadedCatchIOX (
        EntityFilter                    filter,
        BlockProcessor                  bp
    ) 
    {
        try {
            return (ensureIndexAndDataLoaded (filter, bp));
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);        
        }
    }

    private synchronized boolean        isIndexLoaded() {
        return dbs != null;
    }

    /*
       Returns false if index is not loaded and creates index blocks
     */
    private FileInput                   ensureIndexLoaded() throws IOException {
        boolean loadIndex = !isIndexLoaded();

        FileInput input = inputs.borrow();
        if (loadIndex) // accept that index maybe already loaded on this line
            openOrSeek (input, 0);

        // what if file gets deleted here?

        synchronized (this) {
            if (state == null)
                throw new IllegalStateException(this + " is deactivated");

            loadIndex = (dbs == null);
            if (dbs == null)
                dbs = new ObjectArrayList<>();

            if (loadIndex) {
                if (input.isOpened())
                    readIndexBlock(input);
            }
        }

        return input;
    }
    
    private DataBlock           ensureIndexAndDataLoaded (
        EntityFilter                    filter,
        BlockProcessor                  bp
    ) 
        throws IOException
    {     
        if (isIndexLoaded() && filter == null)
            return (null);
        
        DataBlock               lastDB = null;

        FileInput input = null;
        
        try {
            input = ensureIndexLoaded();
                        
            if (filter == null)
                return (null);

            if (filter instanceof AbstractSingleEntityFilter) {
                int startOffset = -1;

                int entity = ((AbstractSingleEntityFilter) filter).getSingleEntity();

                synchronized (this) {
                    int pos = find(entity);

                    if (pos >= 0) {
                        DataBlockInfo info = dbs.getObjectNoRangeCheck(pos);
                        if (info instanceof DataBlockStub) {
                            startOffset = ((DataBlockStub) info).getOffsetInFile();
                        } else {
                            lastDB = processEntity(input, pos, dbs.getObjectNoRangeCheck(pos), bp);
                            if (bp != null)
                                bp.complete();

                            return lastDB;
                        }
                    }
                }

                if (startOffset != -1)
                    openOrSeek(input, startOffset);

                synchronized (this) {
                    int pos = find(entity);
                    if (pos >= 0)
                        lastDB = processEntity(input, pos, dbs.getObjectNoRangeCheck(pos), bp);

                    if (bp != null)
                        bp.complete();
                }
            } else {
                int startOffset = -1;
                int pos = 0;

                // first - find not loaded entity
                synchronized (this) {
                    int numEntities = dbs.size();

                    for (pos = 0; pos < numEntities; pos++) {
                        DataBlockInfo test = dbs.getObjectNoRangeCheck(pos);

                        if (filter.accept(test.getEntity())) {
                            if (test instanceof DataBlockStub) {
                                startOffset = ((DataBlockStub) test).getOffsetInFile();
                                break;
                            }

                            lastDB = processEntity(input, pos, test, bp);
                        }
                    }
                }

                // load data outside TSFile lock
                if (startOffset != -1)
                    openOrSeek(input, startOffset);

                synchronized (this) {
                    int numEntities = dbs.size();

                    for (; pos < numEntities; pos++) {
                        DataBlockInfo test = dbs.getObjectNoRangeCheck(pos);

                        if (filter.accept(test.getEntity()))
                            lastDB = processEntity(input, pos, test, bp);
                    }

                    if (bp != null)
                        bp.complete();
                }
            }
            
            return (lastDB);
        } finally {
            close(input);
            inputs.release(input);
        }
    }

    private synchronized void   readIndexBlock (FileInput input)
        throws IOException
    {
        dbs = new ObjectArrayList <> ();
        lastTimestamp = Long.MIN_VALUE;                                      

        DataInputStream     dis = new DataInputStream (input.getInputStream());

        formatVersion = dis.readShort ();
        version = formatVersion >= 3 ? dis.readLong () : dis.readInt();
        
        int                 flags = dis.readInt ();
        int                 numEntities = flags & TSFFormat.NUM_ENTS_MASK;

        compressedOnDisk = (flags & TSFFormat.COMPRESSED_FLAG) != 0;

        if (formatVersion >= 2)
            compressionCode = TSFFormat.getAlgorithmCode(flags);
        else if (compressedOnDisk)
            compressionCode = BlockCompressorFactory.getCode(Algorithm.LZ4);
        
        int                 indexBlockSize = computeIndexBlockSize (compressedOnDisk, numEntities, formatVersion);

        uncompressedSize = indexBlockSize;
        
        int                 blockOffsetOnDisk = indexBlockSize;        
        DataBlockStub       prevStub = null;

        for (int ii = 0; ii < numEntities; ii++) {
            int             entity = dis.readInt ();
        
            if (prevStub != null && entity <= prevStub.getEntity ())
                throw new IOException (
                    "Wrong entity ordering: " + entity + " after " + 
                    prevStub.getEntity ()
                );

            int             dataLength = dis.readInt ();   
            int             lengthOnDisk = compressedOnDisk ? dis.readInt () : dataLength;
            long            startTime = dis.readLong ();
            long            endTime = dis.readLong ();

            if (dataLength == 0) {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace().append(this).append(": Skipping empty data block for ").append(entity).commit();

                blockOffsetOnDisk += lengthOnDisk;
                continue;
            }
        
            DataBlockStub   dbx = 
                new DataBlockStub (
                    entity, 
                    blockOffsetOnDisk,
                    lengthOnDisk, dataLength, 
                    startTime, endTime
                );

            dbs.add (dbx);

            prevStub = dbx;

            uncompressedSize += dataLength;
            blockOffsetOnDisk += lengthOnDisk;
                
            if (endTime > lastTimestamp)
                setLastTimestamp(endTime);
        }

        input.setOffset(indexBlockSize);
    }
    
    private synchronized void       assertCheckedOutTo (DAPrivate accessor) {
//        assert isCheckedOut () :
//            this + " is not in a checked out state";
        
        assert isCheckedOutTo (accessor) :
            this + " is not checked out to " + accessor;
    }
    
    private boolean  isCheckedOutTo (DAPrivate accessor) {
        return (checkouts != null && checkouts.contains (accessor));
    }
    
    private int                     find (int entity) {

        assert Thread.holdsLock(this);

        int         low = 0;
        int         high = dbs.size () - 1;

        while (low <= high) {
            int     mid = (low + high) >>> 1;
            int     midVal = dbs.getObjectNoRangeCheck (mid).getEntity ();
            
            if (midVal < entity)
                low = mid + 1;
            else if (midVal > entity)
                high = mid - 1;
            else
                return mid;
        }

        return -(low + 1);
    }

   
//    private final ObjectArrayList <DALinkPrivate>   bufLinks = 
//        new ObjectArrayList <> ();        
    
//    private void                split (long newStartTs) 
//        throws IOException, InterruptedException 
//    {
//        processBlocks (null, null);
//        
//        TSFile                  latter = 
//            getRoot ().insertFileAfter (this, newStartTs);
//                       
//        int                     numEntities = dbs.size ();
//        
//        for (int pos = 0; pos < numEntities; pos++) {
//            DataBlockStub       dbx = dbs.getObjectNoRangeCheck (pos);
//            int                 entity = dbx.getEntity ();
//            DataBlock           formerBlock = dbx.getBlock ();
//            int                 oldBlockLength = formerBlock.getLength ();
//            int                 splitOffset = formerBlock.findOffset (0, newStartTs + 1);
//            int                 latterBlockLength = oldBlockLength - splitOffset;
//            //
//            //  Future optimization: 
//            //      if (splitOffset == 0) then reuse (formerBlock)
//            //
//            DataBlock           latterBlock;
//            
//            if (latterBlockLength == 0)
//                latterBlock = null;
//            else {
//                latterBlock = latter.getBlock (entity, true);
//                latterBlock.setData (formerBlock.getBytes (), splitOffset, latterBlockLength);
//            }
//            
//            if (splitOffset != oldBlockLength)
//                formerBlock.shorten (splitOffset);  // can't remove!
//            
//            formerBlock.getCheckOuts (bufLinks);
//            
//            for (int ii = 0; ii < bufLinks.size (); ii++) {
//                DALinkPrivate   link = bufLinks.getObjectNoRangeCheck (ii);                
//                DAPrivate       accessor = link.getAccessor ();
//                long            currentTimestamp = accessor.getCurrentTimestamp ();
//                int             blockOffset = link.getBlockOffset ();
//                
//                if (blockOffset > oldBlockLength)
//                    throw new RuntimeException (
//                        link + " is at offset=" + blockOffset + 
//                        " BEYOND block length=" + oldBlockLength
//                    );                    
//                //
//                //  If this accessor is staying with the former TSF,
//                //  nothing needs to be done. The block will be shortened.
//                //
//                if (currentTimestamp < newStartTs) {
//                    if (blockOffset > splitOffset)  // Cannot be
//                        throw new RuntimeException (
//                            "Offset/timestamp discrepancy: " + link + 
//                            " is at offset=" + blockOffset + 
//                            ", which is AHEAD OF splitOffset=" + splitOffset +
//                            " for splitTimestamp=" + newStartTs +
//                            ", while the link's accessor " + accessor +
//                            " reports EARLIER currentTimestamp=" + currentTimestamp                                                        
//                        );
//                    
//                    if (splitOffset == 0)
//                        link.kill ();
//                    
//                    //  else the block stays (shortened)
//                }
//                else {
//                    //
//                    //  This accessor is moving on. Is this is the first link,
//                    //  switch the checkout
//                    //                     
//                    if (checkouts.remove (accessor)) {
//                        latter.checkedOutTo (accessor);
//                        
//                        accessor.switchTimeSlice (latter);
//                    }
//                    
//                    if (latterBlockLength == 0)
//                        link.kill ();
//                    else {
//                        int         newOffset = blockOffset - splitOffset;
//                        //
//                        //  It's legal for links to lag with respect to 
//                        //  the accessor's currentTimestamp. Such links will
//                        //  be forcefully fast-forwarded here
//                        //
//                        if (newOffset < 0)  
//                            newOffset = 0;
//                    
//                        link.switchBlock (latterBlock, newOffset);                                                                                    
//                    }
//                }
//            }                                
//        }
//        
//        if (checkouts.isEmpty ()) 
//            getRoot ().getCache ().unreferenced (this);
//    }

}
