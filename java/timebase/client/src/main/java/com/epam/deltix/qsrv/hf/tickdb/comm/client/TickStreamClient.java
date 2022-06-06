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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.IdentityKeyComparator;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.pub.TimeRange;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.VSChannel;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 *
 */
class TickStreamClient implements DXTickStream {
    final DXRemoteDB                        conn;

    public final static  long               TIMEOUT = 1000;

    private String                          key;

    // guarded by this
    private ClientLock                      lock;
    final StreamOptions                     options;
    int                                     formatVersion = 4;

    private final InstrumentToObjectMap<EntityTimeRange>
                                            entities = new InstrumentToObjectMap<EntityTimeRange>();

    final StreamRange                       timeRange = new StreamRange();
    BackgroundProcessInfo                   bgProcess;

    private static final String             CACHE_ENTITIES_PROP = "TimeBase.client.cacheEntities";
    private static final boolean            CACHE_ENTITIES = Boolean.valueOf(System.getProperty(CACHE_ENTITIES_PROP, "true"));

    TickStreamClient (TickDBClient conn, String key, StreamOptions options) {
        this.key = key;
        this.conn = conn;
        this.options = options;
    }

    public TickStreamClient(DXRemoteDB db, TickStreamClient delegate) {
        this.key = delegate.getKey();
        this.conn = db;
        this.options = delegate.options;
    }

    VSChannel                       connect() throws IOException {
        DXRemoteDB db = (DXRemoteDB) getDB();
        return db.connect();
    }

    void                        checkResponse (VSChannel ds) throws IOException {
        TickDBClient.checkResponse(ds);
    }

    public DXTickDB                 getDB () {
        return (conn);
    }

    public String                   getKey () {
        return (key);
    }

    public StreamScope              getScope () {
        return options.scope;
    }

    private VSChannel               sendRequest (int req) throws IOException {
        final VSChannel            ds = connect ();
        final DataOutputStream      out = ds.getDataOutputStream ();

        out.writeInt (req);
        out.writeUTF (key);
        out.flush ();

        checkResponse(ds);
            
        return (ds);
    }
    
//    private String                  getStringProperty (int req) {
//        VSChannel                  ds = null;
//
//        try {
//            ds = sendRequest (req);
//
//            return (TDBProtocol.readNullableString (ds.getDataInputStream ()));
//        } catch (IOException iox) {
//            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
//        } finally {
//            Util.close (ds);
//        }
//    }
//
//    private int                     getIntProperty (int req) {
//        VSChannel                  ds = null;
//
//        try {
//            ds = sendRequest (req);
//
//            return (ds.getDataInputStream().readInt ());
//        } catch (IOException iox) {
//            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
//        } finally {
//            Util.close (ds);
//        }
//    }
//
    private long                    getLongProperty (int req) {
        VSChannel                  ds = null;

        try {
            ds = sendRequest (req);

            return (ds.getDataInputStream().readLong ());
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }
//
//
//    private void                    sendSimpleRequest (int req) {
//        VSChannel                  ds = null;
//
//        try {
//            ds = sendRequest (req);
//        } catch (IOException iox) {
//            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
//        } finally {
//            Util.close (ds);
//        }
//    }
//
    private void                    setStringProperty (int req, String value) {

        assertWritable();
        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();
            out.writeInt (req);
            out.writeUTF (key);
            TDBProtocol.writeNullableString(value, out);
            writeLock(out);
            out.flush ();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

     private void                    setIntProperty (int req, int value) {
        assertWritable();

        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();
            out.writeInt (req);
            out.writeUTF (key);
            out.writeInt(value);
            writeLock(out);
            out.flush ();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public synchronized StreamOptions            getStreamOptions() {

        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        StreamOptions so =
                new StreamOptions (options.scope, getName(), getDescription(), getDistributionFactor());

        so.setMetaData(options.isPolymorphic(), new RecordClassSet(options.getMetaData()));
        so.periodicity = getPeriodicity();
        so.duplicatesAllowed = options.duplicatesAllowed;
        so.highAvailability = getHighAvailability();
        so.unique = options.unique;
        so.owner = options.owner;

        BufferOptions bufferOptions = options.bufferOptions;
        if (bufferOptions != null) {
            so.bufferOptions = new BufferOptions();
            so.bufferOptions.lossless = bufferOptions.lossless;
            so.bufferOptions.maxBufferSize = bufferOptions.maxBufferSize;
            so.bufferOptions.initialBufferSize = bufferOptions.initialBufferSize;
            so.bufferOptions.maxBufferTimeDepth = bufferOptions.maxBufferTimeDepth;
        }

        return so;
    }

    public String                   getName () {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.NAME);
        }

        return options.name;
    }

    public String                   getDescription () {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.DESCRIPTION);
        }
        return options.description;
    }

    public String                   getOwner() {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.OWNER);
        }
        return options.owner;
    }

    public int                      getDistributionFactor () {
        return (options.distributionFactor);
    }

    public long                     getTypeVersion () {
        return (getLongProperty (TDBProtocol.REQ_GET_STREAM_TYPE_VERSION));
    }

    @Override
    public String       describe() {

        StringBuilder sb = new StringBuilder();
        VSChannel                  ds = null;

        try {
            ds = sendRequest (TDBProtocol.REQ_DESCRIBE);
            TDBProtocol.readHugeString(ds.getDataInputStream(), sb);
            return sb.toString();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    private long[]                    loadTimeRange(IdentityKey ... ids) {
        try (VSChannel ds = connect()) {
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_TIME_RANGE);
            out.writeUTF (key);
            TDBProtocol.writeInstrumentIdentities (ids, out);
            out.flush ();

            checkResponse(ds);

            long                    from = ds.getDataInputStream().readLong ();

            if (from == Long.MAX_VALUE)
                return (null);

            long                    to = ds.getDataInputStream().readLong ();

            return (new long [] { from, to });
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    private TimeInterval[]  loadTimeRanges(IdentityKey... ids) {
        TimeInterval[] ranges = new TimeInterval[ids.length];

        try (VSChannel ds = connect()) {
            final DataOutputStream  out = ds.getDataOutputStream ();
            final DataInputStream in = ds.getDataInputStream();

            out.writeInt (TDBProtocol.REQ_LIST_TIME_RANGE);
            out.writeUTF (key);
            TDBProtocol.writeInstrumentIdentities(ids, out);
            out.flush ();

            checkResponse(ds);

            for (int i = 0; i < ids.length; i++) {
                long[] values = TDBProtocol.readTimeRangeLong(in);
                if (values != null)
                    ranges[i] = new TimeRange(values[0], values[1]);
            }
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }

        return ranges;
    }

    private IdentityKey[] loadEntities() {
        try (VSChannel ds = connect ()) {
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_LIST_ENTITIES);
            out.writeUTF (key);
            out.flush ();

            checkResponse (ds);

            return (TDBProtocol.readInstrumentIdentities (ds.getDataInputStream()));
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    private boolean     updateStreamProperties(int ...  properties) {
        boolean[] states = new boolean[properties.length];

        // complex code to overcome possible JIT optimization, we need to check each property
        synchronized (this) {
            for (int i = 0; i < properties.length; i++)
                states[i] = conn.getSession().getStreamProperty(key, properties[i]);
        }

        for (boolean state : states) {
            if (state)
                return true;
        }

        return false;
    }

    public long []                      getTimeRange (IdentityKey... ids) {

        if (!CACHE_ENTITIES)
            return loadTimeRange(ids);

        long time = TimeKeeper.currentTime;

        EntityTimeRange range = new EntityTimeRange();
        ArrayList<IdentityKey> update = new ArrayList<IdentityKey>();

        boolean changed;
        synchronized (this) {
            changed = updateStreamProperties(TickStreamProperties.ENTITIES, TickStreamProperties.TIME_RANGE);
            if (changed)
                timeRange.invalidate = changed;
        }

        synchronized (entities) {

            if (changed) {
                for (Map.Entry<IdentityKey, EntityTimeRange> entry : entities.entrySet()) {
                    if (entry == null || entry.getValue() == null)
                        continue;

                    entry.getValue().invalidate = true;
                }
            }

            for (int i = 0; i < ids.length; i++) {
                EntityTimeRange r = entities.get(ids[i]);

                if (changed) {
                    update.add(ids[i]);
                } else if (r != null && r.invalidate) {
                    update.add(ids[i]);
                } else if (r != null) {
                    if (r.writing && time - r.updated > TIMEOUT)
                        update.add(ids[i]);
                    else
                        range.union(r);
                }
            }

            if (ids.length == 0) {

                // load every time
                // timeRange.set(loadTimeRange());

                if (timeRange.invalidate || timeRange.writers > 0) {
                    if (time - timeRange.updated > TIMEOUT) {
                        timeRange.set(loadTimeRange());
                        timeRange.updated = time;
                    }
                }

                range.union(timeRange);
            }
        }

        if (update.size() > 0) {
            TimeInterval[] ranges = loadTimeRanges(update.toArray(new IdentityKey[update.size()]));
            synchronized (entities) {
                for (int i = 0; i < update.size(); ++i) {
                    IdentityKey id = update.get(i);
                    EntityTimeRange r = entities.get(id);
                    if (r != null) {
                        r.set(ranges[i]);
                        r.updated = time;
                        range.union(r);
                    }
                }
            }
        }

        return range.toArray();
    }

    @Override
    public TimeInterval[]           listTimeRange(IdentityKey... ids) {

        if (!CACHE_ENTITIES)
            return loadTimeRanges(ids);

        long time = TimeKeeper.currentTime;

        ArrayList<IdentityKey> update = new ArrayList<IdentityKey>();

        boolean changed = updateStreamProperties(TickStreamProperties.ENTITIES, TickStreamProperties.TIME_RANGE);

        synchronized (entities) {
            if (changed) {
                for (Map.Entry<IdentityKey, EntityTimeRange> entry : entities.entrySet()) {
                    if (entry == null || entry.getValue() == null)
                        continue;

                    entry.getValue().invalidate = true;
                }
            }

            for (int i = 0; i < ids.length; i++) {
                EntityTimeRange r = entities.get(ids[i]);

                if (changed) {
                    update.add(ids[i]);
                } else if (r != null && r.invalidate) {
                    update.add(ids[i]);
                } else if (r != null) {
                    if (r.writing && time - r.updated > TIMEOUT)
                        update.add(ids[i]);
                }
            }
        }

        if (update.size() > 0) {
            TimeInterval[] ranges = loadTimeRanges(update.toArray(new IdentityKey[update.size()]));
            synchronized (entities) {
                for (int i = 0; i < update.size(); ++i) {
                    IdentityKey id = update.get(i);
                    EntityTimeRange r = entities.get(id);
                    if (r != null) {
                        r.set(ranges[i]);
                        r.updated = time;
                    }
                }
            }
        }

        TimeInterval[] ranges = new TimeInterval[ids.length];

        synchronized (entities) {

            for (int i = 0; i < ids.length; i++) {
                EntityTimeRange r = entities.get(ids[i]);
                if (r != null)
                    ranges[i] = new TimeRange(r.from, r.to);
            }
        }

        return ranges;
    }

    void                            setWriteMode(IdentityKey id, boolean writing) {
        synchronized (entities) {
            EntityTimeRange range = entities.get(id);
            if (range == null) {
                range = new EntityTimeRange();
                entities.put(id, range);
            }

            range.writing = writing;
            timeRange.writers += (writing ? 1 : -1);
        }
    }

    void                            setWriteMode(boolean writing) {
        synchronized (entities) {
            timeRange.invalidate = true;
        }
    }

    void                            setEntities(HashMap<IdentityKey, EntityTimeRange> ids, long writers) {
        if (!CACHE_ENTITIES)
            return;

        timeRange.invalidate = true;
        timeRange.writers = writers;

        synchronized (entities) {

            for (Map.Entry<IdentityKey, EntityTimeRange> id : ids.entrySet()) {
                entities.put(id.getKey(), id.getValue());
            }

            entities.keySet().removeIf(id -> !ids.containsKey(id));
        }
    }

    public IdentityKey []    listEntities () {
        if (!CACHE_ENTITIES)
            return loadEntities();

        final TreeSet<IdentityKey> set =
                new TreeSet <IdentityKey> (
                        IdentityKeyComparator.DEFAULT_INSTANCE
                );

        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.ENTITIES);
        }

        synchronized (entities) {
            set.addAll(entities.keySet());
        }
        return set.toArray(new IdentityKey[set.size()]);
    }

    @Override
    public void renameInstruments(IdentityKey[] from, IdentityKey[] to) {
        VSChannel                  ds = null;

        try {
            ds = conn.connect ();
            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_RENAME_INSTRUMENTS);
            out.writeUTF(key);
            TDBProtocol.writeInstrumentIdentities(from, out);
            TDBProtocol.writeInstrumentIdentities(to, out);
            writeLock(out);
            out.flush();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Override
    public TickCursor               select(
            long time,
            SelectionOptions options,
            String[] types, IdentityKey[] entities)
    {
        return TickCursorClientFactory.create(conn, options, time, null, null, entities, types, getAeronContext(), this);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols) {
        return TickCursorClientFactory.create(conn, options, time, Long.MAX_VALUE, null, null, symbols, types, getAeronContext(), this);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types) {
        return TickCursorClientFactory.create(conn, options, time, null, null, types, getAeronContext(), this);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options) {
        return TickCursorClientFactory.create(conn, options, time, null, null, getAeronContext(), this);
    }

    @Override
    public MessageSource<InstrumentMessage> selectMulticast(boolean raw) {
        return TickCursorClientFactoryMulticast.create(conn, this, raw, getAeronContext());
    }
    
    public TickCursor               createCursor (SelectionOptions options) {
        long time = options != null && options.reversed ? Long.MIN_VALUE : Long.MAX_VALUE;
        return TickCursorClientFactory.create(conn, options, time, null, null, new IdentityKey[0], null, getAeronContext(), this);
    }

    public void                     setDescription (String value) {
        setStringProperty(TDBProtocol.REQ_SET_STREAM_DESCR, value);
        options.description = value;
    }

    public void                     setOwner(String value) {
        setStringProperty(TDBProtocol.REQ_SET_STREAM_OWNER, value);
        options.owner = value;
    }

    public void                     setName (String value) {
        setStringProperty(TDBProtocol.REQ_SET_STREAM_NAME, value);
        options.name = value;
    }

    @Override
    public void                     setHighAvailability(boolean value) {
        setIntProperty(TDBProtocol.REQ_SET_STREAM_HA, value ? 1 : 0);
        options.highAvailability = value;
    }

    @Override
    public boolean                  getHighAvailability() {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.HIGH_AVAILABILITY);
        }
        return options.highAvailability;
    }

    public void                     setTargetNumFiles (int value) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    private void                    setType (boolean polymorphic, RecordClassDescriptor ... cds)  {
        assertWritable();

        RecordClassSet  tmd = new RecordClassSet ();
        tmd.addContentClasses (cds);

        VSChannel                  ds = null;
        
        try {
            ds = connect();
            
            final DataOutputStream  out = ds.getDataOutputStream ();
            
            out.writeInt (TDBProtocol.REQ_SET_STREAM_TYPE);
            out.writeUTF (key);
            out.writeBoolean (polymorphic);
            TDBProtocol.writeClassSet (out, tmd);
            writeLock(out);
            out.flush ();
            
            checkResponse(ds);

            options.setMetaData(polymorphic, tmd);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public void                     setPolymorphic (
        RecordClassDescriptor ...               cds
    )  
    {
        setType (true, cds);
    }

    synchronized RecordClassSet     getMetaData() {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);
        return options.getMetaData();
    }

//    private void                    refreshType () {
//        assert Thread.holdsLock (this);
//
//        long                        version = getTypeVersion ();
//
//        if (version != mdVersion) {
//            VSChannel                  ds = null;
//
//            try {
//                ds = sendRequest (TDBProtocol.REQ_GET_STREAM_TYPE);
//
//                DataInputStream in = ds.getDataInputStream();
//                isFixed = in.readBoolean ();
//                md = TDBProtocol.readClassSet (in);
//                mdVersion = version;
//            } catch (IOException iox) {
//                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
//            } finally {
//                Util.close (ds);
//            }
//        }
//    }

    public synchronized void                  setFixedType (RecordClassDescriptor cd)  {
        setType (false, cd);
    }

    public synchronized RecordClassDescriptor getFixedType () {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        if (!options.isFixedType())
            return (null);

        RecordClassSet md = options.getMetaData();
        return (md.getNumTopTypes() == 0 ? null : md.getTopType (0));
    }

    public synchronized RecordClassDescriptor [] getPolymorphicDescriptors () {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        if (options.isFixedType())
            return (null);

        return options.getMetaData().getTopTypes();
    }

    public synchronized ClassDescriptor [] getAllDescriptors () {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        return options.getMetaData().getClassDescriptors();
    }
    
    public synchronized boolean    isFixedType () {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        return options.isFixedType();
    }

    public synchronized boolean    isPolymorphic () {
        conn.getSession().getStreamProperty(key, TickStreamProperties.SCHEMA);

        return !options.isFixedType();
    }
        
    public TickLoader               createLoader () {
        return (createLoader (null));
    }        
    
    public TickLoader               createLoader (LoadingOptions options) {
        assertWritable();
        return TickLoaderClientFactory.create(this, options, getAeronContext());
    }

    public final void                   assertWritable () {
        if (!conn.isOpen ())
            throw new IllegalStateException ("Database is not open");

        if (conn.isReadOnly())
            throw new IllegalStateException ("Database is open in read-only mode");
    }
    
    public void                     delete () {
        assertWritable();

        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt(TDBProtocol.REQ_DELETE_STREAM);            
            out.writeUTF(key);
            writeLock(out);
            out.flush();

            checkResponse(ds);
            //conn.deleted (key);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
        
        conn.getSession().delete(key);
    }

     public void                    rename(String key) {
        assertWritable();

        conn.getSession().rename(this.key, key);

        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt(TDBProtocol.REQ_RENAME_STREAM);
            out.writeUTF(this.key);
            out.writeUTF(key);
            writeLock(out);
            out.flush();

            checkResponse(ds);

            this.key = key; // change key after successful change
            
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public void                     truncate(long time, IdentityKey... ids) {
        assertWritable();

        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_TRUNCATE_DATA);
            out.writeUTF (key);
            out.writeLong (time);
            TDBProtocol.writeInstrumentIdentities (ids, out);
            writeLock(out);
            out.flush ();

            checkResponse(ds);

            setWriteMode(false);

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public void                     clear(IdentityKey... ids) {

        assertWritable();

        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.ENTITIES);
        }

        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_CLEAR_DATA);
            out.writeUTF (key);
            TDBProtocol.writeInstrumentIdentities (ids, out);
            writeLock(out);
            out.flush ();

            checkResponse(ds);

            synchronized (entities) {
                if (ids.length == 0)
                    entities.clear();

                for (IdentityKey id : ids)
                    entities.remove(id);
            }

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }
    
    public  void                    execute(TransformationTask task) {
        VSChannel              ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_RUN_TASK);
            out.writeUTF (key);
            TDBProtocol.writeTransformationTask(task, out);
            writeLock(out);
            out.flush ();

            // schema may change
            if (task instanceof SchemaChangeTask)
                conn.getSession().resetProperty(key, TickStreamProperties.SCHEMA);

            // reset bg process
            conn.getSession().resetProperty(key, TickStreamProperties.BG_PROCESS);

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public void                 delete(TimeStamp from, TimeStamp to, IdentityKey... ids) {
        assertWritable();

        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_DELETE_RANGE);
            out.writeUTF (key);
            writeLock(out);
            out.writeLong (from.getNanoTime());
            out.writeLong (to.getNanoTime());
            TDBProtocol.writeInstrumentIdentities(ids, out);

            out.flush ();

            checkResponse(ds);
            setWriteMode(false);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public void                     purge(long time) {
        assertWritable();

         VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_PURGE_STREAM);
            out.writeUTF (key);
            out.writeLong (time);
            writeLock(out);
            out.flush ();

            conn.getSession().resetProperty(key, TickStreamProperties.BG_PROCESS);

            checkResponse(ds);

            setWriteMode(false);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public void purge(long time, String space) {
        assertSupportsStreamSpaces();

        assertWritable();

        VSChannel ds = null;

        try {
            ds = connect();
            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_PURGE_STREAM_SPACE);
            out.writeUTF(key);
            out.writeLong(time);
            out.writeUTF(space);
            writeLock(out);
            out.flush();

            conn.getSession().resetProperty(key, TickStreamProperties.BG_PROCESS);

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    public BackgroundProcessInfo    getBackgroundProcess() {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.BG_PROCESS);
        }

        if (bgProcess == null || bgProcess.isFinished())
            return bgProcess;
        
        // if bgProcess is not finished - then request it again (bgProcess has dynamic properties)
        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream      out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_BG_PROCESS);
            out.writeUTF (key);
            out.flush ();

            checkResponse(ds);

            return bgProcess = TDBProtocol.readBGProcessInfo(ds.getDataInputStream());

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } catch (ClassNotFoundException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } finally {
            Util.close (ds);
        }
    }

    public void                     abortBackgroundProcess() {
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_ABORT_BG_PROCESS);
            out.writeUTF (key);            
            out.flush ();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public Periodicity              getPeriodicity() {
        synchronized (this) {
            conn.getSession().getStreamProperty(key, TickStreamProperties.PERIODICITY);
        }
        return options.periodicity;
    }

    @Override
    public void                     setPeriodicity(Periodicity periodicity) {
        if (periodicity == null)
            periodicity = Periodicity.mkIrregular();

        setStringProperty(TDBProtocol.REQ_SET_STREAM_PERIOD, periodicity.toString());
        options.periodicity = periodicity;
    }

//    @Override
//    public int                      getNotificationDelay() {
//        return notificationDelay.get();
//    }
//
//    @Override
//    public void setNotificationDelay(int delay) {
//        notificationDelay.set(delay);
//    }

    public String                   toString () {
        return key;
    }

    private void                    assertLocked(LockType type) {
        if (lock != null && lock.isValid() && lock.getType() != type)
            throw new StreamLockedException("Stream '" + this + "' already has " + lock);
    }

    @Override
    public synchronized DBLock      lock(LockType type) throws StreamLockedException {
        assertLocked(type);

        if (lock != null) {
            lock.reuse();
            return lock;
        }
        
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_LOCK_STREAM);
            out.writeUTF (key);
            out.writeBoolean (type == LockType.READ);
            out.flush ();

            checkResponse(ds);

            return this.lock = readLock(ds.getDataInputStream());
            
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public synchronized DBLock      tryLock(LockType type, long timeout) throws StreamLockedException {
        assertLocked(type);

        if (lock != null) {
            lock.reuse();
            return lock;
        }

        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_TRY_LOCK_STREAM);
            out.writeUTF (key);
            out.writeBoolean (type == LockType.READ);
            out.writeLong(timeout);
            out.flush ();

            checkResponse(ds);

            return this.lock = readLock(ds.getDataInputStream());

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public synchronized DBLock      tryLock(long timeout) throws StreamLockedException {
        return tryLock(LockType.WRITE, timeout);
    }

    synchronized void               writeLock(DataOutputStream dout) throws IOException {
       writeLock(dout, lock);
    }

    private void                    writeLock(DataOutputStream dout, ClientLock lock) throws IOException {
        assert Thread.holdsLock(this);

        dout.writeBoolean(lock != null);
        
        if (lock != null) {
            dout.writeUTF(lock.getGuid());
            dout.writeBoolean(lock.getType() == LockType.READ);
        }
    }

    private ClientLock              readLock(DataInputStream din) throws IOException {
        boolean exists = din.readBoolean();
        
        if (exists) {
            String guid = din.readUTF();
            LockType type = din.readBoolean() ? LockType.READ : LockType.WRITE;

            return new ClientLock(this, type, guid);
        }
        
        return null;
    }

    @Override
    public synchronized DBLock      lock() throws StreamLockedException {
        return lock(LockType.WRITE);
    }    

    public synchronized void        unlock() {
        if (lock == null)
            return;

        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_UNLOCK_STREAM);
            out.writeUTF (key);
            writeLock(out, lock);
            out.flush();

            checkResponse(ds);

            lock = null;

        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public synchronized DBLock      verify(DBLock dbLock, LockType type) throws StreamLockedException {
        if (lock != null && !lock.equals(dbLock))
            throw new StreamLockedException("Stream locked by " + lock);

        return dbLock;
    }

    public synchronized boolean     isValid(ClientLock lock) {
         VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_LOCK_STATE);
            out.writeUTF (key);
            writeLock(out, lock);
            out.flush();

            checkResponse(ds);
            return ds.getDataInputStream().readBoolean();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public boolean                  enableVersioning() {
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_ENABLE_VERSIONING);
            out.writeUTF (key);
            out.flush ();
            checkResponse(ds);
            return ds.getDataInputStream().readBoolean();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public int getFormatVersion() {
        return formatVersion;
    }

    @Override
    public long                     getDataVersion() {
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_DATA_VERSION);
            out.writeUTF (key);
            out.flush ();
            checkResponse(ds);
            return ds.getDataInputStream().readLong();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public long                     getReplicaVersion() {
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_REPLICA_VERSION);
            out.writeUTF (key);
            out.flush ();
            checkResponse(ds);
            return ds.getDataInputStream().readLong();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public void                 setReplicaVersion(long version) {
        VSChannel                  ds = null;

        try {
            ds = connect();
            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_SET_REPLICA_VERSION);
            out.writeUTF (key);
            out.writeLong(version);
            out.flush ();
            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    @Override
    public IdentityKey[]     getComposition(IdentityKey... ids) {
        VSChannel                  ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_GET_INSTR_COMPOSITION);
            out.writeUTF (key);
            TDBProtocol.writeInstrumentIdentities(ids, out);
            out.flush ();

            checkResponse(ds);

            return (TDBProtocol.readInstrumentIdentities (ds.getDataInputStream()));
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }


    @Override
    public RecordClassDescriptor[]          getTypes() {
        return isFixedType() ? new RecordClassDescriptor[] {getFixedType()} : getPolymorphicDescriptors();
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        SelectionOptions opts;

        if (options instanceof SelectionOptions) {
            opts = (SelectionOptions) options;
        } else {
            opts = new SelectionOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createCursor(opts);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        LoadingOptions opts;

        if (options instanceof LoadingOptions) {
            opts = (LoadingOptions) options;
        } else {
            opts = new LoadingOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createLoader(opts);
    }

    @Nullable
    @Override
    public String[]             listSpaces() {
        assertSupportsStreamSpaces();

        VSChannel ds = null;

        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_LIST_STREAM_SPACES);
            out.writeUTF(key);
            out.flush();

            checkResponse(ds);

            DataInputStream din = ds.getDataInputStream();
            return TDBProtocol.readNullableStringArray(din);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Override
    public void         deleteSpaces(String... names) {
        assertSupportsStreamSpaces();

        if (names == null)
            throw new IllegalArgumentException("spaces argument is null");

        VSChannel ds = null;

        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_DELETE_SPACES);
            out.writeUTF(key);
            TDBProtocol.writeNullableStringArray(out, names);
            out.flush();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Override
    public void renameSpace(String newName, String oldName) {
        assertSupportsStreamSpaces();

        VSChannel ds = null;

        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_RENAME_SPACES);
            out.writeUTF(key);
            TDBProtocol.writeNullableString(newName, out);
            TDBProtocol.writeNullableString(oldName, out);
            out.flush();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Nullable
    @Override
    public IdentityKey[]     listEntities(String space) {
        assertSupportsStreamSpaces();

        if (space == null) {
            throw new IllegalArgumentException("space can't be null");
        }

        VSChannel ds = null;

        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_LIST_IDS_FOR_SPACE);
            out.writeUTF(key);
            out.writeUTF(space);
            out.flush();

            checkResponse(ds);

            DataInputStream din = ds.getDataInputStream();
            return TDBProtocol.readInstrumentIdentities(din);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Nullable
    @Override
    public long[]       getTimeRange(String space) {
        assertSupportsStreamSpaces();

        if (space == null) {
            throw new IllegalArgumentException("space can't be null");
        }

        VSChannel ds = null;

        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_GET_TIME_RANGE_FOR_SPACE);
            out.writeUTF(key);
            out.writeUTF(space);
            out.flush();

            checkResponse(ds);

            DataInputStream din = ds.getDataInputStream();

            return TDBProtocol.readTimeRangeLong(din);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    private void assertSupportsStreamSpaces() {
        if (conn.getServerProtocolVersion() < 110) {
            throw new UnsupportedOperationException("Operation is not supported by the server");
        }
    }

    DXClientAeronContext getAeronContext() {
        if (conn instanceof TickDBClient) {
            return ((TickDBClient) conn).getAeronContext();
        } else if (conn instanceof UserDBClient) {
            return ((UserDBClient) conn).getAeronContext();
        } else if (conn instanceof TickStreamClient) {
            return ((TickStreamClient) conn).getAeronContext();
        } else {
            throw new UnsupportedOperationException("Unknown client type: " + conn.getClass());
        }
    }
}