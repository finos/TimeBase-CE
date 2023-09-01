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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.data.stream.MessageSourceMultiplexerFixed;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.mon.InstrumentChannelStatSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.InstrumentChannelStats;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SymbolAndTypeSubscriptionControllerAdapter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.collections.ElementsEnumeration;
import com.epam.deltix.util.collections.IndexedArrayList;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeKeeper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.*;

/**
 * Modification of
 *
 * @author Alexei Osipov
 */
class TickCursorImplFixed implements
        TickCursor,
        TBCursor,
        SubscriptionManager,
        SymbolAndTypeSubscriptionControllerAdapter {

    //
    //  Immutable objects initialized in the constructor
    //
    private final TickDBImpl db;
    private final SelectionOptions options;

    //
    //  Subscription control objects, guarded by "lock"
    //
    @GuardedBy("lock")
    private MessageSourceMultiplexerFixed<InstrumentMessage> mx;
    @GuardedBy("lock")
    private MessageSourceMultiplexerStub<InstrumentMessage> stubMx;

    private final ObjectToObjectHashMap<ServerStreamImpl, StreamSource> subscribedStreams =
            new ObjectToObjectHashMap<>();

    private InstrumentToObjectMap<Byte> subscribedEntities =
            new InstrumentToObjectMap<>();

    protected boolean isSubscribedToAllEntities = false;
    protected Set<String> subscribedTypeNames = null;

    //
    //  Current message data, guarded by the virtual query thread
    //
    protected InstrumentMessage currentMessage;
    protected RecordClassDescriptor currentType;
    protected TickStream currentStream;
    protected TypedMessageSource currentSource = null;

    //
    //  Support for indexes (stream, entity, type)
    //
    private final InstrumentIndex instrumentIndex =
            new InstrumentIndex();

    private final IndexedArrayList<String> streamKeyIndex = new IndexedArrayList<>();

    private final IndexedArrayList<RecordClassDescriptor> typeIndex = new IndexedArrayList<>();
    private final Object lock = new Object();

    //
    //  Monitoring objects
    //
    private final long openTime;
    private final long monId;
    protected final InstrumentChannelStatSet stats;
    private volatile long resetTime = Long.MIN_VALUE;
    private volatile long closeTime = Long.MIN_VALUE;
    private volatile boolean closed = false;
    private String                              user;
    private String                              application;

    //
    //  Async support
    //
    private volatile Runnable callerAvLnr = null;
    protected Runnable delayedListenerToCall = null;
    private Throwable delayedLnrTrace = null;
    private final Runnable avlnrScheduler = new Runnable() {

        public void run() {
            if (Thread.holdsLock(mx)) {
                delayedListenerToCall = callerAvLnr;

                if (TickDBImpl.ASSERTIONS_ENABLED)
                    delayedLnrTrace = new Throwable("Delayed listener caller");
            } else {
                Runnable lnr = callerAvLnr;

                if (lnr != null)
                    lnr.run();
            }
        }
    };


    private final MessageSourceMultiplexer.ExceptionHandler xhandler =
            new MessageSourceMultiplexer.ExceptionHandler() {
                public void nextThrewException(@Nonnull MessageSource<?> feed, @Nonnull RuntimeException x) {
                    assert Thread.holdsLock(mx);

                    boolean handled = false;
                    if (feed instanceof TickStreamRelated) {
                        ServerStreamImpl stream = (ServerStreamImpl) ((TickStreamRelated) feed).getStream();
                        StreamSource source = subscribedStreams.get(stream, null);
                        if (source != null) {
                            handled = source.handle(feed, x);
                        }
                    }
                    if (!handled) {
                        throw x;
                    }
                }
            };

    @Nullable
    private Runnable lnrTriggered() {
        assert Thread.holdsLock(mx);

        Runnable lnr = delayedListenerToCall;

        if (lnr == null) {
            return null;
        }

        delayedListenerToCall = null;
        return lnr;
    }

    public TickCursorImplFixed(@Nullable TickDBImpl dbImpl, @Nullable SelectionOptions options, @Nullable String[] types, @Nullable IdentityKey[] entities, TickStream... streams) {
        if (options == null) {
            options = new SelectionOptions();
        }

        if (options.isLive()) {
            throw new IllegalArgumentException("Live queue type is not supported");
        }
        if (options.isRealTimeNotification()) {
            throw new IllegalArgumentException("Realtime notification is not supported");
        }
        if (options.ordered) {
            throw new IllegalArgumentException("ordered stream option is not supported");
        }

        this.db = dbImpl;
        this.options = options;
        //this.types = types;
        //this.entities = entities;


        //
        //  Monitoring
        //
        this.openTime = TimeKeeper.currentTime;
        this.stats = new InstrumentChannelStatSet(db);
        this.monId = db != null ? db.registerCursor(this) : -1;

        if (types != null) {
            setTypesInternal(types);
        }
        if (entities == null) {
            subscribeToAllEntitiesInternal();
        } else {
            addEntitiesInternal(entities);
        }

        List<MessageSource<InstrumentMessage>> messageSources = addStreamsInternal(streams);

        this.mx = createFixedMultiplexor(messageSources, resetTime);
    }

    @Nonnull
    private MessageSourceMultiplexerFixed<InstrumentMessage> createFixedMultiplexor(List<MessageSource<InstrumentMessage>> messageSources, long time) {
        return new MessageSourceMultiplexerFixed<>(xhandler, messageSources, !this.options.reversed, time, lock);
    }

    private void setTypesInternal(String... types) {
        assert subscribedStreams.isEmpty();
        assert subscribedTypeNames == null;
        assert delayedListenerToCall == null : dlnrDiag();
        subscribedTypeNames = new HashSet<>(Arrays.asList(types));
    }

    private void subscribeToAllEntitiesInternal() {
        assert subscribedStreams.isEmpty();
        assert delayedListenerToCall == null : dlnrDiag();

        isSubscribedToAllEntities = true;
    }

    private void addEntitiesInternal(IdentityKey... ids) {
        assert subscribedStreams.isEmpty();
        assert !isSubscribedToAllEntities;
        assert delayedListenerToCall == null : dlnrDiag();

        for (IdentityKey id : ids) {
            subscribedEntities.put(id, Byte.MIN_VALUE);
        }
    }

    private List<MessageSource<InstrumentMessage>> addStreamsInternal(TickStream... tickStreams) {
        assert subscribedStreams.isEmpty();
        assert delayedListenerToCall == null : dlnrDiag();

        // FIRST register to be notified of new symbols
        for (TickStream stream : tickStreams) {
            ((ServerStreamImpl) stream).cursorCreated(this);
        }


        boolean allTypesSubscribed = subscribedTypeNames == null;

        ArrayList<ServerStreamImpl> newStreams = new ArrayList<>();
        ArrayList<IdentityKey> entities = new ArrayList<>(subscribedEntities.keySet());
        ArrayList<String> types = allTypesSubscribed ? null : new ArrayList<>(subscribedTypeNames);


        long time = Long.MIN_VALUE; // Time is not set (yet)

        this.stubMx = new MessageSourceMultiplexerStub<>();

        synchronized (stubMx) { // Some sources expect that we hold lock on stubMx. So we have to syncronize here.
            for (TickStream stream : tickStreams) {
                ServerStreamImpl ssi = (ServerStreamImpl) stream;

                if (!subscribedStreams.containsKey(ssi)) {
                    newStreams.add(ssi);
                    StreamSource source = createSource(ssi, stubMx);
                    subscribedStreams.put(ssi, source);

                    if (isSubscribedToAllEntities) {
                        source.subscribeToAllEntities(time);
                    } else {
                        source.addEntities(time, subscribedEntities.keySet());
                    }
                }
            }
        }


        for (ServerStreamImpl s : newStreams) {
            if (allTypesSubscribed) {
                s.allTypesAdded(this);
            } else {
                s.typesChanged(this, types, null);
            }

            if (isSubscribedToAllEntities) {
                s.allEntitiesAdded(this);
            } else {
                s.entitiesChanged(this, entities, null);
            }
        }

        return stubMx.getAddedFeeds();
    }


    public void setAvailabilityListener(
            final Runnable lnr
    ) {
        callerAvLnr = lnr;
        mx.setAvailabilityListener(lnr == null ? null : avlnrScheduler);
    }

    protected final String dlnrDiag() {
        return (
                "Stuck delayed call to " + delayedListenerToCall +
                        " at " + Util.printStackTrace(delayedLnrTrace)
        );
    }

    public void notifySymbolCreated(DXTickStream stream, IdentityKey id) {
        Runnable lnr;

        synchronized (lock) {
            // is mx closed?
            if (mx.syncIsClosed())
                return;

            assert delayedListenerToCall == null : dlnrDiag();

            StreamSource source = subscribedStreams.get((ServerStreamImpl) stream, null);
            if (source != null)
                source.entityCreated(id);

            lnr = lnrTriggered();
        }

        if (lnr != null)
            lnr.run();
    }

    protected boolean isSubscribed(IdentityKey iid) {
        // no allocations
        return subscribedEntities.containsKey(iid);
    }

    /**
     * WARNING! This method should be updated only together with {@link TickCursorImpl_PDStream#next()}.
     * After any change you should copy-paste code of this method to {@link TickCursorImpl_PDStream#next()}
     */
    @Override
    public boolean next() {
        boolean ret;
        RuntimeException x = null;
        Runnable lnr;

        synchronized (lock) {
            assert delayedListenerToCall == null : dlnrDiag();

            while (true) {
                boolean hasNext;

                try {
                    hasNext = mx.syncNext();
                } catch (RuntimeException xx) {
                    x = xx;
                    ret = false;    // make compiler happy
                    break;
                }

                if (!hasNext) {
                    ret = false;
                    break;
                }

                currentMessage = mx.syncGetMessage();

                if (!isSubscribedToAllEntities && !isSubscribed(currentMessage)) {
                    discardMessageBecauseOfWrongEntity();
                    continue;
                }

                final TypedMessageSource source = (TypedMessageSource) mx.syncGetCurrentSource();

                // Instead of getting currentStream we will store currentSource so we can get currentStream lazily
                currentStream = null; //(((TickStreamRelated) source).getStream ());
                // Store source to make it possible to get "currentStream" later
                currentSource = source;

                // Instead of getting currentType we will store currentSource and currentMessage to get currentType in lazy way
                currentType = null;

                if (subscribedTypeNames != null && isSubscribedToCurrentType()) {
                    discardMessageBecauseOfWrongType();
                    continue;
                }

                stats.register(currentMessage);

                ret = true;
                break;
            }
            //
            //  Surprisingly, even mx.next () can call the av lnr (on truncation)
            //
            lnr = lnrTriggered();
        }

        if (lnr != null) {
            lnr.run();
        }

        if (x != null) {
            throw x;
        }

        return (ret);
    }

    private void discardMessageBecauseOfWrongType() {
        if (DebugFlags.DEBUG_MSG_DISCARD) {
            DebugFlags.discard(
                    "TB DEBUG: Discarding message " +
                            currentMessage + " because we are not subscribed to its type"
            );
        }

        assertIsOpen();
    }

    private boolean isSubscribedToCurrentType() {
        return !subscribedTypeNames.contains(getCurrentType().getName());
    }

    private void discardMessageBecauseOfWrongEntity() {
        if (DebugFlags.DEBUG_MSG_DISCARD) {
            DebugFlags.discard(
                    "TB DEBUG: Discarding message " +
                            currentMessage + " because we are not subscribed to its entity"
            );
        }

        assertIsOpen();
    }

    protected void assertIsOpen() {
        if (closed)
            throw new CursorIsClosedException();
    }

    public boolean isAtEnd() {
        synchronized (lock) {
            assert delayedListenerToCall == null : dlnrDiag();

            return (mx.syncIsAtEnd());
        }
    }

    public int getCurrentStreamIndex() {
        TickStream currentStream = getCurrentStream();
        if (currentStream == null)
            return -1;

        return (streamKeyIndex.getIndexOrAdd(currentStream.getKey()));
    }

    public TickStream getCurrentStream() {
        TickStream val = this.currentStream;
        if (val == null) {
            TypedMessageSource cs = this.currentSource;
            if (cs != null) {
                val = ((TickStreamRelated) cs).getStream();
                this.currentStream = val;
            }
        }
        return val;
    }

    public String getCurrentStreamKey() {
        return (getCurrentStream().getKey());
    }

    //
    //  Basic cursor and MessageInfo implementation.
    //  The result of these methods between calls to next ()
    //  may NOT be changed by asynchronous calls to any control methods.
    //  The values returned by these methods may only change while in next (),
    //  including the CONTENT of currentMessage.
    //
    public InstrumentMessage getMessage() {
        return (currentMessage);
    }

    public boolean isClosed() {
        synchronized (lock) {
            assert delayedListenerToCall == null : dlnrDiag();

            return (mx.syncIsClosed());
        }
    }

    public int getCurrentEntityIndex() {
        return (instrumentIndex.getOrAdd(currentMessage));
    }

    public RecordClassDescriptor getCurrentType() {
        RecordClassDescriptor currentType = this.currentType;
        if (currentType == null) {
            if (currentMessage.getClass() == RawMessage.class) {
                currentType = ((RawMessage) currentMessage).type;
            } else {
                currentType = currentSource.getCurrentType();
            }
            this.currentType = currentType;
        }
        return currentType;
    }

    public int getCurrentTypeIndex() {
        return (typeIndex.getIndexOrAdd(getCurrentType()));
    }

    //
    //  TimeController implementation
    //
    public void setTimeForNewSubscriptions(long time) {

    }

    //
    //  TBCursor, etc., implementation
    //
    public SelectionOptions getOptions() {
        return (options);
    }

    public String[] getSourceStreamKeys() {
        String[] keys = new String[subscribedStreams.size()];
        ElementsEnumeration streams = subscribedStreams.keys();
        int n = 0;
        while (streams.hasMoreElements()) {
            ServerStreamImpl ss = (ServerStreamImpl) streams.nextElement();
            keys[n++] = ss.getKey();
        }
        return keys;
    }

    public long getCloseTime() {
        return (closeTime);
    }

    public Date getCloseDate() {
        return (closeTime == Long.MIN_VALUE ? null : new Date(closeTime));
    }

    public InstrumentChannelStats[] getInstrumentStats() {
        return (stats.getInstrumentStats());
    }

    public long getOpenTime() {
        return (openTime);
    }

    public Date getOpenDate() {
        return (new Date(openTime));
    }

    public long getLastResetTime() {
        return (resetTime);
    }

    public Date getLastResetDate() {
        return (resetTime == Long.MIN_VALUE ? null : new Date(resetTime));
    }

    public long getId() {
        return (monId);
    }

    public long getTotalNumMessages() {
        return stats.getTotalNumMessages();
    }

    public long getLastMessageTimestamp() {
        return stats.getLastMessageTimestamp();
    }

    public long getLastMessageSysTime() {
        return stats.getLastMessageSysTime();
    }

    public Date getLastMessageSysDate() {
        return stats.getLastMessageSysDate();
    }

    public Date getLastMessageDate() {
        return stats.getLastMessageDate();
    }

    @Override
    public void close() {
        Runnable lnr;

        closed = true;

        synchronized (lock) {
            assert delayedListenerToCall == null : dlnrDiag();

            if (mx.syncIsClosed())
                return;

            clearAllReaders(false);

            mx.close();

            lnr = lnrTriggered();
        }

        if (lnr != null)
            lnr.run();

        if (db != null)
            db.unregisterCursor(this);

        closeTime = TimeKeeper.currentTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("TickCursorImpl(").append(getId()).append(") <== [");

        String[] keys = getSourceStreamKeys();
        for (int i = 0; i < keys.length; i++) {
            sb.append(i > 0 ? "', " : "'");
            sb.append(keys[i]);
            sb.append("' ");
        }
        sb.append("]");

        return (sb.toString());
    }

    @Override
    public void addEntity(IdentityKey id) {
        throw new UnsupportedOperationException();
    }

    public void addEntities(
            IdentityKey[] ids,
            int offset,
            int length
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEntity(IdentityKey id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeEntities(
            IdentityKey[] ids,
            int offset,
            int length
    ) {

        throw new UnsupportedOperationException();
    }

    public void clearAllEntities() {
        throw new UnsupportedOperationException();
    }


    public void subscribeToAllEntities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(IdentityKey[] ids, String[] types) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(IdentityKey[] ids, String[] types) {
        throw new UnsupportedOperationException();
    }

    //
    //  SubscriptionManager implementation
    //

    @Override
    public IdentityKey[] getSubscribedEntities() {
        return subscribedEntities.keySet().toArray(new IdentityKey[subscribedEntities.size()]);
    }

    @Override
    public boolean isAllEntitiesSubscribed() {
        return isSubscribedToAllEntities;
    }

    @Override
    public String[] getSubscribedTypes() {
        return subscribedTypeNames != null ?
                subscribedTypeNames.toArray(new String[subscribedTypeNames.size()]) :
                new String[0];
    }

    @Override
    public boolean isAllTypesSubscribed() {
        return subscribedTypeNames == null;
    }

    @Override
    public boolean hasSubscribedTypes() {
        return subscribedTypeNames == null || !subscribedTypeNames.isEmpty();
    }


    public void addStream(TickStream... tickStreams) {
        throw new UnsupportedOperationException();
    }

    private StreamSource createSource(DXTickStream stream, MessageSourceMultiplexerStub<InstrumentMessage> feedAggregator) {
        if (stream instanceof ServerStreamWrapper)
            stream = ((ServerStreamWrapper) stream).getNestedInstance();

        if (stream instanceof PDStream) {
            return new PDStreamSource((PDStream) stream, feedAggregator, options);
        } else if (stream instanceof SingleChannelStream) {
            return new SingleStreamSource(feedAggregator, stream, options);
        }
//        else if (stream instanceof FBStream) {
//            return new FBStreamSource((FBStream) stream, mx, options);
//        }

        throw new IllegalStateException("Stream is not supported yet: " + stream.getClass());
    }


    public void removeStream(TickStream... tickStreams) {
        throw new UnsupportedOperationException();
    }

    private void clearAllReaders(boolean retainEntitySubscription) {
        // TODO: Rewrite
        if (!retainEntitySubscription)
            subscribedEntities.clear();

        for (StreamSource ss : subscribedStreams)
            ss.close();

        ElementsEnumeration keys = subscribedStreams.keys();
        while (keys.hasMoreElements()) {
            ServerStreamImpl s = (ServerStreamImpl) keys.nextElement();
            s.cursorClosed(this);
        }

        subscribedStreams.clear();

        //mx.clearSources ();
    }

    public void removeAllStreams() {
        throw new UnsupportedOperationException();
    }

    public void addTypes(String... types) {
        throw new UnsupportedOperationException();
    }

    public void setTypes(String... types) {
        throw new UnsupportedOperationException();
    }


    public void removeTypes(String... types) {
        throw new UnsupportedOperationException();
    }

    public void subscribeToAllTypes() {
        throw new UnsupportedOperationException();
    }


    public void reset(long time) {
        Runnable lnr;

        synchronized (lock) {
            if (time == TimeConstants.USE_CURRENT_TIME)
                time = TimeKeeper.currentTime;

            assert delayedListenerToCall == null : dlnrDiag();

            if (mx.syncIsClosed())
                throw new IllegalStateException(this + " is closed.");

            mx.close();
            stubMx.clear();

            for (StreamSource ssi : subscribedStreams) {
                ssi.reset(time);
            }

            resetTime = time;
            this.mx = createFixedMultiplexor(stubMx.getAddedFeeds(), time);

            lnr = lnrTriggered();
        }

        if (lnr != null) {
            lnr.run();
        }
    }

    @Override
    public boolean isRealTime() {
        return false;
    }

    @Override
    public boolean realTimeAvailable() {
        return false;
    }

    public static boolean isSupported(SelectionOptions options) {
        return options.restrictStreamType && !(options.isLive() || options.isRealTimeNotification() || options.ordered);
    }

    /**
     * This class acts as adaptor between {@link TickCursorImplFixed} and classes that depend on {@link MessageSourceMultiplexer}.
     *
     * Main idea that we let classes use this stub to create sources and then pass those sources to {@link MessageSourceMultiplexerFixed}.
     *
     */
    private static final class MessageSourceMultiplexerStub<T extends TimeStampedMessage> extends PrioritizedMessageSourceMultiplexer<T> {
        private final List<MessageSource<T>> feeds = new ArrayList<>();

        @Override
        public void                     add (MessageSource <T> feed, long fastForwardToTime) {
            add(feed);
        }

        @Override
        public void                     add (MessageSource <T> feed) {
            feeds.add(feed);
        }

        @Override
        public boolean     closeAndRemove (MessageSource <T> feed) {
            return feeds.remove(feed);
        }


        public List<MessageSource<T>> getAddedFeeds() {
            return feeds;
        }

        public void clear() {
            feeds.clear();
        }
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public void setApplication(String application) {
        this.application = application;
    }
}