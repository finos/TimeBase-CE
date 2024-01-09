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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.migration.SchemaChangeMessageBuilder;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.FSUtils;
import com.epam.deltix.qsrv.dtb.store.dataacc.DataAccessorBase;
import com.epam.deltix.qsrv.dtb.store.dataacc.TimeSlice;
import com.epam.deltix.qsrv.dtb.store.impl.Restorer;
import com.epam.deltix.qsrv.dtb.store.impl.WriterChannel;
import com.epam.deltix.qsrv.dtb.store.pub.DataAccessor;
import com.epam.deltix.qsrv.dtb.store.pub.DataReader;
import com.epam.deltix.qsrv.dtb.store.pub.DataWriter;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.SymbolRegistry;
import com.epam.deltix.qsrv.dtb.store.pub.TSRef;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.qsrv.dtb.store.pub.TimeRange;
import com.epam.deltix.qsrv.dtb.store.pub.TimeSliceIterator;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaUpdateTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamCopyTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.timebase.messages.schema.SchemaChangeMessage;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.progress.ExecutionMonitorImpl;
import com.epam.deltix.util.progress.ExecutionStatus;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.time.TimeKeeper;
import net.jcip.annotations.GuardedBy;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@XmlRootElement(name = "pdstream")
public class PDStream extends TickStreamImpl {
    public static final String DEFAULT_SPACE = "";
    private static final String DEFAULT_SPACE_FOLDER = "data";

    private static final String TRANSFORMED_PATH_SUFFIX = "_";

    protected static final Log LOGGER = LogFactory.getLog(PDStream.class);

    private static final String                 CONSISTENCY_CHECK_PROP = "TimeBase.consistencyCheck";
    private static final boolean                CONSISTENCY_CHECK = Boolean.parseBoolean(System.getProperty(CONSISTENCY_CHECK_PROP, "true"));

    private volatile TSRoot                     root;

    @GuardedBy("this")
    private volatile Thread                     bgRunner;
    private volatile BackgroundProcessInfo      bgProcess;

    @GuardedBy("this")
    private boolean                             schemaChangeInProgress = false;

    private final ObjectHashSet<DataWriter>     writers = new ObjectHashSet<>();
    private final ObjectHashSet<DataReader>     readers = new ObjectHashSet<>();

    @GuardedBy("this")
    @Deprecated
    @XmlElement(name = "locations")
    private ArrayList<String>             locations;

    protected final ArrayList<TSRoot>           roots = new ArrayList<>();

    @GuardedBy("roots")
    @XmlElement(name = "spaces")
    private ArrayList<SpaceEntry>         spaces;

    public PDStream() {
    }

    public PDStream(DXTickDB db, String key, StreamOptions options) {
        super(db, key, options);
    }

    @Override
    public synchronized void        format() {
        runUnderLock(this::formatWithoutLock);
    }

    /**
     * Performs {@link #format()} without taking remote lock.
     */
    synchronized void formatWithoutLock() {
        boolean isEmpty = root == null;

        init();

        if (!isEmpty) {
            TSRoot[] active = getActiveRoots();
            for (TSRoot tsr : active)
                tsr.format();
        }

        onDelete(); // clear redundant files

        isOpen = true;
        isReadOnly = false;

        writeMetadata(true);

        setDirty(true);
    }

    public synchronized void        rename (String key) {
        if (key == null)
            throw new IllegalArgumentException("Stream key cannot be null.");

        assertWritable();
        assertFinal();

        if (!Util.xequals (key, getKey())) {
            if (db.getStream(key) != null)
                throw new IllegalArgumentException("Stream with key '" + key + "' already exists.");

            String before = getKey();

            TickDBImpl.RemoteStreamLock lock1 = getDBImpl().getRemoteStreamLockOrStub(before, isRemoteMetadata());
            TickDBImpl.RemoteStreamLock lock2 = getDBImpl().getRemoteStreamLockOrStub(key, isRemoteMetadata());
            try {
                if (isRemoteMetadata()) {
                    getDBImpl().syncRemoteStreams();
                    if (!before.equals(getKey())) {
                        throw new IllegalArgumentException("Stream key was changed remotely.");
                    }
                }

                getDBImpl().streamRenamed(key, before);
                renameInternal(before, key, true, true);
            } finally {
                lock1.releaseSilent();
                lock2.releaseSilent();
            }
        }
    }

    void renameInternal(String before, String key, boolean writeFiles, boolean holdsRemoteStreamLock) {
        setKey(key);

        if (writeFiles) {
            String encodedName = SimpleStringCodec.DEFAULT_INSTANCE.encode(key);
            String fileName = encodedName + TickDBImpl.STREAM_EXTENSION;
            if (isRemoteMetadata()) {
                // Remote metadata
                AbstractPath streamFolder = metadataLocation.getPath().getParentPath().getParentPath().append(encodedName);
                AbstractPath newStreamFile = streamFolder.append(fileName);
                try {
                    streamFolder.makeFolderRecursive();
                    metadataLocation.getPath().moveTo(newStreamFile);
                } catch (IOException e) {
                    // Failed to rename
                    // TODO: Handle?
                    throw new RuntimeException("Failed to rename remote stream metadata file", e);
                }
            } else {
                File newFile = new File(file.getParent(), fileName);

                AbstractPath path = metadataLocation.getPath();
                FileLocation newLocation = new FileLocation(newFile);
                try {
                    if (path.exists())
                        path.moveTo(newLocation.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                metadataLocation = newLocation;
                file = newFile;
            }

            writeMetadata(holdsRemoteStreamLock);

            if (isRemoteMetadata()) {
                getDBImpl().notifyRemoteStreamRenamed(before, key);
            }
        }

        onRename(file.getParentFile(), before);

        getDBImpl ().fireRenamed(this, before);
    }

    public RecordClassDescriptor[]             getTypes() {
        return isFixedType() ? new RecordClassDescriptor[] {getFixedType()} : getPolymorphicDescriptors();
    }

    @Override
    public int                      getFormatVersion() {
        return 5;
    }

    @Override
    public void                     truncate(long timestamp, IdentityKey ... ids) {
        long time = TimeStamp.getNanoTime(timestamp);

        boolean changed = false;
        for (TSRoot tsr : getActiveRoots()) {
            if (deleteInternal(tsr, time, Long.MAX_VALUE, ids))
                changed = true;
        }

        if (changed)
            onStreamTruncated(time, ids.length != 0 ? ids : listEntities());
    }

    /**
     * Truncate single entity using specified nanoseconds time
     * @param nstime time in nanoseconds
     * @param id Instrument Identity to truncate
     */
    void                     truncateInternal(TSRoot root, long nstime, IdentityKey id) {
        if (deleteInternal(root, nstime, Long.MAX_VALUE, id))
           onStreamTruncated(nstime, id);
    }

    @Override
    public void                     delete(TimeStamp start, TimeStamp end, IdentityKey ... ids) {
        boolean changed = false;

        for (TSRoot tsr : getActiveRoots()) {
            if (deleteInternal(tsr, start.getNanoTime(), end.getNanoTime(), ids))
                changed = true;
        }

        if (changed)
            onStreamTruncated(start.getNanoTime(), ids.length != 0 ? ids : listEntities());
    }

    private TSRoot                  getRoot(IdentityKey id) {

        assertOpen();

        TSRoot[] active = getActiveRoots();
        for (TSRoot tsr : active) {
            if (tsr.getSymbolRegistry().symbolToId(id.getSymbol()) != SymbolRegistry.NO_SUCH_SYMBOL)
                return tsr;
        }

        return root;
    }

    private TSRoot[]                        getActiveRoots() {

        synchronized (roots) {
            TSRoot[] list = roots.toArray(new TSRoot[this.roots.size() + 1]);
            list[list.length - 1] = root;
            return list;
        }
    }

    TSRoot[]        getRoots(PDStreamSource.SourceSubscription sub, long nstime, boolean live, @Nullable String[] spaces) {
        if (spaces == null) {
            return getRoots(sub, nstime, live);
        } else {
            return getRootsForSpace(spaces);
        }
    }

    private TSRoot[]            getRootsForSpace(@Nonnull String[] spaces) {
        assertOpen();

        ArrayList<TSRoot> result = new ArrayList<>();

        for (String space : spaces) {
            if (space == null) {
                throw new IllegalArgumentException("Space cannot be null");
            } else {
                TSRoot tsr = getOrCreateRoot(space, false);
                if (tsr != null)
                    result.add(tsr);
            }
        }

        return result.toArray(new TSRoot[0]);
    }

    @Nonnull
    private TSRoot getRootBySpaceName(@Nonnull String space) {
        TSRoot tsr = getOrCreateRoot(space, false);
        if (tsr == null)
            throw new UnknownSpaceException("Space '" + space + "' does not exist in stream [" + getKey() + "]");
        return tsr;
    }

    private TSRoot[] getRoots(PDStreamSource.SourceSubscription sub, long nstime, boolean live) {

        assertOpen();

        HashSet<TSRoot> result = new HashSet<TSRoot>();

        TSRoot[] active = getActiveRoots();

        if (sub.subscribed == null || live) {
            result.addAll(Arrays.asList(active));
//            if (live) {
//
//            } else {
//                TimeRange out = new TimeRange();
//                for (int i = 0, activeLength = active.length; i < activeLength; i++) {
//                    TSRoot tsr = active[i];
//                    tsr.getTimeRange(out);
//                    if (nstime <= out.to)
//                        result.add(tsr);
//                }
//            }
        } else if (!sub.isEmpty()) {
            for (IdentityKey id : sub.subscribed) {

                for (int i = 0, activeLength = active.length; i < activeLength; i++) {
                    TSRoot tsr = active[i];
                    if (tsr.getSymbolRegistry().symbolToId(id.getSymbol()) != SymbolRegistry.NO_SUCH_SYMBOL)
                        result.add(tsr);
                }

                if (result.size() == active.length) // if all roots already added
                    break;
            }
        }

        return result.toArray(new TSRoot[result.size()]);
    }

    private TSRoot getOrCreateRoot(@Nonnull String spot, boolean create) {
        if (DEFAULT_SPACE.equals(spot)) {
            // Default space
            return this.root;
        }

        String key = getKey();

        if (spot.equals(DEFAULT_SPACE_FOLDER))
            throw new IllegalArgumentException("Stream [" + key + "] space name '" + DEFAULT_SPACE_FOLDER + "' is reserved and not permitted");

        try {
            TSRoot selected = null;
            boolean changed = false;
            // = roots.stream().filter(x -> x.getPath().equals(path)).findFirst().get();

            synchronized (roots) {

                for (TSRoot tsr : roots) {
                    String tsrSpace = tsr.getSpace();
                    assert tsrSpace != null;
                    if (spot.equals(tsrSpace)) {
                        selected = tsr;
                        break;
                    }
                }

                if (selected == null) {
                    if (create) {
                        String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(spot);

                        AbstractPath parent = root.getPath().getParentPath();
                        AbstractPath path = root.getFileSystem().createPath(parent, name);

                        // we already had space with this name
                        if (path.exists()) {
                            // add day ms
                            long ns = GMT.getTomorrow().getTime() - TimeKeeper.currentTime;

                            name = SimpleStringCodec.DEFAULT_INSTANCE.encode(ns + "@" + spot);
                            path = root.getFileSystem().createPath(parent, name);

                            LOGGER.info("Stream [" + key + "] already has path associated with space = (" + spot + "). Assign new name: (" + ns + "@" + spot + ")");
                        }

                        if (path.exists())
                            throw new IllegalStateException("Stream [" + key + "] already has path associated with space = (" + spot + "): " + path);

                        selected = createRoot(path, spot);

                        roots.add(selected);
                        spaces.add(new SpaceEntry(spot, "\\" + path.getName()));
                        changed = true;
                    } else {
                        return null;
                    }
                }
            }

            // lock order: this->roots
            if (changed) {
//                SpaceCreatedMessage msg = new SpaceCreatedMessage();
//                msg.setSymbol(""); // @SYSTEM
//                msg.setTimeStampMs(TimeKeeper.currentTime);
//                msg.setInstrumentType(InstrumentType.SYSTEM);
//                msg.setName(spot);
//
//                addSystemMessage(msg);

                setDirty(true);

                TickCursor[] cursors = getSnapshotOfOpenCursors();
                for (int i = 0; i < cursors.length; i++) {
                    TickCursor cursor = cursors[i];
                    if (cursor instanceof TickCursorImpl)
                        ((TickCursorImpl) cursor).notifySpaceCreated(this, spot);
                }
            }

            return selected;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
        Deletes data, time range defined in nanoseconds
     */
     /*
        Deletes data, time range defined in nanoseconds
     */
    private boolean                     deleteInternal(TSRoot root, final long from, final long to, IdentityKey... ids) {

        assertOpen();

        AtomicBoolean dataChanged = new AtomicBoolean(false);

        if (from > to)
            throw new IllegalArgumentException("Start time (" + GMT.formatNanos(from) + ") > End Time (" + GMT.formatNanos(to) + ")");

        if (ids == null || ids.length == 0) {

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Deleting data from [" + getKey() + ", space=" + root.getSpace() +
                        " using time range [" + GMT.formatNanos(from) + "," + GMT.formatNanos(to) + "; instruments = ALL");

            // drop files first
            dataChanged.set(root.drop(new TimeRange(from, to)));

            try (DataWriter writer = getDBImpl().store.createWriter ()) {
                writer.associate(root);

                root.iterate(new TimeRange(from, to), null, new TimeSliceIterator() {

                    @Override
                    public DataAccessor getAccessor() {
                        return writer;
                    }

                    @Override
                    public void process(TimeSlice slice) {
                        //System.out.println("Process slice: " + slice);
                        dataChanged.set(slice.cut(from, to, (DataAccessorBase) writer));
                    }
                });
            }
        } else {
            IntegerArrayList list = new IntegerArrayList(ids.length);
            SymbolRegistry sr = root.getSymbolRegistry();
            ArrayList<CharSequence> names = new ArrayList<>();

            for (IdentityKey entity : ids) {
                int id = sr.symbolToId(entity.getSymbol());

                if (id == SymbolRegistry.NO_SUCH_SYMBOL)
                    continue;

                list.add(id);
                names.add(entity.getSymbol());
            }

            final long[] range = new long[] {from, to};
            final int[] entities = list.toIntArray();

            // actual entities exists found in this root

            if (entities.length > 0) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Deleting data from [" + getKey() + ", space=" + root.getSpace() +
                            " using time range [" + GMT.formatNanos(from) + "," + GMT.formatNanos(to) + "; instruments = (" + Arrays.toString(names.toArray()) + ")");

                try (final DataWriter writer = getDBImpl().store.createWriter()) {
                    writer.associate(root);

                    root.iterate(new TimeRange(from, to), null, new TimeSliceIterator() {

                        @Override
                        public DataAccessor getAccessor() {
                            return writer;
                        }

                        @Override
                        public void process(TimeSlice slice) {
                            //System.out.println("Process slice: " + slice);
                            dataChanged.set(slice.cut(range, entities, (DataAccessorBase) writer));
                        }
                    });
                }
            }
        }

        if (dataChanged.get()) {
            firePropertyChanged(TickStreamProperties.ENTITIES);
            firePropertyChanged(TickStreamProperties.TIME_RANGE);
        }

        return dataChanged.get();
    }

    @Override
    public IdentityKey[]             getComposition(IdentityKey... ids) {
        return ids;
    }

    @Override
    public void                             clear(IdentityKey... ids) {
        TSRoot[] active = getActiveRoots();
        for (TSRoot tsr : active)
            clear(tsr, ids);

        firePropertyChanged(TickStreamProperties.ENTITIES);
        firePropertyChanged(TickStreamProperties.TIME_RANGE);
    }

    public void                             clear(TSRoot tsr, IdentityKey... ids) {
        assertWritable();

        if (ids == null || ids.length == 0) {
            deleteInternal(tsr, Long.MIN_VALUE, Long.MAX_VALUE);
            ids = listEntities();
        } else {
            deleteInternal(tsr, Long.MIN_VALUE, Long.MAX_VALUE, ids);
        }

        SymbolRegistry symbols = tsr.getSymbolRegistry();
        for (IdentityKey id : ids)
            symbols.unregisterSymbol(id.getSymbol());
    }

    @Override
    public void                             purge(long time) {
        assertWritable();

        if (time != Long.MIN_VALUE)
            time = time - 1;

        String key = getKey();
        long startTime = System.currentTimeMillis();
        LOGGER.info("Purge for stream %s using time=%s started ...").with(key).with(GMT.formatDateTimeMillis(time));
        try {
            TSRoot[] active = getActiveRoots();
            for (TSRoot tsr : active)
                deleteInternal(tsr, Long.MIN_VALUE, TimeStamp.getNanoTime(time)); // delete all older that time
        } finally {
            long endTime = System.currentTimeMillis();
            LOGGER.info("Purge for stream %s was finished in %s ms").with(key).with(endTime - startTime);
        }
    }

    @Override
    public void                             purge(long time, String space) {
        assertWritable();

        if (time != Long.MIN_VALUE)
            time = time - 1;

        String key = getKey();
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Purge for stream %s space %s using time=%s started ... ").with(key).with(space).with(GMT.formatDateTimeMillis(time));
        try {
            TSRoot tsr = getRootBySpaceName(space);
            deleteInternal(tsr, Long.MIN_VALUE, TimeStamp.getNanoTime(time)); // delete all older that time
        } finally {
            long endTime = System.currentTimeMillis();
            LOGGER.debug("Purge for stream %s space %s was finished in %s ms").with(key).with(space).with(endTime - startTime);
        }
    }

    @Override
    MessageChannel<InstrumentMessage> createChannel(InstrumentMessage msg, LoadingOptions options) {

        assertWritable();

        if (options == null)
            options = new LoadingOptions ();

        TSRoot tsr = (options.space != null) ? getOrCreateRoot(options.space, true) : root;

        DataWriter writer = getDBImpl().store.createWriter();
        writer.associate(tsr);

        MessageProducer<? extends InstrumentMessage> producer = options.raw ?
                new RawProducer(getTypes()) :
                new Producer(getTypes(), options.getTypeLoader(), getCodecFactory(options.channelQOS == ChannelQualityOfService.MIN_INIT_TIME));

        return new PDStreamChannel(this, tsr, writer, producer, options);
    }

    public SymbolRegistry                  getSymbols() {
        return root.getSymbolRegistry();
    }

    @Override
    public IdentityKey []                   listEntities () {
        HashSet<IdentityKey> ids = new HashSet<IdentityKey>();

        TSRoot[] active = getActiveRoots();
        for (TSRoot tsr : active)
            listEntities(tsr, ids);

        return ids.toArray(new IdentityKey[ids.size()]);
    }

    public void            listEntities (TSRoot root, HashSet<IdentityKey> ids) {
        //List<InstrumentType> typeList = Arrays.asList(types);

        ArrayList<String> symbols = new ArrayList<String>();
        ArrayList<String> data = new ArrayList<String>();

        SymbolRegistry sr = root.getSymbolRegistry();
        sr.listSymbols(symbols, data);

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            String value = data.get(i);

            ids.add(new ConstantIdentityKey(symbol));
        }
    }

    @Override
    public synchronized void renameInstruments(final IdentityKey[] from, final IdentityKey[] to) {
        assertWritable();

        if (from == null || to == null)
            throw new NullPointerException("Input parameters can't be null!");

        if (from.length != to.length)
            throw new IllegalArgumentException("Sizes of arrays are not equal!");

        for (int i = 0; i < to.length - 1; ++i) {
            for (int j = i + 1; j < to.length; ++j) {
                if (to[i].equals(to[j]))
                    throw new IllegalArgumentException("We can't rename 2 different instruments into one the same " +
                            "(" + to[i] + ")!");
            }
        }

        for (int i = 0; i < from.length - 1; ++i) {
            for (int j = i + 1; j < from.length; ++j) {
                if (from[i].equals(from[j]))
                    throw new IllegalArgumentException("Duplicate source instrument (" + from[i] + ")!");
            }
        }

        for (int i = 0; i < to.length; ++i) {
            SymbolRegistry sr = getRoot(to[i]).getSymbolRegistry();

            if (sr.symbolToId(to[i].getSymbol()) != SymbolRegistry.NO_SUCH_SYMBOL)
                throw new IllegalArgumentException("Instrument " + to[i] + " already exists!");
        }

        HashSet<TSRoot> dirty = new HashSet<TSRoot>();

        for (int i = 0; i < from.length; ++i) {
            TSRoot tsr = getRoot(from[i]);

            SymbolRegistry sr = tsr.getSymbolRegistry();
            sr.renameSymbol(
                    from[i].getSymbol().toString(),
                    to[i].getSymbol().toString(),
                    null);

            dirty.add(tsr);
        }

        try {
            for (TSRoot tsr : dirty)
                tsr.getSymbolRegistry().storeIfDirty(tsr.getPath());
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        firePropertyChanged(TickStreamProperties.ENTITIES);
    }

    @Override
    public void             addInstrument(IdentityKey id) {
        SymbolRegistry symbols = root.getSymbolRegistry();
        String symbol = id.getSymbol().toString();

        symbols.registerSymbol(symbol, null);
    }

    @Override
    public long[]           getTimeRange(IdentityKey... entities) {

        com.epam.deltix.qsrv.dtb.store.pub.TimeRange all = new com.epam.deltix.qsrv.dtb.store.pub.TimeRange();

        TSRoot[] active = getActiveRoots();
        for (TSRoot tsr : active)
            getTimeRange(tsr, all, entities);

        return !all.isUndefined() ? new long[] {TimeStamp.getMilliseconds(all.from), TimeStamp.getMilliseconds(all.to)} : null;
    }

    public void           getTimeRange(TSRoot root, com.epam.deltix.qsrv.dtb.store.pub.TimeRange all, IdentityKey... entities) {

        if (entities.length == 0) {
            com.epam.deltix.qsrv.dtb.store.pub.TimeRange next = new com.epam.deltix.qsrv.dtb.store.pub.TimeRange();
            root.getTimeRange(next);
            if (!next.isUndefined())
                all.unionInPlace(next.from, next.to);

        } else {
            SymbolRegistry sr = root.getSymbolRegistry();
            com.epam.deltix.qsrv.dtb.store.pub.TimeRange range = new com.epam.deltix.qsrv.dtb.store.pub.TimeRange();

            for (IdentityKey entity : entities) {
                int id = sr.symbolToId(entity.getSymbol());

                root.getTimeRange(id, range);

                if (!range.isUndefined())
                    all.unionInPlace(range.from, range.to);
            }
        }
    }

    @Override
    public TimeInterval[]       listTimeRange(IdentityKey ... ids) {

        TimeInterval[] intervals = listTimeRange(root, ids);

        TSRoot[] active = getActiveRoots();

        for (int ii = 0; ii < active.length - 1; ii++) {
            TimeInterval[] range = listTimeRange(active[ii], ids);
            // merge with main
            for (int i = 0; i < intervals.length; i++) {
                if (intervals[i] == null)
                    intervals[i] = range[i];
                else if (range[i] != null)
                    intervals[i] = union(range[i], intervals[i]);
            }

        }

        return intervals;
    }

    public TimeInterval         union(TimeInterval t1, TimeInterval t2) {
        TimeRange range = new TimeRange(t1.getFromTime(), t2.getToTime());
        range.unionInPlace(t2.getFromTime(), t2.getToTime());
        return range;
    }

    private TimeInterval[]       listTimeRange(TSRoot root, IdentityKey ... ids) {

        SymbolRegistry sr = root.getSymbolRegistry();

        IntegerArrayList list = new IntegerArrayList(ids.length);
        for (IdentityKey entity : ids) {
            int id = sr.symbolToId(entity.getSymbol());
            list.add(id);
        }

        return root.getTimeRanges(list.toIntArray());
    }

    public String           getDataLocation() {
        if (this.location == null)
            setLocation(File.separator + DEFAULT_SPACE_FOLDER);

        return getAbsolutePath(location);
    }

    private String  getAbsolutePath(String location) {
        if (location.startsWith("\\") || location.startsWith("/")) { // relative path
            if (metadataLocation.isRemote()) {
                throw new UnsupportedOperationException("Local data files for remote streams is not supported");
            }
            return new File(file.getParentFile(), location.substring(1)).getAbsolutePath();
        } else {
            return location;
        }
    }

    /**
     * Attempts to convert absolute path to a relative path using provided base path.
     * If the base path does not match to a relative path then writes a warning to a log and returns absolute path "as is"
     *
     * @param basePathString base path. May not have "\" or "/" at the end.
     * @param path path to be converted to a relative path
     * @return relative path in case of success and absolute path in case of failure.
     */
    private String getRelativePath(String basePathString, AbstractPath path) {
        String pathString = path.getPathString();
        if (pathString.startsWith(basePathString)) {
            String relativePath = pathString.substring(basePathString.length());
            if (relativePath.startsWith("\\") || relativePath.startsWith("/")) {
                // Success
                return relativePath;
            }
        }

        LOGGER.warn("Failed to build relative path from \"%s\" with base path \"%s\" for PDStream \"%s\" during transformation")
                .with(pathString).with(basePathString).with(getKey());
        return pathString;
    }

    // Used by Repair Shop

    public String[]         getDataLocations() {

        ArrayList<String> result = new ArrayList<String>();
        result.add(getDataLocation());

        if (spaces != null) {
            for (SpaceEntry e : spaces)
                result.add(getAbsolutePath(e.path));
        }

        return result.toArray(new String[result.size()]);
    }

    private void            setLocation(String location) {
        if (!StringUtils.equals(this.location, location)) {
            if (metadataLocation.isRemote()) {
                this.location = location;
            } else {
                this.location = location.replace(file.getParentFile().getAbsolutePath(), "");
            }
            setDirty(TickStreamPropertiesEnum.LOCATION);
        }
    }

    private void init() {
        try {

            synchronized (roots) {
                if (root == null)
                    root = createRoot(getDataLocation(), null);

                // new version
                if (spaces == null && locations != null) {
                    spaces = new ArrayList<>();
                    for (String path : locations)
                        spaces.add(new SpaceEntry(getSpaceName(path), path));

                    locations = null;
                } else if (spaces == null) {
                    spaces = new ArrayList<>();
                }

                Set<String> list = !spaces.isEmpty() ? new HashSet<>() : null;
                for (SpaceEntry se : spaces) {
                    TSRoot root = createRoot(getAbsolutePath(se.path), se.name);
                    roots.add(root);

                    // Ensure there is no conflicting spaces
                    String space = root.getSpace();
                    if (list.contains(space))
                        LOGGER.error("Duplicate locations for space \"%s\" of stream \"%s\"").with(space).with(getKey());

                    list.add(space);
                }
            }

        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    private TSRoot createRoot(String dataLocation, String space) throws IOException {
        AbstractFileSystem fs = FSFactory.create(dataLocation);
        AbstractPath path = fs.createPath(dataLocation);

        return createRoot(path, space);
    }

    private TSRoot createRoot(AbstractPath path, @Nullable String space) throws IOException {
        boolean exists = path.exists();
        path.makeFolderRecursive();

        TickDBImpl db = getDBImpl();
        TSRoot r = db.store.createRoot(space, path);

        if (!exists) {
            r.setMaxFileSize(db.fs.maxFileSize);
            r.setMaxFolderSize(db.fs.maxFolderSize);
            r.setCompression(db.fs.compression);
            r.format();
        } else if (isOpen) {
            r.open(isReadOnly);
        }

        return r;
    }

    @Override
    protected void                  onOpen (boolean verify) throws IOException {
        init();

//        // verify spaces
//        {
//            AbstractPath parentPath = root.getPath().getParentPath();
//            String[] items = parentPath.listFolder();
//
//            HashSet<String> cache = new HashSet<String>();
//
//            for (SpaceEntry e : spaces)
//                cache.add(e.path.substring(1));
//
//            for (String item : items) {
//                if (!cache.contains(item)) {
//                    if (parentPath.append(item).isFolder() && "data".equals(item))
//                        TickDBImpl.LOG.warn("Space with path: [" + item + "] is not present in stream");
//                }
//            }
//        }

        TickDBImpl.LOG.info("[%s] open: checking data consistency...").with(getKey());

        TSRoot[] active = getActiveRoots();
        for (TSRoot r : active)
            Restorer.restore(r, isReadOnly(), verify || CONSISTENCY_CHECK);

        TickDBImpl.LOG.info("[%s] open: consistency check done successfully.").with(getKey());

        for (TSRoot r : active)
            r.open(isReadOnly());

        if (versioning)
            enableVersioning();
    }

    @Override
    public int getDistributionFactor() {
        return 0;
    }

    @Override
    public void setHighAvailability(boolean value) {
    }

    @Override
    public boolean hasWriter(IdentityKey id) {
        synchronized (writers) {
            return writers.size() > 0;
        }
    }

    @Override
    public boolean getHighAvailability() {
        return false;
    }

//    public PDStreamReader               createReader(long time, SelectionOptions options, final EntityFilter filter) {
//        assertOpen();
//
//        final RegistryCache cache = new RegistryCache(root.getSymbolRegistry());
//
//        final DataReader reader = getDBImpl().store.createReader (options.live);
//        reader.associate (root);
//        readerCreated(reader);
//        reader.open (time, !options.reversed, options.live, filter);
//
//        return new PDStreamReader(this, reader, options.raw ?
//                new RawConsumer(cache, getTypes(), options.isRealTimeNotification()) :
//                new Consumer(cache, getTypes(), options.getTypeLoader(), getCodecFactory(options.channelQOS == ChannelQualityOfService.MIN_INIT_TIME), options.isRealTimeNotification()));
//    }

    public PDStreamReader createReader(TSRoot tsr, long time, SelectionOptions options, final EntityFilter filter) {
        assertOpen();

        final RegistryCache cache = new RegistryCache(tsr.getSymbolRegistry());

        final DataReader reader = getDBImpl().store.createReader (options.live);
        reader.associate (tsr);
        readerCreated(reader);
        reader.open (time, !options.reversed, filter);

        return new PDStreamReader(this, time, tsr, reader, options.raw ?
                new RawConsumer(cache, getTypes(), options.isRealTimeNotification()) :
                new Consumer(cache, getTypes(), options.getTypeLoader(),
                        getCodecFactory(options.channelQOS == ChannelQualityOfService.MIN_INIT_TIME), options.isRealTimeNotification()));
    }

//    @Override
//    public MessageSource<InstrumentMessage> createSource(long time, SelectionOptions options, final QuickMessageFilter filter) {
//        return createReader(time, options, filter);
//    }

//    public InstrumentMessageSource      createSource(long time, SelectionOptions options) {
//        return createSource(time, options, null, null);
//    }

//    @Override
//    public InstrumentMessageSource createSource(long time, SelectionOptions options, IdentityKey[] identities, String[] types) {
//        return new SingleChannelSource(this, options, time, identities, types);
//    }

    @Override
    public TickLoader           createLoader(LoadingOptions options) {

        Thread runner = bgRunner;
        if (runner != null && runner.isAlive() && "transform".equals(runner.getName()))
            throw new IllegalArgumentException("Cannot create loader for the [" + getKey() + "] while converting data.");

        return super.createLoader(options);
    }

    void                        writerCreated(DataWriter writer) {
        fireWriterCreated(listEntities());

        synchronized (writers) {
            writers.add(writer);
        }
    }

    void                        writerClosed(DataWriter writer) {
        if (isOpen())
            fireWriterClosed(listEntities());

        synchronized (writers) {
            writers.remove(writer);
        }
    }

    void                        readerCreated(DataReader reader) {
        synchronized (readers) {
            readers.add(reader);
        }
    }

    void                        readerClosed(DataReader reader) {
        synchronized (readers) {
            readers.remove(reader);
        }
    }

    public void                         abortBackgroundProcess() {
        BackgroundProcessInfo process = bgProcess;

        if (process != null)
            process.getMonitor().abort(null);
    }

    public BackgroundProcessInfo        getBackgroundProcess() {
        if (bgProcess != null)
            bgProcess.update();

        return bgProcess;
    }

    @Override
    public void            execute(TransformationTask task) {
        if (task instanceof SchemaUpdateTask)
            task = ((SchemaUpdateTask) task).getChanges(this);

        if (task instanceof SchemaChangeTask) {
            executeSchemaChangeTask((SchemaChangeTask) task);
        } else if (task instanceof StreamCopyTask) {
            StreamCopyTask copyTask = (StreamCopyTask) task;
            // required call to init all references
            copyTask.target = getKey();
            copyTask.invalidate(db);

            transform(copyTask);
        }
    }

    private void executeSchemaChangeTask(SchemaChangeTask changeTask) {
        // required call to init all references
        synchronized (this) {
            if (schemaChangeInProgress) {
                throw new IllegalStateException("Another schema change task is running.");
            }
            changeTask.invalidate(md);

            schemaChangeInProgress = true;
        }

        boolean needToClearChangesFlag = true;
        try {
            needToClearChangesFlag = transform(changeTask); // Returned value is "false" for background task
        } finally {
            if (needToClearChangesFlag) {
                markSchemaChangeComplete();
            }
        }
    }

    /**
     * @return true if transformation was finished (returns false for background tasks)
     */
    private boolean transform(final TransformationTask task) {
        assertFinal();
        assertWritable();

        final boolean isBackground = task.isBackground();
        final ExecutionMonitorImpl monitor = new ExecutionMonitorImpl();

        synchronized (this) {
            if (bgRunner != null && bgRunner.isAlive())
                throw new IllegalStateException("Another background task is running.");

            if (isBackground) {
                bgProcess = new BackgroundProcessInfo("transform", monitor, getKey());
                // TODO: Use common thread factory (this is needed to set affinity) and set thread name
                bgRunner = new Thread(() -> {
                    try {
                        firePropertyChanged(TickStreamProperties.BG_PROCESS);

                        transformImplByType(task, monitor);

                        bgRunner = null;

                        firePropertyChanged(TickStreamProperties.BG_PROCESS);
                    } catch (Throwable e) {
                        monitor.abort(e);
                        firePropertyChanged(TickStreamProperties.BG_PROCESS);
                        throw e;
                    } finally {
                        if (task instanceof SchemaChangeTask) {
                            markSchemaChangeComplete();
                        }
                    }
                });
                bgRunner.setName("Background-Stream-Transform-" + getKey());
                bgRunner.start();
            }
        }
        if (!isBackground) {
            transformImplByType(task, monitor);
        }
        return !isBackground;
    }

    private synchronized void markSchemaChangeComplete() {
        schemaChangeInProgress = false;
    }

    private void transformImplByType(TransformationTask task, ExecutionMonitorImpl monitor) {
        if (task instanceof StreamChangeTask) {
            runUnderLock(() -> {
                transformImpl((StreamChangeTask) task, monitor);
            });
        } else if (task instanceof SchemaChangeTask) {
            runUnderLock(() -> {
                transformImpl((SchemaChangeTask) task, monitor);
            });
        } else if (task instanceof StreamCopyTask) {
            transformImpl((StreamCopyTask) task, monitor);
        }
    }

    private void                        transformImpl(StreamChangeTask task, ExecutionMonitorImpl monitor) {

        setName(task.name);
        setDescription(task.description);
        setPeriodicity(task.periodicity);
        setHighAvailability(task.ha);

        transformImpl((SchemaChangeTask)task, monitor);
    }

    private void                        transformImpl(SchemaChangeTask task, ExecutionMonitorImpl monitor) {

        StreamMetaDataChange change = task.change;

        if (monitor != null)
            monitor.start();

        if (change.getChangeImpact() == SchemaChange.Impact.None || getTimeRange() == null) {

            // just change metadata - we have no changes
            setMetaData(change.targetType == MetaDataChange.ContentType.Polymorphic, change.getMetaData());

            if (monitor != null)
                monitor.setComplete();

            firePropertyChanged(TickStreamProperties.SCHEMA);
            onSchemaChanged(false, Long.MIN_VALUE);
        } else {
            HashMap<TSRoot, TSRoot> changes = new HashMap<TSRoot, TSRoot>();

            TSRoot[] active = getActiveRoots();
            for (TSRoot tsr : active) {
                TSRoot changed = transformImpl(tsr, task, monitor);
                if (changed != null)
                    changes.put(tsr, changed);
            }

            synchronized (roots) {
                // We assume that all spaces share same top folder
                String basePathString = root.getPath().getParentPath().getPathString();

                for (Map.Entry<TSRoot, TSRoot> entry : changes.entrySet()) {
                    TSRoot key = entry.getKey();
                    TSRoot value = entry.getValue();

                    if (roots.remove(key)) {
                        Optional<SpaceEntry> se = spaces.stream().filter(x -> x.name.equals(key.getSpace())).findFirst();
                        if (se.isPresent()) {
                            spaces.remove(se.get());
                        }
                        else {
                            LOGGER.warn("Missing location \"%s\" for PDStream \"%s\" during transformation")
                                    .with(key.getPath().getPathString()).with(getKey());
                        }

//                        //boolean success = locations.remove(getRelativePath(basePathString, key.getPath()));
//                        if (!success) {
//
//                        }

                        roots.add(value);
                        spaces.add(new SpaceEntry(value.getSpace(), getRelativePath(basePathString, value.getPath())));
                        //locations.add(getRelativePath(basePathString, value.getPath()));
                    } else if (root.equals(key)) {
                        root = value;
                        setLocation(getRelativePath(basePathString, value.getPath()));
                    } else {
                        LOGGER.warn("Missing root \"%s\" for PDStream \"%s\" during transformation")
                                .with(key.getPath().getPathString()).with(getKey());
                    }
                }
            }

            if (monitor != null && monitor.getStatus() == ExecutionStatus.Aborted)
                return;

            // save changes here
            if (monitor != null)
                monitor.setComplete();

            setMetaData(task.change.targetType == MetaDataChange.ContentType.Polymorphic, task.change.getMetaData());

            firePropertyChanged(TickStreamProperties.SCHEMA);
            firePropertyChanged(TickStreamProperties.TIME_RANGE);
            firePropertyChanged(TickStreamProperties.ENTITIES);

            if (isRemoteMetadata()) {
                setDirty(TickStreamPropertiesEnum.SCHEMA);
                setDirty(TickStreamPropertiesEnum.TIME_RANGE);
                setDirty(TickStreamPropertiesEnum.ENTITIES);
            }

            onSchemaChanged(true, Long.MIN_VALUE);

            // cleanup
            for (TSRoot r : changes.keySet()) {
                AbstractPath path = r.getPath();
                r.format();
                r.forceClose();

                try {
                    FSUtils.removeRecursive(path, true, null);
                } catch (IOException iox) {
                    TickDBImpl.LOG.warn("Unable to delete path: %s. Error %s").with(path).with(iox);
                }
            }

            changes.clear();
        }
    }

    private TSRoot                        transformImpl(TSRoot root, SchemaChangeTask task,
                                                        ExecutionMonitorImpl monitor) {

        long[] range = getTimeRange();

        SchemaConverter converter = new SchemaConverter(task.change);

        MessageChannel<InstrumentMessage>   channel = null;
        PDStreamReader                      reader = null;

        String rootPath = root.getPathString();
        AbstractPath newPath = root.getFileSystem().createPath(rootPath.endsWith(TRANSFORMED_PATH_SUFFIX) ?
                rootPath.substring(0, rootPath.length() - TRANSFORMED_PATH_SUFFIX.length()) : rootPath + TRANSFORMED_PATH_SUFFIX);

        try {
            TSRoot newRoot = getDBImpl().store.createRoot(root.getSpace(), newPath);
            newRoot.format();

            reader = createReader(root, Long.MIN_VALUE, new SelectionOptions(true, false), EntityFilter.ALL);

            DataWriter writer = getDBImpl().store.createWriter();
            writer.associate(newRoot);

            channel = new WriterChannel(writer,
                    new RawProducer(task.change.getMetaData().getTopTypes()),
                    new RegistryCache(newRoot.getSymbolRegistry()));

            int count = 0;

            while (reader.next()) {

                if (monitor != null && monitor.getStatus() == ExecutionStatus.Aborted)
                    return null;

                RawMessage msg = (RawMessage) reader.getMessage();
                try {
                    RawMessage result = converter.convert(msg);
                    if (result != null) {
                        channel.send(result);
                        count++;
                    } else {
                        System.out.println("Cannot convert message: " + msg);
                    }

                } catch (IllegalArgumentException e) {
                    TickDBImpl.LOG.trace("[%s] convert: dropping message: %s. Error: %s").with(getKey()).with(msg).with(e);
                }

                if (count % 100 == 0) {
                    double progress = ((double) (msg.getTimeStampMs() - range[0])) / (range[1] - range[0]);
                    if (monitor != null)
                        monitor.setProgress(progress);
                }
            }

            reader = Util.close(reader);
            channel = Util.close(channel);

            if (monitor != null)
                monitor.setProgress(1);

            return newRoot;

        } catch (Throwable e) {
            if (monitor != null)
                monitor.abort(e);

            Util.close(channel);
            Util.close(reader);

            try {
                FSUtils.removeRecursive(newPath, true, null);
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            }

            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            Util.close(channel);
            Util.close(reader);
        }
    }

       private long[] recalculateTimeRange (DXTickStream[] sources){
        long[] range = null;

        for (int i = 0; i < sources.length; i++) {
            long[] timeRange = sources[i].getTimeRange();

            if (range == null && timeRange != null) {
                range = timeRange;
            }
            else if (timeRange != null) {
                range[0] = Math.min(range[0], timeRange[0]);
                range[1] = Math.max(range[1], timeRange[1]);
            }
        }

        return range;
    }

    private void transformImpl(StreamCopyTask task, ExecutionMonitorImpl monitor) {
        monitor.start();

        SchemaConverter converter = new SchemaConverter(task.change);

        DXTickStream[] sources = task.getSources(db);
        CharSequence[] entities = task.subscribedEntities;
        String[] types = task.subscribedTypeNames;


        long startTime = task.startTime;
        long endTime = task.endTime;

        if (startTime == Long.MIN_VALUE || endTime == Long.MAX_VALUE) {
            long[] range = recalculateTimeRange(sources);

            if (range == null) {
                monitor.setComplete();
                return;
            }

            if (startTime == Long.MIN_VALUE){
                startTime = range[0];
            }

            if (endTime == Long.MAX_VALUE){
                endTime = range[1];
            }
        }

        TickCursor cursor = db.select(startTime, new SelectionOptions(true, false), types, entities, sources);
        TickLoader loader = createLoader(new LoadingOptions(true));

        try {

            while (cursor.next()) {

                if (monitor.getStatus() == ExecutionStatus.Aborted)
                    return;

                RawMessage msg = (RawMessage) cursor.getMessage();
                RawMessage result = converter.convert(msg);
                if (msg.getTimeStampMs() > endTime)
                    break;

                if (result != null)
                    loader.send(result);

                double progress = ((double) (msg.getTimeStampMs() - startTime)) / (endTime - startTime);
                monitor.setProgress(progress);
            }

            Util.close(cursor);
            Util.close(loader);
            // save changes here
            monitor.setComplete();
        } catch (Throwable e) {
            monitor.abort(e);
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            Util.close(cursor);
            Util.close(loader);

            if (monitor.getStatus() == ExecutionStatus.Running || monitor.getStatus() == ExecutionStatus.None)
                monitor.setComplete();
        }
    }



    @Override
    public long                 getSizeOnDisk() {

        long size = 0;
        TSRoot[] active = getActiveRoots();
        for (TSRoot r : active)
            size += getSizeOnDisk(r);

        return size;
    }

    public long                     getSizeOnDisk(TSRoot root) {
        if (FSUtils.isDistributedFS(root.getFileSystem()))
            return root.getPath().length();

        ArrayList<TSRef> files = new ArrayList<TSRef>();
        root.selectTimeSlices(null, null, files);

        long total = 0;
        for (TSRef ref : files) {
            AbstractPath path = root.getFileSystem().createPath(ref.getPath());
            if (path.exists())
                total += path.length();
        }

        return total;
    }

    /**
     * Deletes stream (including stream metadata).
     */
    @Override
    public synchronized void        delete() {
        runUnderLock(() -> {
            delete(true);
        });
    }

    @Override
    public synchronized void        setPolymorphic (RecordClassDescriptor ... cds) {
        runUnderLock(() -> {
            super.setPolymorphic(cds);
        });
    }

    @Override
    public synchronized void        setFixedType (RecordClassDescriptor cd) {
        runUnderLock(() -> {
            super.setFixedType(cd);
        });
    }

    @Override
    public synchronized void setPeriodicity(Periodicity p) {
        runUnderLock(() -> {
            super.setPeriodicity(p);
        });
    }

    /**
     * Executes provided stream operation with remote lock (for remote stream).
     */
    private synchronized void runUnderLock(Runnable operation) {
        boolean remoteMetadata = isRemoteMetadata();
        if (!remoteMetadata) {
            // This is not a remote stream. Just execute the operation
            operation.run();
        } else {
            assert metaFlushMode == StreamMetaFlushMode.PROHIBITED;
            metaFlushMode = StreamMetaFlushMode.DELAY; // Delay file flush till the operation is complete.
            String key = getKey();
            TickDBImpl.RemoteStreamLock streamLock = getDBImpl().getRemoteStreamLock(key);
            try {
                // We must update stream data to ensure that we operate on the most-recent version of the stream.
                getDBImpl().syncRemoteStreams();
                if (!key.equals(getKey())) {
                    throw new IllegalArgumentException("Stream key was changed remotely: " + key);
                }

                operation.run();

                if (isDirty) {
                    saveToFile(true);
                    if (!changedProperties.isEmpty()) {
                        getDBImpl().notifyRemoteStreamUpdated(key, changedProperties);
                        changedProperties.clear();
                    }
                    isDirty = false;
                }
            } finally {
                metaFlushMode = StreamMetaFlushMode.PROHIBITED;
                if (isDirty) {
                    // Still dirty? This means we failed to flush changes.
                    TickDBImpl.LOG.warn("Failed to flush changes for teh stream: "+ key);

                    // TODO: Consider if it's OK to clear dirty flags. Leaving them means inconsistent sate. Clearing them may hide the problem but not solve it.
                    isDirty = false;
                    changedProperties.clear();
                }
                streamLock.releaseSilent();
            }
        }
    }

    /**
     * @return remote lock or lock stub (for local streams)
     */
    @Deprecated
    private TickDBImpl.RemoteStreamLock getRemoteLock() {
        return getDBImpl().getRemoteStreamLockOrStub(getKey(), isRemoteMetadata());
    }

    /**
     * Saves stream metadata (delayed for local and immediate save for remote).
     */
    private void writeMetadata(boolean holdsRemoteStreamLock) {
        if (isRemoteMetadata()) {
            // Synchronously write data to file
            saveToFile(holdsRemoteStreamLock);
        } else {
            // Schedule delayed write
            setDirty();
        }
    }

    /**
     * Deletes this stream without deleting stream data and metadata files.
     * This method is supposed to be used by remote stream synchronization mechanism to delete local copies of stream.
     */
    void deleteLocalStream() {
        delete(false);
    }

    /**
     * Deletes local stream data. No publishing of the fact to remote storage (if any).
     *
     * @param deleteFiles if true then stream data and metadata files will be deleted
     */
    private void    delete(boolean deleteFiles) {
        assertWritable();

        abortBackgroundProcess();

        LOGGER.info("Delete stream [" + getKey() + "]");

        if (deleteFiles) {
            // Delete data files
            TSRoot[] active = getActiveRoots();
            for (TSRoot tsr : active)
                tsr.delete();
        }

        super.close();

        getDBImpl ().streamDeleted (getKey());
        onDelete ();

        if (deleteFiles) {
            // Delete stream metadata files
            if (isRemoteMetadata()) {
                try {
                    metadataLocation.getPath().deleteExisting();
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to delete remote metadata", e);
                }
                getDBImpl().notifyRemoteStreamDeleted(getKey());
            } else {
                IOUtil.deleteUnchecked(file.getParentFile());
            }
        }
    }

    public synchronized void        cleanup() {
        TickDBImpl.LOG.trace("Closing writers/readers for the stream: %s").with(getKey());

        abortBackgroundProcess();
        // wait until stopped?

        synchronized (readers) {
            for (DataReader r : readers)
                r.close();

            readers.clear();
        }

        synchronized (writers) {
            for (DataWriter w : writers)
                w.close();

            writers.clear();
        }
    }

    @Override
    public synchronized void         close () {
        // first close writers/readers
        cleanup();

        Throwable firstException = null;

        TSRoot[] active = getActiveRoots();
        for (TSRoot tsr : active) {
            if (tsr != null) { // We might get null here if the stream was not yet open
                try {
                    tsr.close();
                } catch (Throwable e) {
                    if (firstException == null) {
                        firstException = e;
                    }
                }
            }
        }

        if (firstException != null) {
            ExceptionUtils.rethrow(firstException);
        }

        super.close();
    }

    /**
     * Applies changes from remote stream.
     * @param changeSet changes on remote stream
     * @param loadedStream loaded remote stream data
     */
    synchronized void updateFromRemote(EnumSet<TickStreamPropertiesEnum> changeSet, PDStream loadedStream) {
        assert isRemoteMetadata();

        StreamMetaFlushMode previousFlushMode = this.metaFlushMode;
        // We don't want to apply any changes because they already applied.
        this.metaFlushMode = StreamMetaFlushMode.DISABLED;
        try {
            if (changeSet.contains(TickStreamPropertiesEnum.LOCATION)) {
                setLocation(loadedStream.getDataLocation());
                updateLocationsFromRemote(loadedStream.getDataLocation(), loadedStream.spaces);
                // TODO: Notify clients?
            }

            if (changeSet.contains(TickStreamPropertiesEnum.SCHEMA)) {
                super.setMetaData(loadedStream.isPolymorphic(), loadedStream.getMetaData());
            }

            if (changeSet.contains(TickStreamPropertiesEnum.NAME)) {
                super.setName(loadedStream.getName());
            }
            if (changeSet.contains(TickStreamPropertiesEnum.DESCRIPTION)) {
                super.setDescription(loadedStream.getDescription());
            }
            if (changeSet.contains(TickStreamPropertiesEnum.PERIODICITY)) {
                super.setPeriodicity(loadedStream.getPeriodicity());
            }
            /*
            if (changeSet.contains(TickStreamPropertiesEnum.HIGH_AVAILABILITY)) {
                setHighAvailability(loadedStream.getHighAvailability());
            }
            */
        } finally {
            this.metaFlushMode = previousFlushMode;
        }
    }

    // TODO: Check if implemented correctly
    private void updateLocationsFromRemote(String dataLocation, List<SpaceEntry> newLocations) {
        AbstractFileSystem fs;
        try {
            fs = FSFactory.create(dataLocation);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        AbstractPath newRootPath = fs.createPath(dataLocation);

        root = getDBImpl().store.createRoot(null, newRootPath);
        root.open(isReadOnly);
        setLocation(dataLocation);

        roots.clear();
        spaces.clear();

        for (SpaceEntry se : newLocations) {
            AbstractPath newPath = fs.createPath(dataLocation);
            TSRoot root = getDBImpl().store.createRoot(getSpaceName(newPath), newPath);
            root.open(isReadOnly);
            roots.add(root);
            spaces.add(new SpaceEntry(se.name, se.path));
        }
    }

    @Override
    public String[]             listSpaces() {
        TSRoot[] roots = getActiveRoots();

        String[] results = new String[roots.length];
        for (int i = 0; i < roots.length; i++) {
            TSRoot tsr = roots[i];
            String space;
            if (tsr == this.root) {
                space = DEFAULT_SPACE;
            } else {
                space = tsr.getSpace();
            }
            assert space != null;
            results[i] = space;
        }

        return results;
    }

    @Override
    public void             deleteSpaces(String ... names) {
        HashSet<String> set = new HashSet<String>(Arrays.asList(names));
        ArrayList<TSRoot> deleted = new ArrayList<>();

        synchronized (roots) {
            for (int i = roots.size() - 1; i >= 0; i--) {
                TSRoot tsr = roots.get(i);

                if (set.contains(tsr.getSpace())) {
                    deleted.add(tsr);

                    // remove from collections
                    roots.remove(i);
                    Optional<SpaceEntry> se = spaces.stream().filter(x -> x.name.equals(tsr.getSpace())).findFirst();
                    assert se.isPresent();
                    se.ifPresent(e -> spaces.remove(e));
                }
            }
        }
        long time = TimeKeeper.currentTime;

        // delete roots outside of lock
        for (TSRoot r : deleted) {
            String space = r.getSpace();
            r.delete();

            LOGGER.info("Stream [" + getKey() + "]: space [" + space + "] deleted.");

//            if (versioning) {
//                SpaceDeletedMessage msg = new SpaceDeletedMessage();
//                msg.setSymbol(""); // @SYSTEM
//                msg.setTimeStampMs(time);
//                msg.setInstrumentType(InstrumentType.SYSTEM);
//                msg.setName(space);
//                addSystemMessage(msg);
//            }
        }

        setDirty(true);
    }

    @Override
    public void             renameSpace(String newName, String oldName) {
        String key = getKey();

        synchronized (roots) {
            TSRoot old = getOrCreateRoot(oldName, false);

            if (old == null)
                throw new UnknownSpaceException("Space '" + oldName + "' does not exist in the stream [" + key + "]");

            TSRoot newSpace = getOrCreateRoot(newName, false);

            if (newSpace != null)
                throw new IllegalStateException("Space '" + newName + "' already exist in the stream [" + key + "]");

            old.setSpace(newName);

            Optional<SpaceEntry> se = spaces.stream().filter(x -> x.name.equals(oldName)).findFirst();
            se.ifPresent(spaceEntry -> spaceEntry.name = newName);
        }

//        if (versioning) {
//            SpaceRenamedMessage msg = new SpaceRenamedMessage();
//            msg.setSymbol(""); // @SYSTEM
//            msg.setTimeStampMs(TimeKeeper.currentTime);
//            msg.setInstrumentType(InstrumentType.SYSTEM);
//            msg.setName(oldName);
//            msg.setNewName(newName);
//            addSystemMessage(msg);
//        }

        setDirty(true);

        LOGGER.info("Stream [" + key + "]: Renamed space [" + oldName + "] to [" + newName + "]");
    }

    private String getSpaceName(String location) throws IOException  {
        AbstractFileSystem fs = FSFactory.create(location);
        return getSpaceName(fs.createPath(location));
    }

    private String getSpaceName(AbstractPath path) {
        String folderName = path.getName();

        try {
            return SimpleStringCodec.DEFAULT_INSTANCE.decode(folderName);
        } catch (Exception ignore) {
        }
        // If we got here this mean that we the codec failed to decode string.
        // We assume that this can happen if we added OR removed "_" as result of stream transformation

        if (folderName.endsWith(TRANSFORMED_PATH_SUFFIX)) {
            folderName = folderName.substring(0, folderName.length() - TRANSFORMED_PATH_SUFFIX.length());
        } else {
            folderName = folderName + TRANSFORMED_PATH_SUFFIX;
        }
        return SimpleStringCodec.DEFAULT_INSTANCE.decode(folderName);
    }

    @Override
    public IdentityKey[]     listEntities(String space) {
        TSRoot tsr = getRootBySpaceName(space);
        if (root != null) {
            HashSet<IdentityKey> ids = new HashSet<IdentityKey>();
            listEntities(tsr, ids);
            return ids.toArray(new IdentityKey[ids.size()]);
        }

        return null;
    }

    @Override
    public long[]           getTimeRange(String space) {
        TSRoot tsr = getRootBySpaceName(space);

        TimeRange out = new TimeRange();
        tsr.getTimeRange(out);

        return out.isUndefined() ? null : new long[]{TimeStamp.getMilliseconds(out.from), TimeStamp.getMilliseconds(out.to)};
    }

    protected void saveToOutputStream(OutputStream out) throws IOException, JAXBException {
        OutputStream bout = createBufferedOutput(out);

        // lock order: this->roots (spaces)
        synchronized (roots) {
            IOUtil.marshall(TickDBJAXBContext.createMarshaller(), bout, this);
        }
    }
}