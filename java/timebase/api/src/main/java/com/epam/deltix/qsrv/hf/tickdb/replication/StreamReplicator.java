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
package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.blocks.InstrumentSet;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;
import com.epam.deltix.qsrv.hf.stream.DXDataReader;
import com.epam.deltix.qsrv.hf.stream.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.ServerException;
import com.epam.deltix.qsrv.hf.tickdb.impl.LockFile;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.timebase.messages.service.MetaDataChangeMessage;
import com.epam.deltix.timebase.messages.service.StreamTruncatedMessage;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.RandomAccessFileStore;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.progress.ExecutionMonitorImpl;
import com.epam.deltix.util.progress.ExecutionStatus;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.concurrent.Signal;

import java.io.*;
import java.util.*;

/**
 *
 */
public class StreamReplicator {
    public final Log LOGGER;

    public static final String              EXTENSION = ".dxdata";
    private static final String             LOCK_FILE_NAME = "r.lock";

    private volatile BackgroundProcessInfo  bgProcess;
    private volatile Thread                 bgRunner;

    private final TimeStamp                 precise = new TimeStamp();

    private UnboundDecoder           truncateMsgDecoder =
            InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(Messages.STREAM_TRUNCATED_MESSAGE_DESCRIPTOR).create ();
    private UnboundDecoder           schemaMsgDecoder =
                InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(Messages.META_DATA_CHANGE_MESSAGE_DESCRIPTOR).create ();

    private final MemoryDataInput   buffer = new MemoryDataInput();

    public StreamReplicator() {
        this(null);
    }

    public StreamReplicator(Log logger) {
        LOGGER = logger != null ? logger : LogFactory.getLog("deltix.qsrv.hf.tickdb.replication");
    }

    private class ReconnectListener implements DisconnectEventListener {
        private Signal reconnectSignal = new Signal();
        private volatile boolean disconnected = false;

        @Override
        public void onDisconnected() {
            disconnected = true;
            LOGGER.info("Disconnected");
        }

        @Override
        public void onReconnected() {
            disconnected = false;
            reconnectSignal.set();
            LOGGER.info("Reconnected");
        }

        public void await() throws InterruptedException {
            reconnectSignal.await();
        }
    }

    private final ReconnectListener fromListener = new ReconnectListener();
    private final ReconnectListener toListener = new ReconnectListener();

    private void        awaitReconnect(ReconnectListener reconnect) {
        try {
            reconnect.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void        waitForTimeout(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private interface RunnableWithMonitor {
        void run(ExecutionMonitorImpl monitor);
    }

    private class ReplicationTask implements RunnableWithMonitor {
        private RunnableWithMonitor runnable;
        private final Storage from;
        private final Storage to;
        private final CommonOptions options;

        private ReplicationTask(RunnableWithMonitor runnable, Storage from, Storage to, CommonOptions options) {
            this.runnable = runnable;
            this.from = from;
            this.to = to;
            this.options = options;
        }

        @Override
        public void run(ExecutionMonitorImpl monitor) {
            monitor.start();
            try {
                runnable.run(monitor);
            } catch (Throwable t) {
                monitor.abort(t);
                throw t;
            } finally {
                completeMonitor(monitor);
            }
        }
    }

    public void         replicate(final StreamStorage from, final Storage to, final ReplicationOptions options) {
        runReplicationTask(
            new ReplicationTask((monitor) ->
                runReplication(from, to, options, monitor), from, to, options
            )
        );
    }

    public void         restore(final Storage from, final Storage to, final RestoreOptions options) {
        runReplicationTask(
            new ReplicationTask((monitor) ->
                runRestoration(from, to, options, monitor), from, to, options
            )
        );
    }

    private void        runReplicationTask(final ReplicationTask task) {
        final ExecutionMonitorImpl monitor = new ExecutionMonitorImpl();
        if (task.options.retries > 0) {
            runWithRetries(task, monitor);
        } else {
            if (task.options.async)
                runAsync(task, monitor);
            else
                runSync(task, monitor);
        }
    }

    private void        runWithRetries(ReplicationTask task, ExecutionMonitorImpl monitor) {
        subscribeDisconnectListeners(task);
        try {
            int retries = task.options.retries;
            long lastRetryTime = TimeKeeper.currentTime;
            long refreshRetriesTimeout = getRefreshRetriesTimeout(task.options.retryTimeout);
            do {
                try {
                    if (TimeKeeper.currentTime - lastRetryTime > refreshRetriesTimeout)
                        retries = task.options.retries;      //refresh retries
                    lastRetryTime = TimeKeeper.currentTime;

                    task.run(monitor);
                    break;
                } catch (ReplicationException e) {
                    LOGGER.error("Replication failed %s").with(e);
                    throw e;
                } catch (Throwable t) {
                    LOGGER.warn("Replication failed, restart attempts left: %s. %s ").with(retries).with(t);

                    if (retries <= 0)
                        throw t;

                    waitForTimeout(task.options.retryTimeout);
                    waitForReconnect();
                }
            } while (--retries > 0);
        } finally {
            unsubscribeDisconnectListeners(task);
        }
    }

    private void         runAsync(final RunnableWithMonitor runnable, final ExecutionMonitorImpl monitor) {
        if (bgRunner != null && bgRunner.isAlive())
            throw new IllegalStateException("Another background task is running.");

        bgProcess = new BackgroundProcessInfo("replicate", monitor);
        bgRunner = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run(monitor);
                } finally {
                    bgRunner = null;
                }
            }
        };
        bgRunner.start();
    }

    private void         runSync(final RunnableWithMonitor runnable, final ExecutionMonitorImpl monitor) {
        bgProcess = new BackgroundProcessInfo("replicate", monitor);
        runnable.run(monitor);
    }

    private void         runReplication(
        final StreamStorage from, final Storage to, final ReplicationOptions options, final ExecutionMonitorImpl monitor)
    {
        if (to instanceof StreamStorage)
            replicate(from, (StreamStorage) to, options, monitor);
        else if (to instanceof FileStorage)
            backup(from, (FileStorage) to, options, monitor);
        else
            throw new ReplicationException("Target storage " + to + " is not supported");
    }

    private void          runRestoration(
        final Storage from, final Storage to, final RestoreOptions options, final ExecutionMonitorImpl monitor)
    {
        if (from instanceof StreamStorage) {
            if (to instanceof StreamStorage)
                restore((StreamStorage) from, (StreamStorage) to, options, monitor);
            else
                throw new ReplicationException("Target storage " + to + " is not supported");
        } else if (from instanceof FileStorage) {
            if (to instanceof StreamStorage)
                restore((FileStorage) from, (StreamStorage) to, options, monitor);
            else
                throw new ReplicationException("Target storage " + to + " is not supported");
        } else
            throw new ReplicationException("Source storage " + from + " is not supported");
    }

    private void subscribeDisconnectListeners(ReplicationTask task) {
        subscribeDisconnectListener(task.from, fromListener);
        subscribeDisconnectListener(task.to, toListener);
    }

    private void unsubscribeDisconnectListeners(ReplicationTask task) {
        unsubscribeDisconnectListeners(task.from, fromListener);
        unsubscribeDisconnectListeners(task.to, toListener);
    }

    private void subscribeDisconnectListener(Storage storage, ReconnectListener listener) {
        if (storage instanceof StreamStorage) {
            StreamStorage streamStorage = (StreamStorage) storage;
            if (streamStorage.db instanceof Disconnectable) {
                ((Disconnectable) streamStorage.db).addDisconnectEventListener(listener);
                listener.disconnected = false;
            }
        }
    }

    private void unsubscribeDisconnectListeners(Storage storage, ReconnectListener listener) {
        if (storage instanceof StreamStorage) {
            StreamStorage streamStorage = (StreamStorage) storage;
            if (streamStorage.db instanceof Disconnectable) {
                ((Disconnectable) streamStorage.db).removeDisconnectEventListener(listener);
                listener.disconnected = false;
            }
        }
    }

    private void waitForReconnect() {
        if (fromListener.disconnected) {
            LOGGER.info("Waiting source for reconnect.");
            awaitReconnect(fromListener);
        }

        if (toListener.disconnected) {
            LOGGER.info("Waiting target for reconnect.");
            awaitReconnect(toListener);
        }
    }

    private long getRefreshRetriesTimeout(long retryTimeout) {
        long refreshRetriesTimeout = 3 * retryTimeout;
        return refreshRetriesTimeout < 30000 ? 30000 : refreshRetriesTimeout;
    }

    private void replicate(StreamStorage from, StreamStorage to, ReplicationOptions options, ExecutionMonitorImpl monitor) {
        TypesMapping mapping = null;

        DXTickStream source = from.getSource();
        if (source == null)
            throw new ReplicationException("Source stream '" + from.name + "' is not found in source db");
        
        DXTickStream target = to.getSource();

        long[] range = options.range != null ? options.range : new long[] { Long.MIN_VALUE, Long.MAX_VALUE };
        
        if (target == null) {
            StreamOptions so = source.getStreamOptions();
            so.name = to.name;
            target = to.db.createStream(to.name, so);
            LOGGER.info("Stream [%s] was created.").with(target.getKey());
        } else {
            RecordClassDescriptor[] inTypes = DXTickStream.getClassDescriptors(source);
            RecordClassDescriptor[] outTypes = DXTickStream.getClassDescriptors(target);

            if (!MessageProcessor.isBinaryCompatible(inTypes, outTypes)) {
                LOGGER.warn("Target stream [" + target.getKey() +  "] schema in not compatible with [" + source.getKey() + "]");
                if (options.mode == ReloadMode.allow) {
                    LOGGER.warn ("Clearing data and changing target [" + target.getKey() + "] schema due to rewrite mode.");

                    if (source.isFixedType())
                        target.setFixedType(source.getFixedType());
                    else
                        target.setPolymorphic(source.getPolymorphicDescriptors());
                } else {
                    LOGGER.error("Cannot replicate stream " + source.getKey() + " into different schema:" +
                        " source types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \ntarget types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                    return;
                }
            } else if (!MessageProcessor.isEquals(inTypes, outTypes)) {
                // check that we should remap types
                mapping = new TypesMapping(inTypes, outTypes);
            }
        }

        int sourceDF = source.getDistributionFactor();
        int targetDF = target.getDistributionFactor();
        if (sourceDF != targetDF && targetDF != 0)
            throw new ReplicationException(
                "Mismatch streams distribution factors: " +
                    "Source [DF=" + (sourceDF == 0 ? "MAX" : sourceDF) + "] " +
                    "Target [DF=" + (targetDF == 0 ? "MAX" : targetDF) + "]");

        HashMap<IdentityKey, InstrumentsComposition> times =
                new HashMap<IdentityKey, InstrumentsComposition>();

        // calculate time ranges
        InstrumentSet entities = new InstrumentSet();
        entities.addAll(Arrays.asList(options.entities == null ? source.listEntities() : options.entities));

        for (IdentityKey id : entities) {
            if (!times.containsKey(id)) {
                IdentityKey[] composition = target.getComposition(id);

                InstrumentsComposition ic = new InstrumentsComposition();
                long[] tr = target.getTimeRange(id);

                long fromTime = tr != null ? tr[1] : Long.MIN_VALUE;

                // if is not exists in target
                if (tr == null) {
                    tr = source.getTimeRange(id);
                    fromTime = tr != null ? tr[0] : Long.MIN_VALUE;
                }

                // respect user-defined range, otherwise set to last time of target stream
                ic.timestamp = fromTime != Long.MIN_VALUE && TimeStamp.isUndefined(range[0]) ? fromTime : range[0];

                for (IdentityKey iid : composition)
                    if (entities.contains(iid)) {
                        ic.add(iid);
                        times.put(iid, ic);
                    }
            }
        }

        source.enableVersioning();

        long version = target.getReplicaVersion();

        SelectionOptions so = new SelectionOptions(true, options.live);
        so.versionTracking = true;
        so.allowLateOutOfOrder = true;

        long end = range[1];

        boolean schemaChanged = false;

        MessageSourceMultiplexer<InstrumentMessage> msm = new
                MessageSourceMultiplexer<InstrumentMessage>(true, false);
        msm.setLive(options.live);

        TickCursor cursor = null;
        TickLoader loader = null;
        DBLock dblock = null;

        try {
            RawMessage msg;

            TickCursor sCursor = source.select(Long.MIN_VALUE, so, null,
                    new IdentityKey[] { new ConstantIdentityKey("@SYSTEM") });
            msm.add(sCursor);

            boolean logVersionErrors = true;
            long dataVersion = -1;
            // reading system messages only
            long sourceVersion = source.getDataVersion();

            while (dataVersion < sourceVersion && msm.next()) {

                msg = (RawMessage) msm.getMessage();
                long msgVersion = getVersion(msg);

                if (msgVersion < dataVersion) {
                    if (logVersionErrors)
                        LOGGER.warn("[" + source.getKey() + "] incorrect version: " + msgVersion);
                    logVersionErrors = false;
                }
                dataVersion = msgVersion;

                if (isStreamTruncatedMessage(msg)) {

                    if (dataVersion > version) { // process truncate
                        long ts = getNanoTime(msg);
                        precise.setNanoTime(ts);
                        long time = precise.getTime();

                        String instruments = readInstruments(msg);
                        LOGGER.warn("Stream [" + source.getKey() + "] was truncated (time=" + ts + ", instruments=" + instruments + ")");
                        if (options.mode == ReloadMode.prohibit) {
                            LOGGER.warn("Prohibit target [" + target.getKey() + "] truncation.");
                            throw new ReplicationException("Cannot truncate due to restrictions of reload mode [" + options.mode + "]");
                        } else {
                            LOGGER.warn("Truncating target [" + target.getKey() + "] using time:" + time);
                        }

                        IdentityKey[] ids = parseInstruments(instruments);
                        target.truncate(time, ids);

                        // update start time for the instruments
                        IdentityKey[] composition = target.getComposition(ids);

                        for (int i = 0; i < composition.length; i++) {
                            InstrumentsComposition ic = times.get(composition[i]);
                            if (ic != null)
                                ic.timestamp = Math.min(time, ic.timestamp);
                        }

                    }
                } else if (isMetaDataChangeMessage(msg)) {

                    if (version != -1 && dataVersion > version) {

                        if (options.mode != ReloadMode.allow)
                            throw new ReplicationException("Cannot change schema due to restrictions of reload mode [" + options.mode + "]");
                        
                        schemaChanged = true;

                        if (isDataConverted(msg)) {
                            LOGGER.warn("Source stream [" + source.getKey() + "] schema was changed.");

                            LOGGER.warn("Clearing and reloading target [" + target.getKey() + "] stream.");
                            if (source.isPolymorphic())
                                target.setPolymorphic(source.getPolymorphicDescriptors());
                            else
                                target.setFixedType(source.getFixedType());
                        }
                    }
                }
            }

            if (schemaChanged)
                processSchemaChange(source, target);

            target.setReplicaVersion(dataVersion);

            so = new SelectionOptions(true, options.live);
            so.allowLateOutOfOrder = true;

            if (schemaChanged) {
                cursor = source.select(range[0], so, options.types, options.entities);
                msm.add(cursor);
            } else {
                cursor = source.createCursor(so);
                cursor.reset(range[0]);

                // transform subscription using instruments composition of the source stream
                HashMap<IdentityKey, InstrumentsComposition> subscription =
                        new HashMap<IdentityKey, InstrumentsComposition>();

                for (IdentityKey id : entities) {
                    InstrumentsComposition cc = times.get(id);

                    IdentityKey[] composition = source.getComposition(id);
                    InstrumentsComposition ic = subscription.get(id);
                    if (ic == null) {
                        ic = new InstrumentsComposition();
                        ic.timestamp = cc.timestamp;
                    }

                    for (IdentityKey iid : composition) {
                        if (entities.contains(iid)) {
                            subscription.put(iid, ic);
                            ic.add(iid);
                            ic.timestamp = Math.min(ic.timestamp, cc.timestamp);
                        }
                    }
                }

                // group by timestamp
                SortedMap<Long, InstrumentSet> subscribed = new TreeMap<Long, InstrumentSet>();

                for (InstrumentsComposition ic : subscription.values()) {
                    InstrumentSet set = subscribed.get(ic.timestamp);

                    if (set == null)
                        subscribed.put(ic.timestamp, set = new InstrumentSet());

                    set.addAll(ic.list());
                }

                if (options.types != null && options.types.length > 0)
                    cursor.addTypes(options.types);

                // subscribe
                for (Map.Entry<Long, InstrumentSet> entry : subscribed.entrySet()) {
                    cursor.setTimeForNewSubscriptions(entry.getKey());
                    IdentityKey[] ids = entry.getValue().toArray();
                    cursor.addEntities(ids, 0, ids.length);
                }
                
                if (options.entities == null && options.live) {
                    cursor.setTimeForNewSubscriptions(range[0]);
                    cursor.subscribeToAllEntities();
                }

                msm.add(cursor);
            }

            dblock = !options.live ? target.lock() : null;
            loader = target.createLoader(new LoadingOptions(true));
            final LoadingErrorListener listener = new LoadingErrorListener() {
                 public void onError(LoadingError e) {
                     LOGGER.error("Error writing message: %s").with(e);
                 }
            };
            loader.addEventListener(listener);

            long[] sourceRange = options.entities != null ? source.getTimeRange(options.entities) : source.getTimeRange();
            sourceRange = calcMonitorRange(sourceRange, range);

            boolean updateMonitor = true;
            long rangeLength = sourceRange[1] - sourceRange[0];
            if (options.live || rangeLength == 0)
                updateMonitor = false;

            long lastTime = Long.MIN_VALUE;
            int flushNum = 0;
            while (true) {
                try {
                    if (!msm.next() || msm.getMessage().getTimeStampMs() > end)
                        break;

                    msg = (RawMessage) msm.getMessage();

                    if (isStreamTruncatedMessage(msg)) {
                        if (options.mode == ReloadMode.prohibit)
                            throw new ReplicationException("Cannot truncate due to restrictions of reload mode [" + options.mode + "]");

                        processTruncate(target, msg);
                        target.setReplicaVersion(getVersion(msg));
                        readInstruments(msg);
                    } else if (isMetaDataChangeMessage(msg)) {
                        if (options.mode != ReloadMode.allow)
                            throw new ReplicationException("Cannot change schema due to restrictions of reload mode [" + options.mode + "]");

                        target.setReplicaVersion(getVersion(msg));
                        LOGGER.warn("Stream schema was changed. Reloading all data.");
                        loader.close();

                        msm.closeAndRemove(cursor);
                        cursor = source.select(range[0], so, options.types, options.entities);
                        msm.add(cursor);

                        if (source.isPolymorphic())
                            target.setPolymorphic(source.getPolymorphicDescriptors());
                        else
                            target.setFixedType(source.getFixedType());

                        loader = target.createLoader(new LoadingOptions(true));
                    }
                    else {
                        try {
                            if (mapping != null)
                                msg.type = mapping.getType(msg.type);

                            //System.out.println("Sending message: " + msg);
                            assert msg.getTimeStampMs() >= lastTime: "message" + msg + " timestamp is less that latest time: " + GMT.formatDateTimeMillis(lastTime);

                            if (updateMonitor)
                                monitor.setProgress((double) (msg.getTimeStampMs() - sourceRange[0]) / (double) rangeLength);

                            loader.send(msg);
                            lastTime = msg.getTimeStampMs();
                            if (options.flush > 0 && ++flushNum >= options.flush) {
                                flushNum = 0;
                                if (loader instanceof Flushable)
                                    ((Flushable) loader).flush();
                            }
                        } catch (IllegalArgumentException e) {
                            // we can get wrong message after schema change, but it's ok,
                            // full reload will be done
                        }
                    }
                } catch (CursorException ex) {
                    LOGGER.warn("Re-creating cursor due to cursor error. %s").with(ex);
                    // recreate cursor
                    msm.closeAndRemove(cursor);
                    cursor = source.select(lastTime, so, options.types, options.entities);
                    msm.add(cursor);

                    msm.setException(null); // clear exception
                } catch (IOException ioe) {
                    LOGGER.error("Replication error: %s").with(ioe);
                    throw new UncheckedIOException(ioe);
                } catch (Throwable ex) {
                    LOGGER.error("Replication error: %s").with(ex);
                    throw ex;
                }
            }

        } finally {
            Util.close(loader);

            if (dblock != null)
                dblock.release();

            Util.close(msm);
        }

        LOGGER.info("Replication completed.");
    }

    public BackgroundProcessInfo        getBackgroundProcess() {
        if (bgProcess != null)
            bgProcess.update();

        return bgProcess;
    }

    private long[]                      calcMonitorRange(long[] sourceRange, long[] selectRange) {
        if (sourceRange == null)
            sourceRange = selectRange;
        if (sourceRange[0] < selectRange[0])
            sourceRange[0] = selectRange[0];
        if (sourceRange[1] > selectRange[1])
            sourceRange[1] = selectRange[1];

        return sourceRange;
    }

    private void                        completeMonitor(ExecutionMonitorImpl monitor) {
        ExecutionStatus executionStatus = monitor.getStatus();
        if (executionStatus == ExecutionStatus.Running || executionStatus == ExecutionStatus.None)
            monitor.setComplete();
    }

    private boolean        isStreamTruncatedMessage(RawMessage msg) {
        return msg.type.getName().equals(StreamTruncatedMessage.class.getName());
    }

    private boolean        isMetaDataChangeMessage(RawMessage msg) {
        return msg.type.getName().equals(MetaDataChangeMessage.class.getName());
    }

    private long            processTruncate(DXTickStream target, RawMessage truncate) {
        precise.setNanoTime(getNanoTime(truncate));
        long time = precise.getTime();
        IdentityKey[] ids = getInstruments(truncate);
        target.truncate(time, ids);
        return time;
    }

    private void            processSchemaChange(DXTickStream source, DXTickStream target) {
        StreamMetaDataChange change = SchemaAnalyzer.getChanges(source, target);

        // we can convert data
        target.execute(new SchemaChangeTask(change));
        BackgroundProcessInfo process;
        while ((process = target.getBackgroundProcess()) != null && !process.isFinished()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }

    private void         restore(StreamStorage src, StreamStorage dest, RestoreOptions options, ExecutionMonitorImpl monitor) {
        TypesMapping mapping = null;

        DXTickStream source = src.getSource();

        if (source == null) {
            LOGGER.warn("Source stream " + src.name + " does not exists in source database.");
            return;
        }

        DXTickStream target = dest.getSource();

        if (target == null) {
            final StreamOptions so = source.getStreamOptions();
            so.version = null;
            so.name = dest.name;
            target = dest.db.createStream(dest.name, so);
            LOGGER.info("Stream [%s] was created.").with(target.getKey());
        } else {
            // check compatibility
            RecordClassDescriptor[] inTypes = DXTickStream.getClassDescriptors(source);
            RecordClassDescriptor[] outTypes = DXTickStream.getClassDescriptors(target);

            if (!MessageProcessor.isBinaryCompatible(inTypes, outTypes)) {
                LOGGER.warn("Target stream [" + target.getKey() +  "] schema in not compatible with backup.");

                if (options.mode == ReloadMode.allow) {
                    LOGGER.warn("Clearing data and changing stream [" + target.getKey() + "] schema while due to rewrite mode.");

                    if (source.isFixedType())
                        target.setFixedType(source.getFixedType());
                    else
                        target.setPolymorphic(source.getPolymorphicDescriptors());

                } else {
                    LOGGER.error("Cannot restore stream " + target.getKey() + " into different schema:" +
                            " source types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \ntarget types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                    return;
                }
            } else if (!MessageProcessor.isEquals(inTypes, outTypes)) {
                // check that we should remap types
                mapping = new TypesMapping(inTypes, outTypes);
            }
        }

        IdentityKey[] entities = options.entities;
        long[] range = options.range != null ? options.range : new long[] { Long.MIN_VALUE, Long.MAX_VALUE };

        LoadingOptions loadingOptions = new LoadingOptions((true));
        if (options.mode == ReloadMode.prohibit) {
            loadingOptions.writeMode = LoadingOptions.WriteMode.APPEND;
            LOGGER.warn("Restore will only append new data into [" + source.getKey() + "] stream due to '" + options.mode +  "' mode ");
        }

        TickLoader loader = null;
        TickCursor cursor = null;
        DBLock lock = null;

        long[] sourceRange = options.entities != null ? source.getTimeRange(options.entities) : source.getTimeRange();
        sourceRange = calcMonitorRange(sourceRange, range);
        boolean updateMonitor = true;
        long rangeLength = sourceRange[1] - sourceRange[0];
        if (rangeLength == 0)
            updateMonitor = false;

        try
        {
            lock = target.tryLock(LockType.WRITE, 5000);

            loader = target.createLoader(loadingOptions);
            cursor = source.select(range[0], new SelectionOptions(true, false), null, entities);

            while (true) {
                try {
                    if (!cursor.next() || cursor.getMessage().getTimeStampMs() > range[1])
                        break;

                    RawMessage msg = (RawMessage) cursor.getMessage();
                    if (mapping != null)
                        msg.type = mapping.getType(msg.type);

                    if (updateMonitor)
                        monitor.setProgress((double) (msg.getTimeStampMs() - sourceRange[0]) / (double) rangeLength);

                    loader.send(msg);

                } catch (CursorException ex) {
                    // ignore
                }
            }

        } catch (StreamLockedException e) {
            LOGGER.error("Cannot lock stream [%s]: %s").with(target.getKey()).with(e);
        } finally {
            Util.close(cursor);
            Util.close(loader);

            if (lock != null)
                lock.release();
        }

        LOGGER.info("Stream " + dest.name + " restoring complete.");
    }

    private void writeStreamOptions(File file, StreamOptions options) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new FileOutputStream(file));
            out.writeInt(TDBProtocol.VERSION);
            TDBProtocol.writeStreamOptions(out, options, TDBProtocol.VERSION);
        } catch (IOException e) {
            LOGGER.warn("Cannot store stream options: %s").with(e);
        } finally {
            Util.close(out);
        }
    }

    private StreamOptions readStreamOptions(File file) {

        // read stream options
        try {
            byte[] content = IOUtil.readBytes(file);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(content));

            if (content[0] < 4) // it was StreamScope before version 76
                return TDBProtocol.readStreamOptions76(in);

            int version = in.readInt();
            if (version == TDBProtocol.VERSION)
                return TDBProtocol.readStreamOptions(in, version);

            throw new ReplicationException("Unsupported version: " + version);

        } catch (IOException e) {
            LOGGER.warn("Cannot read stream options: %s").with(e);
        }

        return null;
    }

    private void         restore(FileStorage from, StreamStorage to, RestoreOptions options, ExecutionMonitorImpl monitor) {
        TypesMapping mapping = null;

        File dir = from.folder;
        final String prefix = getPrefix(options.name);

        // look up for backup files for the stream

        final File[] list = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(prefix + EXTENSION);
            }
        });

        long[] range = options.range != null ? options.range : new long[] { Long.MIN_VALUE, Long.MAX_VALUE };

        if (list == null || list.length == 0) {
            LOGGER.warn("There are no backup files for stream [%s] in folder [%s]").with(options.name).with(dir);
            return;
        }

        Arrays.sort(list);
        int start = -1, end = -1;

        for (int i = 0; i < list.length; i++) {
            long timestamp = getFileTime(list[i]);

            if (range[0] <= timestamp && start == -1)
                start = end = i;

            if (range[0] <= timestamp && timestamp < range[1])
                end = i;
        }

        if (start == -1) {
            LOGGER.warn("There are no backup files for stream [%s] in the specified time range [%s, %s]").with(options.name).withTimestamp(range[0]).withTimestamp(range[1]);
            return;
        }

        StreamOptions so = readStreamOptions(new File(from.folder, prefix + ".xml"));

//        // read stream options
//        FileInputStream in = null;
//        try {
//            in = new FileInputStream(new File(from.folder, prefix + ".xml"));
//            so = TDBProtocol.readStreamOptions(new DataInputStream(in));
//        } catch (IOException e) {
//            LOGGER.log(Level.WARNING, "Cannot read stream options.", e);
//        } finally {
//            Util.close(in);
//        }

        try {
            DXDataReader reader = new DXDataReader(list[end]);
            ConsumableMessageSource<InstrumentMessage> source = reader.readBlock();
            if (source != null)
                so.setPolymorphic(reader.getBlockTypes());
            reader.close();
        } catch (IOException e) {
            LOGGER.error("Restore procedure failed on %s: %s ").with(list[end]).with(e);
            throw new UncheckedIOException(e);
        }

        DXTickStream target = to.getSource();

        if (target == null) {
            // create stream if required
            if (hasStream(to.db, so.name))
                so.name = to.name;
            target = to.db.createStream(to.name, so);

            LOGGER.info("Stream [%s] was created.").with(target.getKey());
        } else {
            RecordClassSet classSet = so.getMetaData();
            // check compatibility
            RecordClassDescriptor[] inTypes = classSet.getContentClasses();
            RecordClassDescriptor[] outTypes = DXTickStream.getClassDescriptors(target);

            if (!MessageProcessor.isBinaryCompatible(inTypes, outTypes)) {
                LOGGER.warn("Target stream [" + target.getKey() +  "] schema in not compatible with backup.");

                if (options.mode == ReloadMode.allow) {
                    LOGGER.warn("Clearing data and changing stream [" + target.getKey() + "] schema while due to rewrite mode.");

                    if (so.isFixedType())
                        target.setFixedType(classSet.getTopType(0));
                    else
                        target.setPolymorphic(classSet.getTopTypes());

                } else {
                    LOGGER.error("Cannot restore stream " + target.getKey() + " into different schema:" +
                            " source types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \ntarget types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                    return;
                }
            } else if (!MessageProcessor.isEquals(inTypes, outTypes)) {
                // check that we should remap types
                mapping = new TypesMapping(inTypes, outTypes);
            }
        }

        DBLock lock = null;
        TickLoader loader = null;

        boolean updateMonitor = true;
        long rangeLength = end - start;
        if (rangeLength == 0)
            updateMonitor = false;

        try {
            lock = target.tryLock(LockType.WRITE, 5000);

            LoadingOptions loadingOptions = new LoadingOptions((true));
            if (options.mode == ReloadMode.prohibit) {
                loadingOptions.writeMode = LoadingOptions.WriteMode.APPEND;
                LOGGER.warn("Restore will only append new data into [" + target.getKey() + "] stream due to '" + options.mode +  "' mode ");
            }

            loader = target.createLoader(loadingOptions);

            for (int i = start; i <= end; i++) {
                File file = list[i];

                try {
                    LOGGER.info("Restoring from file %s;").with(file);

                    DXDataReader reader = new DXDataReader(file);
                    long[] bRange = reader.getTimeRange();

                    if (bRange[0] != Long.MIN_VALUE && bRange[1] != Long.MIN_VALUE) {
                        ConsumableMessageSource<InstrumentMessage> source;

                        while ((source = reader.readBlock()) != null) {
                            while (source.next()) {
                                RawMessage msg = (RawMessage)source.getMessage();

                                long time = msg.getTimeStampMs();
                                if (time >= range[0] && time <= range[1]) {
                                    if (mapping != null)
                                        msg.type = mapping.getType(msg.type);

                                    loader.send(msg);
                                }
                                else if (time > range[1]) {
                                    break;
                                }
                            }
                        }
                    }

                    if (updateMonitor)
                        monitor.setProgress((double)(i - start) / (double)(rangeLength));

                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("Error processing file: %s: %s").with(file).with(e);
                    throw new UncheckedIOException(e);
                } catch (Throwable ex) {
                    LOGGER.error("Replication error: %s").with(ex);
                    throw ex;
                }
            }
        } catch (StreamLockedException e) {
            LOGGER.error("Cannot lock stream [%s]: %s").with(target.getKey()).with(e);
        }  finally {
            Util.close(loader);

            if (lock != null)
                lock.release();
        }

        LOGGER.info("Stream " + options.name + " restoring complete.");
    }

    private void    backup(StreamStorage from, FileStorage to, ReplicationOptions options, ExecutionMonitorImpl monitor) {

        File folder = to.folder;

        RandomAccessFileStore lock;

        try{
            lock = lock(folder);
        } catch (IOException ex) {
            LOGGER.error("Cannot lock folder %s for backup: %s").with(folder).with(ex);
            return;
        }

        final DXTickStream source = from.getSource();

        final File[] list = folder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(EXTENSION);
            }
        });

        if (options.format && list != null) {
            LOGGER.warn("Clear previous backups in " + folder);

            for (File file : list) {
                String stream = file.getName().substring(file.getName().indexOf("-") + 1).replace(EXTENSION, "");
                File def = new File(folder, stream + ".xml");
                if (def.exists() && !def.delete())
                    LOGGER.warn("Cannot delete file " + def);

                if (!file.delete())
                    LOGGER.warn("Cannot delete file " + file);
            }
        }
//        else if (list != null) {
//            for (File file : list) {
//                String name = getStreamName(file);
//                if (!src.name.equals(name)) {
//                    LOGGER.log(Level.WARNING, "Cannot have backups for the different stream in a single folder." + folder);
//                    return;
//                }
//            }
//        }

        final String prefix = getPrefix(source.getKey());
        final String pattern = "-" + prefix + EXTENSION;

        final File[] data = folder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(pattern);
            }
        });

        long[] range = options.range != null ? options.range : new long[] { Long.MIN_VALUE, Long.MAX_VALUE };

        long start = range[0];
        long version = -1;

        ArrayList<File> files = new ArrayList<File>();
        // lookup for time timestamp
        if (data != null && data.length > 0) {

            Arrays.sort(data);

            for (File file : data) {
                files.add(file); // put all actual files

                long timestamp = getFileTime(file);
                if (timestamp > start)
                    start = timestamp;
            }

            LOGGER.warn("Do incremental backup from time " + start);

            File file = data[data.length - 1];

            // get last stored info
            DXDataReader reader = null;
            try {
                reader = new DXDataReader(file);
                while (reader.nextBlock()) {
                }

                try {
                    version = Long.parseLong(reader.getBlockName());
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid version in backup file : " + file);
                }

                reader.close();
            } catch (IOException e) {
                LOGGER.warn("Error reading file: %s. Error: %s").with(file).with(e);
            } finally {
                Util.close(reader);
            }
        }

        source.enableVersioning();

        SelectionOptions so = new SelectionOptions(true, options.live);
        so.versionTracking = true;
        so.allowLateOutOfOrder = true;

        MessageSourceMultiplexer<InstrumentMessage> msm = new
            MessageSourceMultiplexer<InstrumentMessage>(true, false);
        msm.setLive(options.live);

        long end = range[1];
        long time = Long.MIN_VALUE;

        TickCursor cursor = null;

        long[] sourceRange = options.entities != null ? source.getTimeRange(options.entities) : source.getTimeRange();
        sourceRange = calcMonitorRange(sourceRange, range);
        boolean updateMonitor = true;
        long rangeLength = sourceRange[1] - sourceRange[0];
        if (options.live || rangeLength == 0)
            updateMonitor = false;

        try {
            RawMessage msg;

            TickCursor sCursor = source.select(Long.MIN_VALUE, so, null,
                                new IdentityKey[] { new ConstantIdentityKey("@SYSTEM") });
            msm.add(sCursor);

            boolean logVersionErrors = true;
            long dataVersion = -1;
            // reading system messages only
            long sourceVersion = source.getDataVersion();

            while (dataVersion < sourceVersion && msm.next()) {

                msg = (RawMessage) msm.getMessage();
                long msgVersion = getVersion(msg);

                if (msgVersion < dataVersion) {
                    if (logVersionErrors)
                        LOGGER.warn("[" + source.getKey() + "] incorrect version: " + msgVersion);
                    logVersionErrors = false;
                }
                dataVersion = msgVersion;

                if (isStreamTruncatedMessage(msg)) {
                    String instruments = readInstruments(msg);
                    long ts = TimeStamp.getMilliseconds(getNanoTime(msg));
                    LOGGER.warn("Stream [%s] data was truncated (time=%s, instruments=%s)").with(source.getKey()).withTimestamp(ts).with(instruments);

                    dataVersion = getVersion(msg);

                    if (version < dataVersion)
                        start = ts;

                } else if (isMetaDataChangeMessage(msg)) {
                    dataVersion = getVersion(msg);

                    if (dataVersion > version && isDataConverted(msg)) {
                        LOGGER.warn("Stream [%s] schema was changed.").with(source.getKey());

                        if (files.size() > 0 && options.mode != ReloadMode.allow)
                            throw new ReplicationException("Cannot change schema due to restrictions of reload mode [" + options.mode + "]");

                        clearFiles(files);
                        start = range[0];
                    }
                }
            }

            if (version < dataVersion) {
                if (files.size() > 0 && options.mode == ReloadMode.prohibit)
                    throw new ReplicationException("Cannot truncate due to restrictions of reload mode [" + options.mode + "]");

                long ts = truncateFiles(start, files);
                start = ts == Long.MIN_VALUE ? range[0] : ts;
            }

            // main subscription

            so = new SelectionOptions(true, options.live);
            so.allowLateOutOfOrder = true;
            cursor = source.select(start, so, options.types, options.entities);
            msm.add(cursor);

            BackupWriter writer = new BackupWriter(folder, files, version, options);
            writer.setKey(prefix);
            writer.setClasses(DXTickStream.getClassDescriptors(source));

            long lastTime = Long.MIN_VALUE;
            while (true) {
                try {
                    if (!msm.next() || msm.getMessage().getTimeStampMs() > end)
                        break;

                    msg = (RawMessage) msm.getMessage();

                    if (isStreamTruncatedMessage(msg)) {
                        version = getVersion(msg);
                        long ts = TimeStamp.getMilliseconds(getNanoTime(msg));
                        String instruments = readInstruments(msg);

                        LOGGER.warn("Stream [%s] data was truncated (time=%s, instruments=%s)").with(source.getKey()).withTimestamp(ts).with(instruments);

                        if (options.mode == ReloadMode.prohibit)
                            throw new ReplicationException("Cannot truncate due to restrictions of reload mode [" + options.mode + "]");

                        ts = writer.onTruncate(version, ts);
                        time = ts != Long.MIN_VALUE ? ts : range[0];
                        
                        if (cursor.isClosed()) {
                            msm.closeAndRemove(cursor);
                            cursor = source.select(time, so, options.types, options.entities);
                            msm.add(cursor);
                        } else {
                            msm.remove(cursor);
                            cursor.reset(time);
                            msm.add(cursor);
                        }

                    } else if (isMetaDataChangeMessage(msg)) {
                        version = getVersion(msg);
                        LOGGER.warn("Stream [%s] schema was changed.").with(source.getKey());

                        if (options.mode != ReloadMode.allow)
                            throw new ReplicationException("Cannot change schema due to restrictions of reload mode [" + options.mode + "]");

                        writer.setClasses(DXTickStream.getClassDescriptors(source));
                        writer.onSchemaChange(version);
                        cursor.reset(start);
                    }
                    else {
                        try {
                            if (updateMonitor)
                                monitor.setProgress((double) (msg.getTimeStampMs() - sourceRange[0]) / (double) rangeLength);

                            writer.send(msg);
                            lastTime = msg.getTimeStampMs();
                        } catch (IllegalArgumentException ex) {
                            // may occurs when decoding old messages after schema change
                        }
                    }
                } catch (CursorException ex) {
                    LOGGER.warn("Re-creating cursor due to cursor error: %s").with(ex);
                    // recreate cursor
                    msm.closeAndRemove(cursor);
                    cursor = source.select(lastTime, so, options.types, options.entities);
                    msm.add(cursor);

                    msm.setException(null); // clear exception
                } catch (Throwable ex) {
                    // may occurs when decoding old messages after schema change
                    LOGGER.error("Error: %s").with(ex);
                    throw ex;
                }
            }

            writer.close();

        } finally {
            msm.close();
        }

        writeStreamOptions(new File(folder, prefix + ".xml"), source.getStreamOptions());

//        FileOutputStream out = null;
//        try {
//            out = new FileOutputStream(new File(folder, prefix + ".xml"));
//            TDBProtocol.writeStreamOptions(new DataOutputStream(out), source.getStreamOptions());
//        } catch (IOException e) {
//            LOGGER.log(Level.WARNING, "Cannot store stream options.", e);
//        } finally {
//            Util.close(out);
//        }

        if (lock != null)
            lock.close();

        LOGGER.info("Backup completed.");
    }

    private long            getFileTime(File file) {
        try {
            String value = file.getName().substring(0, file.getName().indexOf("-"));
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid timestamp in backup file: " + file);
        }

        return -1;
    }

    long    truncateFiles(long time, ArrayList<File> files) {
        int index = 0;
        for (index = 0; index < files.size(); index++) {
            long ts = getFileTime(files.get(index));
            if (time < ts)
                break;
        }

        for (int i = files.size() - 1; i >= index; i--)
            files.remove(i).delete();

        if (index == 0 || index >= files.size())
            return Long.MIN_VALUE;
        else
            return getFileTime(files.get(index - 1)) + 1;
    }

    void    clearFiles(ArrayList<File> files) {
        for (File file : files)
            file.delete();

        files.clear();
    }

    private String            getStreamName(File file) {
        String value = file.getName().substring(file.getName().indexOf("-") + 1);
        return SimpleStringCodec.DEFAULT_INSTANCE.decode(value.replace(EXTENSION, ""));
    }

    private static String getPrefix(String key) {
        String prefix = SimpleStringCodec.DEFAULT_INSTANCE.encode(key);
        // prefix length must be >=3 to satisfy File.createTempFile
        if (prefix.length() < 3)
            prefix += prefix.length() == 1 ? "  " : " ";

        return prefix;
    }

    private long            getVersion(RawMessage msg) {
        final UnboundDecoder decoder = truncateMsgDecoder;
        buffer.setBytes(msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);
        decoder.nextField();
        return decoder.getLong();
    }

    private long            getNanoTime(RawMessage msg) {
        final UnboundDecoder decoder = truncateMsgDecoder;
        buffer.setBytes (msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);
        decoder.nextField();
        decoder.nextField();

        try {
            return decoder.getLong();
        } catch (NullValueException e) {
            return Long.MIN_VALUE;
        }
    }

    private IdentityKey[]    getInstruments(RawMessage msg) {
        final UnboundDecoder decoder = truncateMsgDecoder;
        buffer.setBytes (msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);
        decoder.nextField();
        decoder.nextField();
        decoder.nextField();
        return parseInstruments(decoder.getString());
    }

    private String                  readInstruments(RawMessage msg) {
        final UnboundDecoder decoder = truncateMsgDecoder;
        buffer.setBytes (msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);
        decoder.nextField();
        decoder.nextField();
        decoder.nextField();
        return decoder.getString();
    }

    private boolean         isDataConverted(RawMessage msg) {
        final UnboundDecoder decoder = schemaMsgDecoder;
        buffer.setBytes (msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);
        decoder.nextField();
        decoder.nextField();
        return decoder.getBoolean();
    }

    private IdentityKey[]    parseInstruments(String value) {
        String[] values = value.split(";");
        ArrayList<IdentityKey> keys = new ArrayList<IdentityKey>();

        for (String key : values) {
            keys.add(new ConstantIdentityKey(key));
        }

        return keys.toArray(new IdentityKey[keys.size()]);
    }
    
    private boolean         hasStream(DXTickDB db, String name) {
        DXTickStream[] streams = db.listStreams();

        for (DXTickStream stream : streams) {
            try {
                if (name.equals(stream.getName()))
                    return true;
            } catch (UnknownStreamException e) {
                // ignore
            } catch (ServerException e) {
                // ignore
            }
        }

        return false;
    }

    private static RandomAccessFileStore lock(File folder) throws IOException {

        if (!folder.exists() && !folder.mkdirs())
            throw new IOException("Failed create folder: " + folder);

        File lockFile = new File(folder, LOCK_FILE_NAME);

        boolean exists = lockFile.exists();
        if (!exists) {
            if (!lockFile.createNewFile())
                throw new IOException("Failed create lock file in " + folder);
        }

        LockFile raf = new LockFile(lockFile, exists ? LockFile.State.TERMINATED : LockFile.State.CLOSED);
        boolean success = false;
        try {
            raf.open(false);

            success = true;
            return raf;
        } finally {
            if (!success)
                Util.close(raf);
        }
    }

}