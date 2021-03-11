package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex4;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.mon.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.collections.*;
import com.epam.deltix.util.collections.generated.LongToIntegerHashMap;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.lang.Wrapper;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.lang.Util;

import javax.annotation.Nonnull;
import java.util.*;

/**
 *
 */
class TickCursorImpl implements 
    TickCursor,
    TBCursor,
    SubscriptionManager,
        SymbolAndTypeSubscriptionControllerClient
{
    //
    //  Immutable objects initialized in the constructor
    //
    private final TickDBImpl                    db;
    private final SelectionOptions              options;
    
    private final PrioritizedMessageSourceMultiplexer.ExceptionHandler xhandler =
        new PrioritizedMessageSourceMultiplexer.ExceptionHandler () {
            public void nextThrewException (@Nonnull MessageSource<?> feed, @Nonnull RuntimeException x) {
                assert Thread.holdsLock (mx);

                boolean handled = false;
                if (feed instanceof TickStreamRelated) {
                    ServerStreamImpl stream = (ServerStreamImpl) ((TickStreamRelated)feed).getStream();
                        if (stream instanceof Wrapper)
                            stream = (ServerStreamImpl) ((Wrapper) stream).getNestedInstance();

                    StreamSource source = subscribedStreams.get(stream, null);
                    if (source != null)
                        handled = source.handle(feed, x);
                    else
                       throw x;
                }

                    if (!handled)
                        throw x;
            }
        };

    static final class MessageComparator<T extends InstrumentMessage> implements Comparator<T> {
        @Override
        public final int      compare(T a, T b) {
            return a.compareTo(b);
        }
    }

    static final class TimeComparator<T extends InstrumentMessage> implements Comparator<T> {
        @Override
        public final int      compare(T a, T b) {
            //if (a.hasNanoTime() && b.hasNanoTime())
            return Util.compare(a.getNanoTime(), b.getNanoTime());
            
            //return a.getTimeStampMs() > b.timestamp ? 1 : -1;
        }
    }

    private static class CursorMultiplexer extends PrioritizedMessageSourceMultiplexer<InstrumentMessage> {

        private final SelectionOptions options;

        CursorMultiplexer(SelectionOptions o) {
            super (!o.reversed, o.realTimeNotification, o.ordered ?
                    new MessageComparator<InstrumentMessage>() : new TimeComparator<InstrumentMessage>());
            /*
            if (o.ordered) {
                throw new IllegalArgumentException("SelectionOptions.ordered is not supported anymore! If you have use cases that need it please report to TimeBase dev team.");
            }
            */
            this.options = o;
        }
        
        @Override
        public InstrumentMessage    createRealTimeMessage() {
            long currentTime = getCurrentTime();
            long time = currentTime != Long.MAX_VALUE ? currentTime : Long.MIN_VALUE;
            return TickStreamImpl.createRealTimeStartMessage(options.raw, time);
        }

        @Override
        public boolean isRealTimeMessage(InstrumentMessage message) {
            if (options.raw)
                return ((RawMessage)message).type.getGuid().equals(RealTimeStartMessage.DESCRIPTOR_GUID);

            return message instanceof RealTimeStartMessage;
        }

        @Override
        protected boolean isEmpty() {
            boolean resultFromParent = super.isEmpty();
            if (resultFromParent || !options.versionTracking || options.isLive()) {
                return resultFromParent;
            }
            return hasOnlyStreamVersionsReaders();
        }

        private boolean hasOnlyStreamVersionsReaders() {
            // TODO: remove this hack
            // ignore StreamVersionsReaders - they will block historical cursor next
            for (PrioritizedSource<InstrumentMessage> source : emptySources) {
                if (!(source.getSrc() instanceof StreamVersionsReader))
                    return false;
            }

            return true;
        }
    }

    //
    //  Subscription control objects, guarded by "mx"
    //
    protected final PrioritizedMessageSourceMultiplexer <InstrumentMessage> mx;
    private long                                newSubscriptionTime =
        TimeConstants.USE_CURSOR_TIME;

    private final ObjectToObjectHashMap<ServerStreamImpl, StreamSource> subscribedStreams =
            new ObjectToObjectHashMap<ServerStreamImpl, StreamSource>();

    private InstrumentToObjectMap<Byte>         subscribedEntities =
            new InstrumentToObjectMap<Byte>();

    protected boolean                             isSubscribedToAllEntities = false;
    protected Set <String>                        subscribedTypeNames = null;
    
    //
    //  Current message data, guarded by the virtual query thread
    //
    protected InstrumentMessage                   currentMessage;
    protected RecordClassDescriptor               currentType;
    protected ServerStreamImpl                    currentStream;
    //protected TypedMessageSource                  currentSource = null;

    //
    //  Support for indexes (stream, entity, type)
    //
    private final InstrumentIndex4               instrumentIndex =
        new InstrumentIndex4(64);

    private final LongToIntegerHashMap           streamKeyIndex = new LongToIntegerHashMap();

    private final IndexedArrayList <RecordClassDescriptor> typeIndex =
        new IndexedArrayList <RecordClassDescriptor> ();

    protected boolean                             inRealtime = false;
    protected boolean                             realTimeNotifications = false;

    //
    //  Monitoring objects
    //
    private final long                          openTime;
    private final long                          monId;
    protected final InstrumentChannelStatSet      stats;
    private volatile long                       resetTime = Long.MIN_VALUE;
    private volatile long                       closeTime = Long.MIN_VALUE;
    private volatile boolean                    closed = false;
    private String                              user;
    private String                              application;
    //
    //  Async support
    //
    private volatile Runnable                   callerAvLnr = null;
    protected Runnable                            delayedListenerToCall = null;
    private Throwable                           delayedLnrTrace = null;
    private final Runnable                      avlnrScheduler =        new Runnable () {

        public void             run () {
            if (Thread.holdsLock (mx)) {
                delayedListenerToCall = callerAvLnr;
                
                if (TickDBImpl.ASSERTIONS_ENABLED)
                    delayedLnrTrace = new Throwable ("Delayed listener caller");
            }
            else {
                Runnable    lnr = callerAvLnr;

                if (lnr != null)
                    lnr.run ();
            }
        }
    };

//    private TickStream getCurrentStreamInternal() {
//        TickStream val = this.currentStream;
//        if (val == null) {
//            TypedMessageSource cs = this.currentSource;
//            if (cs != null) {
//                val = ((TickStreamRelated) cs).getStream ();
//                this.currentStream = val;
//            }
//        }
//        return val;
//    }

    protected Runnable            lnrTriggered () {
        assert Thread.holdsLock (mx);

        Runnable    lnr = delayedListenerToCall;

        if (lnr == null)
            return (null);

        delayedListenerToCall = null;
        return (lnr);
    }

    protected static class ConstructorOptions {
        final SelectionOptions options;
        final PrioritizedMessageSourceMultiplexer <InstrumentMessage> msm;

        public ConstructorOptions(SelectionOptions selectionOptions, PrioritizedMessageSourceMultiplexer<InstrumentMessage> msm) {
            this.options = selectionOptions;
            this.msm = msm;
        }
    }

    TickCursorImpl (TickDBImpl db, SelectionOptions options, TickStream ... streams) {
        this(db, getOpts(options), streams);
    }

    private static ConstructorOptions getOpts(SelectionOptions options) {
        if (options == null)
            options = new SelectionOptions ();
        CursorMultiplexer cursorMultiplexer = new CursorMultiplexer(options);
        return new ConstructorOptions(options, cursorMultiplexer);
    }

    TickCursorImpl (TickDBImpl db, ConstructorOptions opts, TickStream ... streams) {
        this.db = db;
        this.options = opts.options;
        this.realTimeNotifications = options.realTimeNotification;

        mx = opts.msm;
        mx.setLive (options.live);

        if (options.live) // live mode should use allowLateOutOfOrder
            mx.setAllowLateOutOfOrder(true);
        else
            mx.setAllowLateOutOfOrder (options.allowLateOutOfOrder);
        mx.setExceptionHandler (xhandler);
        
        //
        //  Monitoring
        //
        openTime = TimeKeeper.currentTime;
        stats = new InstrumentChannelStatSet (db);
        monId = db != null ? db.registerCursor (this) : -1;

        addStream (streams);
    }

    public void                 setAvailabilityListener (
        final Runnable              lnr
    )
    {
        callerAvLnr = lnr;
        mx.setAvailabilityListener (lnr == null ? null : avlnrScheduler);
    }

    private long                getNewSubscriptionTime () {
        if (newSubscriptionTime == TimeConstants.USE_CURSOR_TIME)
            return (mx.getCurrentTime ());

        if (newSubscriptionTime == TimeConstants.USE_CURRENT_TIME)
            return (TimeKeeper.currentTime);

        return (newSubscriptionTime);
    }

    protected final String              dlnrDiag () {
        return (
            "Stuck delayed call to " + delayedListenerToCall +
            " at " + Util.printStackTrace (delayedLnrTrace)
        );
    }

    public void                        notifySymbolCreated (DXTickStream stream, IdentityKey id) {
        Runnable lnr;

        synchronized (mx) {
            // is mx closed?
            if (mx.syncIsClosed ())
                return;

            assert delayedListenerToCall == null : dlnrDiag ();

            StreamSource source = subscribedStreams.get((ServerStreamImpl) stream, null);
            if (source != null)
                source.entityCreated(id);

            lnr = lnrTriggered ();
        }

        if (lnr != null) 
            lnr.run ();
    }

    protected boolean                      isSubscribed(IdentityKey iid) {
        // no allocations
        return subscribedEntities.containsKey(iid);
    }

    /**
     * WARNING! This method should be updated only together with {@link TickCursorImpl_PDStream#next()}.
     * After any change you should copy-paste code of this method to {@link TickCursorImpl_PDStream#next()}
     */
    @Override
    public boolean              next () {
        boolean                 ret;
        RuntimeException        x = null;
        Runnable                lnr;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            for (;;) {
                boolean         hasNext;

                try {
                    hasNext = mx.syncNext ();
                } catch (RuntimeException xx) {
                    x = xx;
                    ret = false;    // make compiler happy
                    break;
                }

                if (!hasNext) {
                    ret = false;
                    break;
                }

                currentMessage = mx.syncGetMessage ();

                // current message is indicator of real-time mode
                if (realTimeNotifications && mx.isRealTimeStarted()) {
                    currentType = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                    ret = inRealtime = true;
                    break;
                }
                
                if (!isSubscribedToAllEntities && !isSubscribed(currentMessage)) {
                    if (DebugFlags.DEBUG_MSG_DISCARD) {
                        DebugFlags.discard (
                            "TB DEBUG: Discarding message " +
                            currentMessage + " because we are not subscribed to its entity"
                        );
                    }

                    assertIsOpen();

                    continue;
                }

                final TypedMessageSource  source =
                    (TypedMessageSource) mx.syncGetCurrentSource ();

                // Instead of getting currentStream we will store currentSource so we can get currentStream lazily
                currentStream = (ServerStreamImpl) ((TickStreamRelated) source).getStream ();
//                // Store source to make it possible to get "currentStream" later
//                currentSource = source;

                if (options.raw) //  currentMessage.getClass () == RawMessage.class
                    currentType = ((RawMessage) currentMessage).type;
                else
                    currentType = source.getCurrentType ();

                if (subscribedTypeNames != null &&
                        !subscribedTypeNames.contains (currentType.getName ()))
                {
                    if (DebugFlags.DEBUG_MSG_DISCARD) {
                        DebugFlags.discard (
                            "TB DEBUG: Discarding message " +
                            currentMessage + " because we are not subscribed to its type"
                        );
                    }

                    assertIsOpen();

                    continue;
                }

                stats.register (currentMessage);

                ret = true;
                break;
            }
            //
            //  Surprisingly, even mx.next () can call the av lnr (on truncation)
            //
            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();

        if (x != null)
            throw x;
        
        return (ret);
    }

    protected void assertIsOpen() {
        if (closed)
            throw new CursorIsClosedException();
    }

    public boolean          isAtEnd () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            return (mx.syncIsAtEnd ());
        }
    }

    public int              getCurrentStreamIndex () {
        if (currentStream == null)
            return -1;

        long streamIndex = currentStream.getIndex();

        int index = streamKeyIndex.get(streamIndex, -1);
        if (index == -1) {
            index = streamKeyIndex.size();
            streamKeyIndex.put(streamIndex, index);
        }

        return index;
    }

    public TickStream       getCurrentStream () {        
        return currentStream;
    }

    public String           getCurrentStreamKey () {
        return currentStream != null ? currentStream.getKey() : null;
    }

    //
    //  Subscription control
    //
    private ServerStreamImpl [] getSubStreamSnapshot () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            return (subscribedStreams.keysToArray (new ServerStreamImpl [subscribedStreams.size ()]));
        }
    }

    //
    //  Basic cursor and MessageInfo implementation.
    //  The result of these methods between calls to next ()
    //  may NOT be changed by asynchronous calls to any control methods.
    //  The values returned by these methods may only change while in next (),
    //  including the CONTENT of currentMessage.
    //
    public InstrumentMessage        getMessage () {
        return (currentMessage);
    }

    public boolean                  isClosed () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            return (mx.syncIsClosed ());
        }
    }

    public int                      getCurrentEntityIndex () {
        return (instrumentIndex.getOrAdd (currentMessage.getSymbol()));
    }

    public RecordClassDescriptor    getCurrentType () {
        return (currentType);
    }

    public int                      getCurrentTypeIndex () {
        return (typeIndex.getIndexOrAdd (getCurrentType ()));
    }

    //
    //  TimeController implementation
    //
    public void                 setTimeForNewSubscriptions (long time) {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            newSubscriptionTime = time;
        }
    }
    //
    //  TBCursor, etc., implementation
    //
    public SelectionOptions                 getOptions () {
        return (options);
    }

    public String []                        getSourceStreamKeys () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            int             n = subscribedStreams.size ();
            String []       keys = new String [n];

            n = 0;

            ElementsEnumeration streams = subscribedStreams.keys();

            while (streams.hasMoreElements()) {
                ServerStreamImpl ss = (ServerStreamImpl) streams.nextElement();
                keys [n++] = ss.getKey ();
            }

            return (keys);
        }
    }

    public long                             getCloseTime () {
        return (closeTime);
    }

    public Date                             getCloseDate () {
        return (closeTime == Long.MIN_VALUE ? null : new Date (closeTime));
    }

    public InstrumentChannelStats []        getInstrumentStats () {
        return (stats.getInstrumentStats ());
    }

    public long                             getOpenTime () {
        return (openTime);
    }

    public Date                             getOpenDate () {
        return (new Date (openTime));
    }

    public long                             getLastResetTime () {
        return (resetTime);
    }

    public Date                             getLastResetDate () {
        return (resetTime == Long.MIN_VALUE ? null : new Date (resetTime));
    }

    public long                             getId () {
        return (monId);
    }

    public long                             getTotalNumMessages () {
        return stats.getTotalNumMessages ();
    }

    public long                             getLastMessageTimestamp () {
        return stats.getLastMessageTimestamp ();
    }

    public long                             getLastMessageSysTime () {
        return stats.getLastMessageSysTime ();
    }

    public Date                             getLastMessageSysDate () {
        return stats.getLastMessageSysDate ();
    }

    public Date                             getLastMessageDate () {
        return stats.getLastMessageDate ();
    }

    @Override
    public void                             close () {
        Runnable        lnr;

        closed = true;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            if (mx.syncIsClosed ())
                return;

            clearAllReaders (false);

            mx.close ();
        
            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();

        if (db != null)
            db.unregisterCursor (this);
        
        closeTime = TimeKeeper.currentTime;        
    }
        
    @Override
    public String                           toString () {
        StringBuilder   sb = new StringBuilder ();

        sb.append ("TickCursorImpl(").append(getId()).append(") <== [");

        String[] keys = getSourceStreamKeys();
        for (int i = 0; i < keys.length; i++) {
            sb.append (i > 0 ? "', " : "'");
            sb.append (keys[i]);
            sb.append ("' ");
        }
        sb.append ("]");

        return (sb.toString ());
    }

    //
    //  EntitySubscriptionController implementation
    //
    /**
     *  Link the specified SEM entry to all subscribed streams.
     */
//    private void                subToAllStreams (StreamToEntryMap sem) {
//        for (ServerStreamImpl s : subscribedStreams)
//            if (s instanceof DurableStreamImpl)
//                addSub (sem, (DurableStreamImpl) s);
//    }

//    public void                 addEntity (IdentityKey id) {
//        Runnable lnr;
//
//        ArrayList<IdentityKey> added = new ArrayList<IdentityKey>();
//        boolean allRemoved = false;
//
//        synchronized (mx) {
//            assert delayedListenerToCall == null : dlnrDiag ();
//
//            if (isSubscribedToAllEntities) {
//                //
//                // protect sem against closing in removeAllEntitiesInternal ()
//                //
//                StreamToEntryMap        sem = subscribedEntities.remove (id);
//
//                if (sem == null) {
//                    removeAllEntitiesInternal ();
//                    allRemoved = true;
//
//                    addEntityInternal(id);
//                    added.add(id);
//                } else {
//                    //
//                    //  remove everything else
//                    //
//                    for (StreamToEntryMap xem : subscribedEntities.values ())
//                        removeEntityInternal (xem);
//
//                    subscribedEntities.clear ();
//                    subscribedEntities.add (sem);
//                    added.add(id);
//                }
//
//                isSubscribedToAllEntities = false;
//            } else {
//                addEntityInternal (id);
//                added.add(id);
//            }
//
//            lnr = lnrTriggered ();
//
//            // propagate subscription
//            for (ServerStreamImpl stream : subscribedStreams) {
//                if (stream instanceof PDStream)
//                    checkSource((PDStream)stream, isAllTypesSubscribed() ? null : getSubscribedTypes(), new IdentityKey[] {id});
//            }
//
//            // update real-time state
//            inRealtime = mx.isRealTime();
//        }
//
//        if (lnr != null)
//            lnr.run ();
//
//        // fire event
//        if (allRemoved)
//            fireAllEntitiesRemoved();
//
//        fireEntitiesChanged(added, null);
//    }

    private void                fireAllEntitiesSubscribed() {
        for (ServerStreamImpl s : getSubStreamSnapshot ())
            s.allEntitiesAdded(this);

    }

    private void                fireAllEntitiesRemoved() {
         for (ServerStreamImpl s : getSubStreamSnapshot ())
            s.allEntitiesRemoved(this);
    }

    private void                fireEntitiesChanged(Collection<IdentityKey> added,
                                                    Collection<IdentityKey> removed) {
        for (ServerStreamImpl s : getSubStreamSnapshot ())
            s.entitiesChanged(this, added, removed);
    }

    private void                fireAllTypesSubscribed() {
           for (ServerStreamImpl s : getSubStreamSnapshot ())
               s.allTypesAdded(this);
    }

    private void                fireAllTypesRemoved() {
        for (ServerStreamImpl s : getSubStreamSnapshot ())
           s.allTypesRemoved(this);
    }

    private void                fireTypesChanged(Collection<String> added, Collection<String> removed) {
        for (ServerStreamImpl s : getSubStreamSnapshot ())
            s.typesChanged(this, added, removed);
    }

    @Override
    public void addEntity(IdentityKey id) {
        addEntitiesInternal(id);
    }

    public void                 addEntities (
        IdentityKey []       ids,
        int                         offset,
        int                         length
    )
    {
        IdentityKey[] identities = ids;
        if (ids != null && ids.length != length)
            identities = Arrays.copyOfRange(ids, offset, offset + length);

        addEntitiesInternal(identities);
    }

    private  void                 addEntitiesInternal (IdentityKey  ... ids)
    {
        Runnable lnr;

        ArrayList<IdentityKey> added = new ArrayList<IdentityKey>();
        ArrayList<IdentityKey> removed = new ArrayList<IdentityKey>();

        boolean allRemoved = false;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            //  (Go to great lengths to) preserve open readers

            InstrumentToObjectMap<Byte> old = null;

            if (isSubscribedToAllEntities) {
                isSubscribedToAllEntities = false;
                old = subscribedEntities;
                subscribedEntities = new InstrumentToObjectMap<Byte>();
                allRemoved = true;
            }

            for (int i = 0; i < ids.length; i++) {
                IdentityKey id = ids[i];
                subscribedEntities.put(id, Byte.MIN_VALUE);

                if (old == null || old.remove(id) == null)
                    added.add(id);
            }

            long time = getNewSubscriptionTime();

            // propagate subscription
            for (StreamSource s : subscribedStreams)
                s.addEntities(time, ids);

            if (old != null)
                removed.addAll(old.keySet());

            // update real-time state
            inRealtime = mx.isRealTime();

            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();

        // fire events
        if (allRemoved)
            fireAllEntitiesRemoved();

        fireEntitiesChanged(added, removed);
    }

    /**
     *  Clear subscribedEntities and fileToEntryMap. Leave file stream readers
     *  intact.
     */
    private void                removeAllEntitiesInternal () {
        subscribedEntities.clear ();

        for (StreamSource source : subscribedStreams)
            source.clearAllEntities();
    }

    @Override
    public void                 removeEntity (IdentityKey id) {
        removeEntitiesInternal(new IdentityKey[] { id });
    }

    @Override
    public void                 removeEntities (
        IdentityKey []       ids,
        int                         offset,
        int                         length
    )
    {

        IdentityKey[] identities = ids;
        if (ids != null && ids.length != length)
            identities = Arrays.copyOfRange(ids, offset, offset + length);

        removeEntitiesInternal(identities);
    }

    private void                 removeEntitiesInternal (IdentityKey[] ids)
    {
        ArrayList<IdentityKey> removed = new ArrayList<IdentityKey>();

        boolean allSubscribed = isSubscribedToAllEntities;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            if (isSubscribedToAllEntities) {
                removeAllEntitiesInternal ();
                isSubscribedToAllEntities = false;
            } else {

                for (IdentityKey id : ids) {
                    if (subscribedEntities.remove(id) != null)
                        removed.add(id);
                }
                // propagate subscription
                for (StreamSource s : subscribedStreams)
                    s.removeEntities(ids);

            }

            // update real-time state
            inRealtime = mx.isRealTime();
        }

        if (allSubscribed)
            fireAllEntitiesRemoved();
        else
            fireEntitiesChanged(null, removed);
    }

    public void                 clearAllEntities () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            removeAllEntitiesInternal ();
            isSubscribedToAllEntities = false;

            // update real-time state
            inRealtime = mx.isRealTime();
        }

        fireAllEntitiesRemoved();
    }

//    private void                subscribeToEntireStream (DurableStreamImpl s) {
//        for (IdentityKey id : s.listEntities ()) {
//            StreamToEntryMap        sem = subscribedEntities.get (id);
//
//            if (sem == null) {
//                sem = new StreamToEntryMap (id);
//                subscribedEntities.add (sem);
//            }
//
//            if (!sem.isSubscribed(s))
//                addSub (sem, s);
//        }
//    }

//    private void                subscribeToEntireStream (PDStream s) {
//        checkSource(s, isAllTypesSubscribed() ? null : getSubscribedTypes(), isAllEntitiesSubscribed() ? null : getSubscribedEntities());
//    }

//    private InstrumentMessageSource                checkSource (PDStream s, String[] types, IdentityKey[] ids) {
//        InstrumentMessageSource source = streamsToSCS.get(s);
//
//        // empty subscription
//        if (ids != null && ids.length == 0)
//            return null;
//
//        long time = getNewSubscriptionTime();
//
//        boolean isNew = false;
//
//        if ((source == null || source.isClosed())) {
//            source = s.createSource(time, options, ids, types);
//            isNew = true;
//        } else {
//            source.setTimeForNewSubscriptions(time);
//
//            if (ids != null)
//                source.addEntities(ids, 0, ids.length);
//            else
//                source.subscribeToAllEntities();
//
//            if (types != null)
//                source.setTypes(types);
//            else
//                source.subscribeToAllTypes();
//        }
//
//        if (isNew) {
//            streamsToSCS.put(s, source);
//            mx.add(source, time);
//        }
//
//        return source;
//    }

    public void                 subscribeToAllEntities () {
        Runnable lnr;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            if (isSubscribedToAllEntities)
                return;

            // critical change it here - used below
            isSubscribedToAllEntities = true;

            long time = getNewSubscriptionTime();

            for (StreamSource s : subscribedStreams)
                s.subscribeToAllEntities(time);

            // update real-time state
            inRealtime = mx.isRealTime();

            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();

        fireAllEntitiesSubscribed();
    }

    @Override
    public void add(IdentityKey[] ids, String[] types) {
        addTypes(types);
        addEntitiesInternal(ids);
    }

    @Override
    public void remove(IdentityKey[] ids, String[] types) {
        removeEntitiesInternal(ids);
        removeTypes(types);
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

//    //
//    //  StreamSubscriptionController implementation
//    //
//    private void            addSpecialReaders (ServerStreamImpl s) {
//        long time = getNewSubscriptionTime ();
//
//        if (s instanceof TickStreamImpl) {
//            TickStreamImpl      tsi = (TickStreamImpl) s;
//
//            if (tsi.isUnique () && options.rebroadcast) {
//                UniqueMessageReader umr = new UniqueMessageReader (tsi.accumulator, options, tsi);
//
//                streamToSpecialReaderMap.put (tsi, umr);
//                mx.add (umr);
//            }
//
//            if (options.versionTracking && tsi.versioning) {
//                StreamVersionsReader reader = tsi.createVersionsReader(time, options);
//                if (reader != null) {
//                    streamToSpecialReaderMap.put(tsi, reader);
//                    mx.add(reader);
//                }
//            }
//        }
//
//        if (s instanceof FileStreamImpl) {
//            if (isSubscribedToAllEntities || !subscribedEntities.isEmpty()) {
//                SingleChannelStream fsi = (SingleChannelStream) s;
//
//                MessageSource <InstrumentMessage>   source = fsi.createSource (time, options, quickFilter);
//
//                streamToSpecialReaderMap.put (s, source);
//                mx.add (source, time);
//            }
//        }
//        else if (s instanceof TransientStreamImpl) {
//            SingleChannelStream fsi = (SingleChannelStream) s;
//
//            MessageSource <InstrumentMessage>   source =
//                fsi.createSource(time, options, quickFilter);
//
//            streamToSpecialReaderMap.put (s, source);
//            mx.add (source, time);
//        } else if (s instanceof ExternalStreamImpl) {
//            ExternalStreamImpl externalStream = (ExternalStreamImpl) s;
//
//            MessageSource <InstrumentMessage> source;
//            if (isSubscribedToAllEntities)
//                source = externalStream.createSource(time, options, (IdentityKey[]) null);
//            else
//                source = externalStream.createSource(time, options, getSubscribedEntities());
//
//            streamToSpecialReaderMap.put (s, source);
//            mx.add (source, time);
//        }
//    }

//    private InstrumentMessageSource createSource(SingleChannelStream s, long time) {
//
//        InstrumentMessageSource   source = s.createSource (time, options);
//
//        // apply subscription
//        if (isAllEntitiesSubscribed())
//            source.subscribeToAllEntities();
//        else {
//            IdentityKey[] ids = subscribedEntities.toArray();
//            source.addEntities(ids, 0, ids.length);
//        }
//
//        if (isAllTypesSubscribed())
//            source.subscribeToAllTypes();
//        else
//            source.addTypes(getSubscribedTypes());
//
//        return source;
//    }

    public void                 addStream (TickStream ... tickStreams) {
        assertIsOpen();

        // FIRST register to be notified of new symbols
        for (TickStream stream : tickStreams)
            ((ServerStreamImpl) stream).cursorCreated (this);

        Runnable lnr;

        boolean allEntitiesSubscribed;
        boolean allTypesSubscribed;

        ArrayList<ServerStreamImpl> newStreams = new ArrayList<ServerStreamImpl>();
        ArrayList<IdentityKey> entities = new ArrayList<IdentityKey>();
        ArrayList<String> types = new ArrayList<String>();

        synchronized (mx) {
            allEntitiesSubscribed = isSubscribedToAllEntities;
            entities.addAll(subscribedEntities.keySet());

            allTypesSubscribed = subscribedTypeNames == null;
            if (!allTypesSubscribed)
                types.addAll(subscribedTypeNames);
            
            assert delayedListenerToCall == null : dlnrDiag ();

            long time = getNewSubscriptionTime();

            for (TickStream stream : tickStreams) {
                ServerStreamImpl    ssi = (ServerStreamImpl) stream;

                if (!subscribedStreams.containsKey(ssi)) {
                    newStreams.add(ssi);
                    StreamSource source = createSource(ssi);
                    subscribedStreams.put(ssi, source);

                    if (allEntitiesSubscribed)
                        source.subscribeToAllEntities(time);
                    else
                        source.addEntities(time, subscribedEntities.keySet());
                }
            }

            // update real-time state
            inRealtime = mx.isRealTime();

            lnr = lnrTriggered ();
        }

        for (ServerStreamImpl s : newStreams) {
            if (allTypesSubscribed)
                s.allTypesAdded(this);
            else
                s.typesChanged(this, types, null);
            
            if (allEntitiesSubscribed)
                s.allEntitiesAdded(this);
            else
                s.entitiesChanged(this, entities, null);
        }

        if (lnr != null)
            lnr.run ();
    }

    private StreamSource              createSource(DXTickStream stream) {
        assertIsOpen();

        if (stream instanceof ServerStreamWrapper)
            stream = ((ServerStreamWrapper) stream).getNestedInstance();

//        if (stream instanceof DurableStreamImpl) {
//            return new DurableStreamSource(mx, (DurableStreamImpl) stream, options);
//        } else
        if (stream instanceof PDStream) {
            return new PDStreamSource((PDStream) stream, mx,  options);
        } else if (stream instanceof SingleChannelStream){
            return new SingleStreamSource(mx, stream, options);
        }
//        else if (stream instanceof FBStream) {
//            return new FBStreamSource((FBStream) stream, mx, options);
//        }

        throw new IllegalStateException("Stream is not supported yet: " + stream.getClass());
    }

    public void                 removeStream (TickStream ... tickStreams) {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            for (TickStream stream : tickStreams) {
                ServerStreamImpl    ssi = (ServerStreamImpl) stream;

                StreamSource source = subscribedStreams.remove (ssi, null);
                if (source != null)
                    source.close();
            }

            // update real-time state
            inRealtime = mx.isRealTime();
        }

        // Release the lock
        for (TickStream stream : tickStreams) 
            ((ServerStreamImpl) stream).cursorClosed (this);        
    }

    private void                clearAllReaders (boolean retainEntitySubscription) {
        if (!retainEntitySubscription)
            subscribedEntities.clear ();

        for (StreamSource ss : subscribedStreams)
            ss.close();

        ElementsEnumeration keys = subscribedStreams.keys();
        while (keys.hasMoreElements()) {
            ServerStreamImpl s = (ServerStreamImpl) keys.nextElement();
            s.cursorClosed (this);
        }

        subscribedStreams.clear ();

        mx.clearSources ();
    }

    public void                 removeAllStreams () {
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            clearAllReaders (!isSubscribedToAllEntities);
        }
    }
    //
    //  TypeSubscriptionController implementation
    //
    public void                 addTypes (String ... types) {
        boolean allSubscribed;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            allSubscribed = subscribedTypeNames == null;
            if (allSubscribed)
                subscribedTypeNames = new HashSet <String> ();

            for (String s : types)
                subscribedTypeNames.add (s);
        }

        if (allSubscribed)
            fireAllTypesRemoved();
        
        fireTypesChanged(Arrays.asList(types), null);
    }

    public void                 setTypes (String ... types) {
        boolean allSubscribed;
        ArrayList<String> removed = new ArrayList<String>();
        
        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            allSubscribed = subscribedTypeNames == null;

            if (subscribedTypeNames == null)
                subscribedTypeNames = new HashSet <String> ();
            else {
                removed.addAll(subscribedTypeNames);
                subscribedTypeNames.clear();
            }

            subscribedTypeNames.addAll(Arrays.asList(types));
        }

        if (allSubscribed)
            fireAllTypesRemoved();

        fireTypesChanged(Arrays.asList(types), removed);
    }

    public void                 removeTypes (String ... types) {
        boolean allSubscribed = false;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            allSubscribed = subscribedTypeNames == null;
            if (allSubscribed)
                subscribedTypeNames = new HashSet <String> ();
            else
                for (String s : types)
                    subscribedTypeNames.remove (s);
        }

        if (allSubscribed)
            fireAllTypesRemoved();

        fireTypesChanged(null, Arrays.asList(types));
    }

    public void                 subscribeToAllTypes () {
        synchronized (mx) {
            if (subscribedTypeNames == null)
                return;

            assert delayedListenerToCall == null : dlnrDiag ();

            subscribedTypeNames = null;
        }

        fireAllTypesSubscribed();
    }

    public void                 reset (long time) {
        Runnable lnr;

        synchronized (mx) {
            if (time == TimeConstants.USE_CURRENT_TIME)
                time = TimeKeeper.currentTime;
            
            assert delayedListenerToCall == null : dlnrDiag ();

            if (mx.isClosed ())
                throw new IllegalStateException (this + " is closed.");

            newSubscriptionTime = TimeConstants.USE_CURSOR_TIME;
            
            mx.reset (time);

            for (StreamSource ssi : subscribedStreams)
                ssi.reset(time);

            resetTime = time;

            // update real-time state
            inRealtime = mx.isRealTime();

            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();
    }

    @Override
    public boolean                  isRealTime() {
        return inRealtime;
    }

    @Override
    public boolean                  realTimeAvailable() {
        return realTimeNotifications;
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
