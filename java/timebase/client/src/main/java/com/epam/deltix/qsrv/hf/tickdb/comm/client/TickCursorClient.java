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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.blocks.InstrumentSet;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.ProtocolViolationException;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.TypeSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SymbolAndTypeSubscriptionControllerClient;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.collections.IndexedArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.concurrent.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DisposableResourceTracker;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.*;
import java.net.SocketException;
import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

/**
 *
 */
class TickCursorClient implements
        TickCursor,
        SubscriptionManager,
        IntermittentlyAvailableCursor,
        SymbolAndTypeSubscriptionControllerClient
{
    static final boolean    DEBUG_COMM = false;
    private static final boolean    DEBUG_COMM_EVERY_MSG = false;
    private static final boolean    DEBUG_SUBSCRIPTION = false;
    private static final boolean    DEBUG_MSG_DISCARD = false;

    static enum ReceiverState {
        NORMAL,
        FAST_FORWARD,
        ACCUMULATING
    }

    private static enum NextMessageState {
        NOT_QUERIED,
        READY,
        END_OF_STREAM,
    }

    private static enum FilterOp {
        ALLOW_LISTED_ONLY,
        PROHIBIT_LISTED
    }
    
    /**
     *  Guards the interaction between the control and receiver virtual threads.
     */
    private final Object                                    interlock = new Object ();
    /**
     *  Used for mutually synchronizing the access to the output stream
     *  by the keep-alive timer task and all control methods.
     */
    private final Object                                    sendlock = new Object ();
    //
    //  Immutable final variables set in constructor
    //
    private final DXRemoteDB                                conn;
    private final SelectionOptions                          options;
    private final boolean                                   raw;
    private final boolean                                   allowOutOfOrder;
    private final VSChannel                                 ds;
    private final DataOutputStream                          out;
    //
    //  Objects cleared in close () but otherwise immutable. Guarded by interlock.
    //
    private DisposableResourceTracker                       tracker;
    //
    //  Objects implicitly guarded by the virtual receiving thread
    //
    private final IndexedArrayList <String>                 streamKeys =
        new IndexedArrayList <> ();

    private final ObjectArrayList <TickStream>              streamInstanceCache =
        new ObjectArrayList <> ();

    private int                                             currentStreamIdx = -1;
    private String                                          currentStreamKey = null;
    private int                                             currentEntityIdx = -1;
    private int                                             currentTypeIdx = -1;
    private RecordClassDescriptor                           currentType = null;
    private final TypeSet                                   types = new TypeSet(new TypeSet.TypeSender() {
        @Override
        public DataOutputStream begin() throws IOException {
            return null;
        }

        @Override
        public void end() throws IOException {

        }

        @Override
        public int version() {
            int v = conn.getServerProtocolVersion();
            if (v >= 132)
                return 2;
            return (v <= 99 || v >= 106) ? 0 : 1;
        }
    });
    private final ObjectArrayList <BoundDecoder>            decoders;
    private final RawMessage                                rawMessage;
    private final ObjectArrayList <ConstantIdentityKey>   entities =
        new ObjectArrayList <> ();

    private InstrumentMessage                       curMsg = null;
    private volatile boolean                        isAtEnd = false;

    private NextMessageState        nextStreamMsgState = NextMessageState.NOT_QUERIED;
    private byte []                 streamMsgBuffer = new byte [256];
    private final MemoryDataInput   streamMsgInput = new MemoryDataInput (streamMsgBuffer);
    private long                    streamMsgNanoTime;
    private final MemoryDataInput   accMsgInput = new MemoryDataInput ();

    // live support

    private boolean                 realtimeAvailable = false;
    private boolean                 isRealTime = false;

    //
    //  Objects responsible for the communication between virtual control and
    //  receiver threads, all guarded by interlock, except for currentTime,
    //  which is volatile.
    //

    private volatile long                           currentTime = Long.MIN_VALUE;

    private long                                    requestSerial = 0;
    private long                                    lastAckSerial = 0;
    private ReceiverState                           state = ReceiverState.NORMAL;
    private MessageAccumulator                      accumulator = null;
    private Set <String>                            filterStreamKeys = null;
    private FilterOp                                streamKeyFilterOp;
    private InstrumentSet                           filterEntities = null;
    private FilterOp                                entityFilterOp;
    private Set <String>                            filterTypeNames = null;
    private FilterOp                                typeFilterOp;

    private final LinkedList<SubscriptionAction>    actions = new LinkedList<>();

    // state for SubscriptionManager
    private final InstrumentToObjectMap<Long>   subscribedEntities;
    private boolean                             allEntitiesSubscribed = false;
    private final Set <String>                  subscribedTypes;
    private boolean                             allTypesSubscribed = false;
    private final HashSet <TickStream>          subscribedStreams;
    
    //
    //  Objects guarded by the virtual control thread.
    //
    private long                    timeForNewSubscriptions =
        TimeConstants.USE_CURSOR_TIME;
    //
    //  Non-blocking access
    //
    private volatile Runnable       callerListener = null;
    private final Object            asyncLock = new Object ();
    private boolean                 listenerIsArmed = false;
    private final Runnable          lnrAdapter =
        new Runnable () {
            public void run () {
                final Runnable      lnr = callerListener;

                if (lnr == null)
                    return;
                
                synchronized (asyncLock) {
                    if (!listenerIsArmed)
                        return;
                }
            
                lnr.run ();
            }
        };

    class SubscribeAllAction extends SubscriptionAction {

        SubscribeAllAction(long serial, ReceiverState state) {
            super(serial, state);
        }

        @Override
        public void     apply() {
            filterEntities = null;
        }
    }

    class SubscribeTypesAction extends SubscriptionAction {

        SubscribeTypesAction(long serial, ReceiverState state) {
            super(serial, state);
        }

        @Override
        public void     apply() {
        }
    }

    class AddStreamsAction extends SubscriptionAction {

        private Collection<TickStream>    elements;

        AddStreamsAction(long serial, Collection<TickStream> streams, ReceiverState state) {
            super(serial, state);
            this.elements = streams;
        }

        @Override
        public void     apply() {

            if (filterStreamKeys != null) {
                for (TickStream s : elements) {

                    if (!subscribedStreams.contains(s))
                        continue;

                    if (streamKeyFilterOp == FilterOp.ALLOW_LISTED_ONLY)
                        filterStreamKeys.add (s.getKey ());
                    else
                        filterStreamKeys.remove (s.getKey ());
                }

                //  Empty list of prohibitions is meaningless
                if (streamKeyFilterOp == FilterOp.PROHIBIT_LISTED && filterStreamKeys.isEmpty ())
                    filterStreamKeys = null;
            }
        }
    }

    class AddEntitiesAction extends SubscriptionAction {

        private Collection<IdentityKey>    elements;

        AddEntitiesAction(long serial, IdentityKey id, ReceiverState state) {
            super(serial, state);
            this.elements = new ArrayList<>(1);
            this.elements.add(id);
        }
        
        AddEntitiesAction(long serial, Collection<IdentityKey> entities, ReceiverState state) {
            super(serial, state);
            this.elements = entities;
        }

        @Override
        public void     apply() {

            if (!allEntitiesSubscribed && filterEntities != null) {
                for (IdentityKey id : elements) {
                    // check that we still have it subscribed
                    if (subscribedEntities.containsKey(id)) {
                        if (entityFilterOp == FilterOp.PROHIBIT_LISTED)
                            filterEntities.remove (id);
                        else
                            filterEntities.add(id);
                    }
                }

                if (entityFilterOp == FilterOp.PROHIBIT_LISTED && filterEntities.isEmpty ())
                    filterEntities = null;
            }
        }
    }

    public TickCursorClient(
            DXRemoteDB db,
            VSChannel channel,
            SelectionOptions options,
            long time,
            boolean allEntitiesSubscribed,
            boolean allTypesSubscribed,
            InstrumentToObjectMap<Long> subscribedEntities,
            HashSet<String> subscribedTypes,
            HashSet<TickStream> subscribedStreams
    )
    {
        this.options = options;

        this.raw = options.raw;
        this.allowOutOfOrder = options.allowLateOutOfOrder;
        this.realtimeAvailable = options.realTimeNotification;

        this.currentTime = time;

        if (raw) {
            this.rawMessage = new RawMessage();
            this.decoders = null;
            this.curMsg = rawMessage;
        } else {
            this.rawMessage = null;
            this.decoders = new ObjectArrayList<>();
        }
        this.conn = db;

        this.allEntitiesSubscribed = allEntitiesSubscribed;
        this.subscribedEntities = subscribedEntities;
        this.allTypesSubscribed = allTypesSubscribed;
        this.subscribedTypes = subscribedTypes;
        this.subscribedStreams = subscribedStreams;

        this.ds = channel;
        this.out = channel.getDataOutputStream ();
        this.tracker = new DisposableResourceTracker(this);
    }

    public void            setTimeForNewSubscriptions (long time) {
        timeForNewSubscriptions = time;
    }

    private long            getTimeForNewSubscriptions () {
        if (timeForNewSubscriptions == TimeConstants.USE_CURSOR_TIME)
            return (currentTime);
        else
            return (timeForNewSubscriptions);
    }
    
    public boolean         isClosed () {
        synchronized (interlock) {
            return isChannelClosed(ds);
        }
    }

    static boolean isChannelClosed(VSChannel ds) {
        return ds != null && (ds.getState() == VSChannelState.Closed ||
                ds.getState() == VSChannelState.Removed);
    }

    public void            close () {
        if (ds.getState() == VSChannelState.Connected) {
            try {
                synchronized (sendlock) {
                    out.writeShort (CURREQ_DISCONNECT);
                    out.flush ();
                }
            } catch (IOException iox) {
                TickDBClient.LOGGER.warn ("Error disconnecting from server (ignore) %s").with(iox);
            }
        }

        synchronized (interlock) {
            Util.close(ds);
        }

        Util.close(tracker);
        tracker = null;
    }

//    private void                            onDisconnected() {
//        conn.onDisconnected();
//        close ();
//    }

    //
    //  StreamSubscriptionController implementation
    //
    public void            addStream (TickStream ... tickStreams) {
        assertNotClosed ();

        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            ArrayList<TickStream> subscribed = new ArrayList<>();
            for (TickStream s : tickStreams) {
                if (!(s instanceof TickStreamClient))
                    throw new IllegalArgumentException("Stream class " + s.getClass() + " is not supported.");

                if (!subscribedStreams.contains(s)) {
                    subscribedStreams.add(s);
                    subscribed.add(s);
                }
            }

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            applyAction(new AddStreamsAction(serial, subscribed, newState));

            onSubscriptionAdded();
        }

        try {
            synchronized (sendlock) {

                if (DEBUG_SUBSCRIPTION) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(describe()).append(" addEntities #").append(serial).append(" at ").append(getTimeForNewSubscriptions()).append(" (");
                    for (TickStream stream : tickStreams) sb.append(" ").append(stream.getKey());
                    sb.append(")\n");
                    TickDBClient.LOGGER.info(sb.toString());
                }

                out.writeShort (CURREQ_ADD_STREAMS);
                out.writeLong (serial);
                out.writeLong (getTimeForNewSubscriptions ());
                writeStreamKeys(out, tickStreams);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void            removeStream (TickStream ... tickStreams) {
        assertNotClosed ();

        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            if (filterStreamKeys == null) {
                filterStreamKeys = new HashSet <> ();
                streamKeyFilterOp = FilterOp.PROHIBIT_LISTED;
            }

            for (TickStream s : tickStreams) {
                subscribedStreams.remove(s);

                if (streamKeyFilterOp == FilterOp.ALLOW_LISTED_ONLY)
                    filterStreamKeys.remove (s.getKey ());
                else
                    filterStreamKeys.add (s.getKey ());
            }
        }

        try {
            synchronized (sendlock) {
                out.writeShort (CURREQ_REMOVE_STREAMS);
                out.writeLong (serial);
                writeStreamKeys(out, tickStreams);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public static void                  writeStreamKeys (
        DataOutputStream                    out,
        TickStream []                       streams
    )
        throws IOException
    {
        out.writeBoolean(streams == null);

        if (streams != null) {
            out.writeShort(streams.length);

            for (TickStream stream : streams) {
                if (!(stream instanceof TickStreamClient))
                    throw new IllegalArgumentException("Stream class " + stream.getClass() + " is not supported.");

                out.writeUTF (stream.getKey ());
                ((TickStreamClient)stream).writeLock(out);
            }
        }
    }

    public void            removeAllStreams () {
        assertNotClosed ();

        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            subscribedStreams.clear();

            if (filterStreamKeys == null)
                filterStreamKeys = new HashSet <> ();
            else
                filterStreamKeys.clear ();

            streamKeyFilterOp = FilterOp.ALLOW_LISTED_ONLY;
        }

        try {
            synchronized (sendlock) {
                out.writeShort (CURREQ_REMOVE_ALL_STREAMS);
                out.writeLong (serial);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

//    @Override
//    public void             reset(long time, IdentityKey ... instruments) {
//
//        if (instruments.length == 0) {
//            reset(time);
//            return;
//        }
//
//        long    serial;
//
//        synchronized (interlock) {
//            serial = ++requestSerial;
//
//            state = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
//
//            nextStreamMsgState = NextMessageState.NOT_QUERIED;// do not consume
//            isAtEnd = false;
//            isRealTime = false;
//         }
//
//        try {
//            synchronized (sendlock) {
//                out.writeShort (CURREQ_RESET_INSTRUMENTS);
//                out.writeLong (serial);
//                out.writeLong (time);
//                writeInstrumentIdentities (instruments, 0, instruments.length, out);
//                out.flush ();
//            }
//        } catch (IOException iox) {
//            onX (iox);
//        }
//    }

    @Override
    public void             reset (long time) {
        assertNotClosed ();

        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            state = ReceiverState.FAST_FORWARD;
            nextStreamMsgState = NextMessageState.NOT_QUERIED;// do not consume
            accumulator = null;
            currentTime = time;
            timeForNewSubscriptions = TimeConstants.USE_CURSOR_TIME;
            isAtEnd = false;
            isRealTime = false;

            actions.clear();
        }

        try {
            synchronized (sendlock) {
                if (DEBUG_SUBSCRIPTION) {
                    TickDBClient.LOGGER.info(describe() + " reset #" + serial + " at " + time);
                }
                out.writeShort (CURREQ_RESET_TIME);
                out.writeLong (serial);
                out.writeLong (time);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }    
    //
    //  EntitySubscriptionController implementation 
    //
    private void        clearAllEntitiesInternal () {
        if (filterEntities == null)
            filterEntities = new InstrumentSet ();
        else
            filterEntities.clear ();

        entityFilterOp = FilterOp.ALLOW_LISTED_ONLY;
    }

    public void        clearAllEntities () {
        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            clearAllEntitiesInternal ();

            allEntitiesSubscribed = false;
            subscribedEntities.clear();

            state = ReceiverState.FAST_FORWARD;
            
            actions.clear(); // we don't need previous actions
        }

        try {
            synchronized (sendlock) {
                out.writeShort(CURREQ_CLEAR_ENTITIES);
                out.writeLong(serial);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void        subscribeToAllEntities () {
        long    serial;

        synchronized (interlock) {
            if (allEntitiesSubscribed)
                return;

            serial = ++requestSerial;

            allEntitiesSubscribed = true;
            subscribedEntities.clear();

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            applyAction(new SubscribeAllAction(serial, newState));

            isRealTime = false;
            onSubscriptionAdded();
        }
            
        try {
            synchronized (sendlock) {
                if (DEBUG_SUBSCRIPTION) {
                    TickDBClient.LOGGER.info(describe() + " allEntities #" + serial + " at " + getTimeForNewSubscriptions());
                }

                out.writeShort(CURREQ_ALL_ENTITIES);
                out.writeLong (serial);
                out.writeLong(getTimeForNewSubscriptions());
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void        removeEntity (IdentityKey id) {
        
        if (allEntitiesSubscribed) {
            clearAllEntities ();
            return;
        }

        long    serial;

        synchronized (interlock) {
            serial = ++requestSerial;
            
            subscribedEntities.remove(id);

            if (filterEntities == null) {
                filterEntities = new InstrumentSet ();
                entityFilterOp = FilterOp.PROHIBIT_LISTED;
            }

            if (entityFilterOp == FilterOp.PROHIBIT_LISTED)
                filterEntities.add (id);
            else
                filterEntities.remove (id);
        }

        try {
            synchronized (sendlock) {

                if (DEBUG_SUBSCRIPTION) {
                    String sb = (describe() + " removeEntity #" + serial + " at " + getTimeForNewSubscriptions() + " (") + id + ")\n";
                    TickDBClient.LOGGER.info(sb);
                }

                out.writeShort (CURREQ_REMOVE_ENTITIES);
                out.writeLong (serial);
                out.writeInt (1);
                writeIdentityKey (id, out);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void        removeEntities (
        IdentityKey []           ids,
        int                             offset,
        int                             length
    )
    {
        if (length == 0)
            return;

        long serial;

        synchronized (interlock) {

            if (allEntitiesSubscribed) {
                clearAllEntities ();
                return;
            }

            serial = ++requestSerial;

            if (filterEntities == null) {
                filterEntities = new InstrumentSet ();
                entityFilterOp = FilterOp.PROHIBIT_LISTED;
            }

            for (int ii = 0; ii < length; ii++) {
                IdentityKey   id = ids [offset + ii];

                if (entityFilterOp == FilterOp.PROHIBIT_LISTED)
                    filterEntities.add (id);
                else
                    filterEntities.remove (id);

                subscribedEntities.remove (id);
            }
        }

        try {
            synchronized (sendlock) {

                if (DEBUG_SUBSCRIPTION) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(describe()).append(" removeEntities #").append(serial).append(" at ").append(getTimeForNewSubscriptions()).append(" (");
                    for (IdentityKey id : ids) sb.append(" ").append(id);
                    sb.append(")\n");
                    TickDBClient.LOGGER.info(sb.toString());
                }

                out.writeShort (CURREQ_REMOVE_ENTITIES);
                out.writeLong (serial);
                writeInstrumentIdentities (ids, offset, length, out);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

//    private void        onSubscriptionAdded() {
//        if (nextStreamMsgState == NextMessageState.END_OF_STREAM && state == ReceiverState.ACCUMULATING)
//            nextStreamMsgState = NextMessageState.NOT_QUERIED;  // do not consume
//
//        // if subscription time goes back - we should consume input stream
//        if (state == ReceiverState.ACCUMULATING && getTimeForNewSubscriptions () < currentTime)
//            nextStreamMsgState = NextMessageState.NOT_QUERIED;
//
//        isAtEnd = false;
//    }

    private void        onSubscriptionAdded() {
        if (nextStreamMsgState == NextMessageState.END_OF_STREAM)
            nextStreamMsgState = NextMessageState.NOT_QUERIED;  // do not consume

        // if subscription time goes back - we should consume input stream
        if (!allowOutOfOrder && getTimeForNewSubscriptions () < currentTime)
            nextStreamMsgState = NextMessageState.NOT_QUERIED;

        isAtEnd = false;
    }

    public void        addEntity (IdentityKey id) {
        long serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            if (allEntitiesSubscribed) {
                clearAllEntitiesInternal();
                subscribedEntities.clear ();
            }

            // when all entities was subscribed, adding new entities do not invoke re-subscription,
            // that's why previous serial should be used for applying actions

            long acceptedSerial = allEntitiesSubscribed ? serial - 1 : serial; // previous serial

            if (!subscribedEntities.containsKey(id))
                subscribedEntities.put(id, acceptedSerial);

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            if (allEntitiesSubscribed) {
                allEntitiesSubscribed = false;
                new AddEntitiesAction(acceptedSerial, id, newState).apply();
            } else {
                applyAction(new AddEntitiesAction(serial, id, newState));
            }

            allEntitiesSubscribed = false;

            onSubscriptionAdded(); //isAtEnd = false;
        }

        try {
            synchronized (sendlock) {
                if (DEBUG_SUBSCRIPTION) {
                    String sb = (describe() + " addEntity #" + serial + " at " + getTimeForNewSubscriptions() + " (") + id + ")\n";
                    TickDBClient.LOGGER.info(sb);
                }

                out.writeShort (CURREQ_ADD_ENTITIES);
                out.writeLong (serial);
                out.writeLong (getTimeForNewSubscriptions ());
                out.writeInt (1);
                writeIdentityKey (id, out);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    private void applyAction(SubscriptionAction action) {

        if (state != ReceiverState.FAST_FORWARD)
            state = action.newState;

        if (action.serial == lastAckSerial - 1)
            action.apply();
        else
            actions.add(action);
    }

    public void        addEntities (
        IdentityKey []           ids,
        int                             offset,
        int                             length
    )
    {
        if (length == 0)
            return;
        
        long serial;
        
        synchronized (interlock) {
            serial = ++requestSerial;

            // when all entities was subscribed, adding new entities do not invoke re-subscription,
            // that's why previous serial should be used for applying actions

            long acceptedSerial = allEntitiesSubscribed ? serial - 1 : serial; // previous serial

            if (allEntitiesSubscribed) {
                subscribedEntities.clear ();
                clearAllEntitiesInternal();
            }

            ArrayList<IdentityKey> entities = new ArrayList<>();
            for (int ii = 0; ii < length; ii++) {
                IdentityKey id = ids[offset + ii];
                if (!subscribedEntities.containsKey(id))
                    subscribedEntities.put(id, acceptedSerial);

                entities.add(id);
            }

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            if (allEntitiesSubscribed) {
                allEntitiesSubscribed = false;
                new AddEntitiesAction(acceptedSerial, entities, newState).apply();
            } else {
                applyAction(new AddEntitiesAction(serial, entities, newState));
            }

            onSubscriptionAdded(); //isAtEnd = false;
        }

        try {
            synchronized (sendlock) {

                if (DEBUG_SUBSCRIPTION) {
                    StringBuilder sb = new StringBuilder();
                    String  time = (timeForNewSubscriptions == TimeConstants.USE_CURSOR_TIME) ? "CURRENT:" + currentTime : timeForNewSubscriptions + "";

                    sb.append(describe()).append(" addEntities #").append(serial).append(" at ").append(time).append(" (");
                    for (IdentityKey id : ids) sb.append(" ").append(id);
                    sb.append(")\n");
                    TickDBClient.LOGGER.info(sb.toString());
                }

                out.writeShort (CURREQ_ADD_ENTITIES);
                out.writeLong (serial);
                out.writeLong (getTimeForNewSubscriptions ());
                writeInstrumentIdentities (ids, offset, length, out);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

     //
    //  SubscriptionManager implementation
    //

    @Override
    public IdentityKey[]     getSubscribedEntities() {
        return subscribedEntities.size() > 0 ?
                subscribedEntities.keySet().toArray(new IdentityKey[subscribedEntities.size()]) :
                new IdentityKey[0];
    }

    @Override
    public boolean                  isAllEntitiesSubscribed() {
        return allEntitiesSubscribed;
    }

    @Override
    public String[]                 getSubscribedTypes() {
        return subscribedTypes.size() > 0 ?
                subscribedTypes.toArray(new String[subscribedTypes.size()]) :
                new String[0];
    }

    @Override
    public boolean                  isAllTypesSubscribed() {
        return allTypesSubscribed;
    }

    @Override
    public boolean                  hasSubscribedTypes() {
        return allEntitiesSubscribed || !subscribedTypes.isEmpty();
    }

    //
    //  TypeSubscriptionController implementation
    //
    
    private void        clearAllTypesInternal() {
        if (filterTypeNames == null)
            filterTypeNames = new HashSet <> ();
        else
            filterTypeNames.clear ();

        subscribedTypes.clear ();

        typeFilterOp = FilterOp.ALLOW_LISTED_ONLY;
    }

    @Override
    public void                     subscribeToAllTypes () {

        long serial;

        synchronized (interlock) {
            if (allTypesSubscribed)
                return;

            serial = ++requestSerial;

//            if (!allowOutOfOrder)
//                state = ReceiverState.ACCUMULATING;

            allTypesSubscribed = true;
            filterTypeNames = null;
            subscribedTypes.clear();

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            applyAction(new SubscribeTypesAction(serial, newState));
            
            onSubscriptionAdded();
        }

        try {
            if (DEBUG_SUBSCRIPTION) {
                TickDBClient.LOGGER.info(describe() + " allTypes #" + serial);
            }

            out.writeShort (CURREQ_ALL_TYPES);
            out.writeLong (serial);
            out.flush ();
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void        setTypes (String ... names) {

       long serial;

       synchronized (interlock) {
           if (allTypesSubscribed)
               clearAllTypesInternal ();

           serial = ++requestSerial;

           allTypesSubscribed = false;
           subscribedTypes.clear();
           subscribedTypes.addAll(Arrays.asList(names));

           if (filterTypeNames != null) {
               for (String name : names) {
                   if (typeFilterOp == FilterOp.ALLOW_LISTED_ONLY)
                       filterTypeNames.add (name);
                   else
                       filterTypeNames.remove (name);
               }

               //  Empty list of prohibitions is meaningless
               if (typeFilterOp == FilterOp.PROHIBIT_LISTED && filterTypeNames.isEmpty ())
                   filterTypeNames = null;
           }

           ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
           applyAction(new SubscribeTypesAction(serial, newState));

           onSubscriptionAdded();
       }

       try {
           synchronized (sendlock) {
               out.writeShort (CURREQ_SET_TYPES);
               out.writeLong (serial);
               writeNonNullableStrings (out, names);
               out.flush ();
           }
       } catch (IOException iox) {
           onX (iox);
       }
    }


    public void        addTypes (String ... names) {
        if (names == null || names.length == 0)
            return;
        
        long serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            subscribeTypes(names);

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            applyAction(new SubscribeTypesAction(serial, newState));

            onSubscriptionAdded();
        }

        try {
            synchronized (sendlock) {
                out.writeShort (CURREQ_ADD_TYPES);
                out.writeLong (serial);
                writeNonNullableStrings (out, names);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    @Override
    public void         add(IdentityKey[] ids, String[] types) {

        long serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            subscribeTypes(types);

            boolean allSubscribed = allEntitiesSubscribed;

            List<IdentityKey> entities = subscribeEntities(serial, ids, 0, ids.length);

            ReceiverState newState = allowOutOfOrder ? ReceiverState.NORMAL : ReceiverState.ACCUMULATING;
            if (allSubscribed)
                new AddEntitiesAction(serial, entities, newState).apply();
            else
                applyAction(new AddEntitiesAction(serial, entities, newState));

            onSubscriptionAdded();
        }

        try {
            synchronized (sendlock) {
                out.writeShort (CURREQ_ADD_ENTITIES_TYPES);
                out.writeLong (serial);
                out.writeLong (getTimeForNewSubscriptions ());
                writeInstrumentIdentities (ids, out);
                writeNonNullableStrings (out, types);
                out.flush ();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    private List<IdentityKey>        subscribeEntities (
            long serial, IdentityKey[] ids, int offset, int length)
    {
        if (allEntitiesSubscribed) {
            subscribedEntities.clear ();
            clearAllEntitiesInternal();
        }
        allEntitiesSubscribed = false;

        ArrayList<IdentityKey> entities = new ArrayList<>();
        for (int ii = 0; ii < length; ii++) {
            IdentityKey id = ids[offset + ii];
            if (!subscribedEntities.containsKey(id))
                subscribedEntities.put(id, serial);

            entities.add(id);
        }

        return entities;
    }
    
    private List<String>        subscribeTypes(String[] types) {
        assert Thread.holdsLock(interlock);

        ArrayList<String> subscribed = new ArrayList<>();

        if (types != null && types.length > 0) {
            allTypesSubscribed = false;

            for (String type : types) {
                if (subscribedTypes.add(type))
                    subscribed.add(type);
            }

            subscribedTypes.addAll(Arrays.asList(types));

            if (filterTypeNames != null) {
                for (String name : types) {
                    if (typeFilterOp == FilterOp.ALLOW_LISTED_ONLY)
                        filterTypeNames.add (name);
                    else
                        filterTypeNames.remove (name);
                }

                //  Empty list of prohibitions is meaningless
                if (typeFilterOp == FilterOp.PROHIBIT_LISTED && filterTypeNames.isEmpty ())
                    filterTypeNames = null;
            }
        }

        return subscribed;
    }

    private void        unsubscribeTypes(String[] types) {
        assert Thread.holdsLock(interlock);

        allTypesSubscribed = false;
        subscribedTypes.removeAll(Arrays.asList(types));

        if (filterTypeNames != null) {
            for (String name : types) {
                if (typeFilterOp == FilterOp.ALLOW_LISTED_ONLY)
                    filterTypeNames.add (name);
                else
                    filterTypeNames.remove (name);
            }

            //  Empty list of prohibitions is meaningless
            if (typeFilterOp == FilterOp.PROHIBIT_LISTED && filterTypeNames.isEmpty ())
                filterTypeNames = null;
        }
    }

    @Override
    public void             remove(IdentityKey[] ids, String[] types) {

        long serial;

        synchronized (interlock) {
            serial = ++requestSerial;

            // entities
            if (ids != null && ids.length > 0) {

                if (allEntitiesSubscribed) {
                    clearAllEntitiesInternal ();

                    subscribedEntities.clear();
                    allEntitiesSubscribed = false;
                    state = ReceiverState.FAST_FORWARD;

                    actions.clear(); // we don't need previous actions
                } else {
                    if (filterEntities == null) {
                        filterEntities = new InstrumentSet ();
                        entityFilterOp = FilterOp.PROHIBIT_LISTED;
                    }

                    for (IdentityKey id : ids) {
                        if (entityFilterOp == FilterOp.PROHIBIT_LISTED)
                            filterEntities.add (id);
                        else
                            filterEntities.remove (id);

                        subscribedEntities.remove (id);
                    }
                }
            }

            if (types != null && types.length > 0) {
                if (allTypesSubscribed)
                    clearAllTypesInternal ();

                unsubscribeTypes(types);

                allTypesSubscribed = false;
            }
        }

        try {
            synchronized (sendlock) {
                out.writeShort(CURREQ_REMOVE_ENTITIES_TYPES);
                out.writeLong(serial);
                writeInstrumentIdentities(ids, out);
                writeNonNullableStrings(out, types);
                out.flush();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }

    public void                     removeTypes (String ... names) {
        if (names == null || names.length == 0)
            return;

        if (allTypesSubscribed) {
            setTypes (); // clear all types
            return;
        }

        long serial;
        synchronized (interlock) {
            serial = ++requestSerial;

            if (filterTypeNames == null) {
                filterTypeNames = new HashSet <> ();
                typeFilterOp = FilterOp.PROHIBIT_LISTED;
            }

            subscribedTypes.removeAll(Arrays.asList(names));

            for (String name : names) {
                if (typeFilterOp == FilterOp.PROHIBIT_LISTED)
                    filterTypeNames.add (name);
                else
                    filterTypeNames.remove (name);
            }
        }

        try {
            synchronized (sendlock) {
                out.writeShort(CURREQ_REMOVE_TYPES);
                out.writeLong(serial);
                writeNonNullableStrings(out, names);
                out.flush();
            }
        } catch (IOException iox) {
            onX (iox);
        }
    }
    
    //
    //  Receiver implementation
    //

    @Override
    public NextResult                   nextIfAvailable () {
        return next(false);
    }

    @Override
    public boolean                      next () {
        return next(true) == NextResult.OK;
    }

    private NextResult                  next (boolean throwable) {
        
        if (currentTime == Long.MAX_VALUE && !options.reversed)
            throw new IllegalStateException("Cursor was never reset.");

        if (isAtEnd)
            return NextResult.END_OF_CURSOR;

        NextResult result;

        for (;;) {
            //
            //  Multiplex the accumulator queue with the incoming stream (if any)
            //
            if (nextStreamMsgState == NextMessageState.NOT_QUERIED) {
                result = queryStream();

                if (result == NextResult.UNAVAILABLE) {
                    if (throwable)
                        throw UnavailableResourceException.INSTANCE;

                    return result;
                }
            }

            UnprocessedMessage  accMsg;
            boolean             consumeAccumulatedMessage;

            if (accumulator != null) {
                accMsg = accumulator.peek ();

                if (accMsg == null) {
                    consumeAccumulatedMessage = false;
                    accumulator = null;

                    if (lastAckSerial == requestSerial)
                        resetFilters ();
                }
                else if (nextStreamMsgState == NextMessageState.END_OF_STREAM)
                    consumeAccumulatedMessage = true;
                else // Give preference to acc, to avoid waiting on stream
                    consumeAccumulatedMessage = accMsg.nanos <= streamMsgNanoTime;
            }
            else {
                consumeAccumulatedMessage = false;
                accMsg = null;
            }

            if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                TickDBClient.LOGGER.info (describe() + ": consumeAccumulatedMessage=" + consumeAccumulatedMessage);
            }

            if (consumeAccumulatedMessage) {
                accumulator.poll ();
                accMsgInput.setBytes (accMsg.data);

                if (setupMessage (accMsgInput, accMsg.nanos, accMsg.serial))
                    return NextResult.OK;
            }
            else if (nextStreamMsgState == NextMessageState.END_OF_STREAM) {
                nextStreamMsgState = NextMessageState.NOT_QUERIED;
                isAtEnd = true;
                return NextResult.END_OF_CURSOR;
            }
            else {
                assert nextStreamMsgState == NextMessageState.READY;

                nextStreamMsgState = NextMessageState.NOT_QUERIED;
                
                if (setupMessage (streamMsgInput, streamMsgNanoTime, -1))
                    return NextResult.OK;
            }
        }
    }

    /**
     *  Keep processing responses until a message comes in or end of 
     *  stream is reached. If end of stream is reached,
     *  set nextStreamMsgState = NextMessageState.END_OF_STREAM.
     *  Otherwise: receive message bytes;
     *  set nextStreamMsgState = NextMessageState.READY; set up
     *  streamMsgInput (positioned right after the initial timestamp);
     *  set streamMsgTicks to the timestamp of the received message.
     */
    private NextResult                    queryStream () {
        try {
            final InputStream       in = ds.getInputStream();
            final DataInputStream   din = ds.getDataInputStream();

            for (;;) {
                if (callerListener != null) {
                    synchronized (asyncLock) {
                        listenerIsArmed = (in.available () == 0);

                        if (listenerIsArmed)
                            return NextResult.UNAVAILABLE;

                            //throw UnavailableResourceException.INSTANCE;
                    }
                }

                int code = MessageSizeCodec.read (in);

                if (code < 0)
                    throw new EOFException ();
                
                switch (code) {
                    case CURRESP_END_OF_CURSOR: 
                        if (processEOC ())
                            return NextResult.END_OF_CURSOR;

                        break;

                    case CURRESP_ACK_SERIAL:
                        processAckSerial ();
                        break;

                    case CURRESP_ERROR:
                        processError (ds.getDataInputStream());
                        break;

                    case CURRESP_LOAD_TYPE:
                        processLoadType ();
                        break;

                    case CURRESP_LOAD_ENTITY:
                        processLoadEntity ();
                        break;

                    case CURRESP_LOAD_STREAM:
                        processLoadStream ();
                        break;

                    default:
                        if (processMessage (din, code))
                            return NextResult.OK;

                        break;
                }
            }
        } catch (EOFException iox) {
            throw new CursorIsClosedException (iox);
        } catch (IOException iox) {
            if (isClosed ())
                throw new CursorIsClosedException (iox);

            onX (iox);
        }

        // unreachable
        return null;
    }

    private boolean                 processEOC () throws IOException {
        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_END_OF_CURSOR; state=" + state);
        }

        synchronized (interlock) {
            if (lastAckSerial != requestSerial)
                return (false);
        }

        nextStreamMsgState = NextMessageState.END_OF_STREAM;
        return (true);
    }

    private void                    processLoadType () throws IOException {
        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_TYPE; state=" + state);
        }

        types.readTypes (ds.getDataInputStream());
    }
   
    private void                    processLoadEntity () throws IOException {
        ConstantIdentityKey  id = readIdentityKey (ds.getDataInputStream());

        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_ENTITY (" + id + "); state=" + state);
        }

        entities.add (id);
    }

    private void                    processLoadStream () throws IOException {
        String  key = ds.getDataInputStream().readUTF ();

        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_STEAM (" + key + "); state=" + state);
        }

        streamKeys.add (key);
    }

    /**
     *  Removes all filters. THis method should be called as soon as BOTH
     *  the server catches up with the client's command state (acknowledges
     *  the correct serial) AND the accumulator is exhausted (until then, the
     *  accumulated messages must be filtered).
     */
    private void                    resetFilters () {
        filterStreamKeys = null;
        filterEntities = null;
        filterTypeNames = null;
    }

    private void                    processAckSerial () throws IOException {
        lastAckSerial = ds.getDataInputStream().readLong ();

        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (
                    describe() + ": RECEIVED CURRESP_ACK_SERIAL (" + lastAckSerial +
                "); expecting #" + requestSerial + "; state=" + state
            );
        }

        synchronized (interlock) {

            SubscriptionAction action = actions.peek();
            // cursor may not send acknowledge for the each subscription
            while (action != null && action.serial <= lastAckSerial) {
                actions.poll().apply();
                state = action.newState;

                action = actions.peek();
            }

            action = actions.peek();
            if (action != null && action.serial == lastAckSerial + 1)
                state = action.newState;
            
            if (lastAckSerial == requestSerial) {
                state = ReceiverState.NORMAL;

                if (accumulator == null)
                    resetFilters ();
            }
            else if (lastAckSerial > requestSerial)
                throw new ProtocolViolationException (
                    "Received serial " + lastAckSerial + " ahead of requested " +
                    requestSerial
                );
            else if (lastAckSerial < 0)
                throw new ProtocolViolationException ("Received serial " + lastAckSerial);
        }
    }

    private boolean                 processMessage (DataInputStream in, int size) throws IOException {
        if (USE_MAGIC) {
            int a = ds.getInputStream().read ();
            int b = ds.getInputStream().read ();

            if (a != 35 || b != 214)
                throw new IOException ("magic wrong: " + a + ", " + b);
        }


        long        sequence = -1;
        
        if (SEND_SEQUENCE) {
            sequence = in.readLong ();
        }

        synchronized (interlock) {
            if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                TickDBClient.LOGGER.info (describe() + ": RECEIVED MESSAGE (size=" + size + "; sequence=" + sequence + "); state=" + state);
            }

            if (state == ReceiverState.FAST_FORWARD) {
                int     s = in.skipBytes (size);

                if (s != size)
                    throw new IOException ("Failed to skip " + size + " bytes (skipped " + s + ")");

                if (DEBUG_COMM && DEBUG_MSG_DISCARD) {
                    TickDBClient.LOGGER.info (describe() + ":     state=FAST_FORWARD - message ignored (timestamp unknown).");
                }

                return (false); // keep reading
            }

            if (streamMsgBuffer.length < size)
                streamMsgBuffer = new byte [Util.doubleUntilAtLeast (streamMsgBuffer.length, size)];

            in.readFully (streamMsgBuffer, 0, size);
            streamMsgInput.setBytes (streamMsgBuffer, 0, size);
            streamMsgNanoTime = TimeCodec.readNanoTime(streamMsgInput);

            if (state == ReceiverState.ACCUMULATING) {
                int             offset = streamMsgInput.getPosition ();
                int             length = size - offset;
                //
                //  Apply filters before accumulating to avoid wasting
                //  memory on rejected messages.
                //
                if (!acceptMessage (streamMsgInput)) {
                    if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                        TickDBClient.LOGGER.info (
                                describe() + ":     nanos=" + streamMsgNanoTime +
                            "; state=ACCUMULATING - rejected by filters."
                        );
                    }

                    return (false);
                }

                if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                    TickDBClient.LOGGER.info (
                            describe() + ":     nanos=" + streamMsgNanoTime +
                        "; state=ACCUMULATING - accumulated."
                    );
                }

                if (accumulator == null) 
                    accumulator = new MessageAccumulator(500);

                accumulator.offer (
                    new UnprocessedMessage (
                        streamMsgNanoTime,
                        lastAckSerial,
                        streamMsgBuffer,
                        offset,
                        length
                    )
                );

                return (false); // keep reading
            }

            if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                TickDBClient.LOGGER.info (describe() + ":     nanos=" + streamMsgNanoTime + "; state=NORMAL - message returned.");
            }
        }

        nextStreamMsgState = NextMessageState.READY;
        return (true);
    }

    // extracts stream key index stored by given offset
    private String extractStreamKey(int offset) {
        final int originalOffset = streamMsgInput.getPosition ();
        try {
            streamMsgInput.seek (offset);
            String streamKey;
            int sidx = streamMsgInput.readUnsignedShort ();
            if (sidx >=0 && sidx < streamKeys.size())
                streamKey = streamKeys.get (sidx);
            else
                streamKey = "<Unknown>";
            return streamKey;
        } finally {
             streamMsgInput.seek (originalOffset);
        }
    }

    private boolean         isRealTimeMessage(RecordClassDescriptor type) {
        return RealTimeStartMessage.DESCRIPTOR_GUID.equals(type.getGuid());
    }

    private boolean         acceptMessage (MemoryDataInput mdi) {
        // we should check type of accepted message in live mode first
        if (realtimeAvailable) {
            int position = mdi.getPosition();
            mdi.skipBytes (6);
            RecordClassDescriptor type = types.getConcreteTypeByIndex(mdi.readUnsignedByte());
            if (isRealTimeMessage(type))
                return true;
            
            mdi.seek(position);
        }

        if (filterStreamKeys != null) {
            int                 sidx = mdi.readUnsignedShort ();

            if ((streamKeyFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                    filterStreamKeys.contains (streamKeys.get (sidx)))
            {
                if (DEBUG_MSG_DISCARD)
                    TickDBClient.LOGGER.info (
                        "TB DEBUG: Discarding message from " + streamKeys.get (sidx) + " due to filter. nanos=" + streamMsgNanoTime
                    );                
                return (false);
            }
        }
        else
            mdi.skipBytes (2);

        int eidx = mdi.readInt ();
        
        if (filterEntities != null) {
            if ((entityFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                    filterEntities.contains (entities.get (eidx)))
            {
                if (DEBUG_MSG_DISCARD)
                    TickDBClient.LOGGER.info (
                        "TB DEBUG: Discarding message " + entities.get (eidx) + " due to entities filter; nanos=" + streamMsgNanoTime
                    );
                return (false);
            }
        }

        Long value = subscribedEntities.get(entities.get(eidx));
        if (value != null && value > lastAckSerial) {
            if (DEBUG_MSG_DISCARD)
                TickDBClient.LOGGER.info (
                    "TB DEBUG: Discarding message " + entities.get (eidx) + " due to previous serial; nanos=" + streamMsgNanoTime
                );
            return false;
        }

        if (filterTypeNames != null) {
            int                 tidx = mdi.readUnsignedByte ();

            if ((typeFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                    filterTypeNames.contains (types.getConcreteTypeByIndex (tidx).getName ())) {
                if (DEBUG_MSG_DISCARD)
                    TickDBClient.LOGGER.info (
                        "TB DEBUG: Discarding message due to types filter; nanos=" + streamMsgNanoTime
                    );
                return (false);
            }
        }

        return (true);
    }

    private boolean        setupMessage (MemoryDataInput mdi, long nanos, long serial) {
        currentStreamIdx = mdi.readShort ();
        currentStreamKey = currentStreamIdx != -1 ? streamKeys.get (currentStreamIdx) : null;

        currentEntityIdx = mdi.readInt ();

        final ConstantIdentityKey     entity = entities.get (currentEntityIdx);
        currentTypeIdx = mdi.readUnsignedByte ();
        currentType = types.getConcreteTypeByIndex (currentTypeIdx);

        synchronized (interlock) {

            boolean rtStarted = realtimeAvailable && isRealTimeMessage(currentType);
            if (rtStarted)
                isRealTime = true;

//            if (rtStarted) {
//                isRealTime = (serial != -1 && lastAckSerial == serial) || (serial == -1 && requestSerial == lastAckSerial);
//
//                if (!isRealTime){
//                    // skip message if subscription has been changed
//                    if (DEBUG_MSG_DISCARD)
//                        TickDBClient.LOGGER.info ("TB DEBUG: Discarding real-time message " + entity + " due to not completed serial=" + serial);
//                    return false;
//                }
//            }
            
            if (!rtStarted) {
                // apply filter only for other message
                
                if (serial != -1) {
                    Long actual = subscribedEntities.get(entity);
                    if (actual != null && serial < actual) {
                        if (DEBUG_MSG_DISCARD)
                            TickDBClient.LOGGER.info (describe() + " DEBUG: Discarding " + entity + " due to not completed serial=" + serial);
                        return false;
                    }
                }

                if (filterStreamKeys != null &&
                    (streamKeyFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                        filterStreamKeys.contains (currentStreamKey))
                    return (false);

                if (filterEntities != null &&
                    (entityFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                        filterEntities.contains (entity)) {

                    if (DEBUG_MSG_DISCARD)
                        TickDBClient.LOGGER.info (describe() + " DEBUG: Discarding " + entity + " due to instrument filter");
                    return (false);
                }

                if (filterTypeNames != null &&
                    (typeFilterOp == FilterOp.ALLOW_LISTED_ONLY) !=
                        filterTypeNames.contains (currentType.getName ())) {

                    if (DEBUG_MSG_DISCARD)
                        TickDBClient.LOGGER.info (describe() + " DEBUG: Discarding " + currentType + " due to type filter ");
                    return (false);
                }
            }
        }
        
        if (raw) {
            rawMessage.type = currentType;
            rawMessage.setBytes (mdi.getBytes (), mdi.getCurrentOffset (), mdi.getAvail ());
        }
        else {
            BoundDecoder                decoder;

            if (currentTypeIdx >= decoders.size ()) {
                decoders.setSize (currentTypeIdx + 1);
                decoder = null;
            }
            else
                decoder = decoders.getObjectNoRangeCheck (currentTypeIdx);

            if (decoder == null) {
                decoder =
                    conn.getCodecFactory (options.channelQOS).createFixedBoundDecoder (
                        options.getTypeLoader(),
                        currentType
                    );

                decoders.set (currentTypeIdx, decoder);
            }

            curMsg = (InstrumentMessage) decoder.decode (mdi);
        }

        curMsg.setNanoTime(nanos);
        currentTime = curMsg.getTimeStampMs();
        curMsg.setSymbol(entity.symbol);

        if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG)
            TickDBClient.LOGGER.info (describe() + ": setupMessage=" + curMsg + "; isRealtime=" + isRealTime + "; serial=" + serial);

        return (true);
    }

    static void                 processError (DataInputStream in) {
        try {
            Throwable obj = (Throwable) TDBProtocol.readBinary(in);

            if (obj instanceof RuntimeException)
                throw (RuntimeException) obj;
            else if (obj instanceof Error)
                throw (Error) obj;
            else
                throw new RuntimeException (obj);
        } catch (IOException | ClassNotFoundException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    public String                      getCurrentStreamKey () {
        return (currentStreamKey);
    }

    public TickStream                  getCurrentStream () {
        if (currentStreamIdx < 0)
            return (null);
        
        while (streamInstanceCache.size () <= currentStreamIdx)
            streamInstanceCache.add (null);

        TickStream      s = streamInstanceCache.get (currentStreamIdx);

        if (s == null) {
            s = conn.getStream (currentStreamKey);
            streamInstanceCache.set (currentStreamIdx, s);
        }

        return (s);
    }

    public int                         getCurrentStreamIndex () {
        return (currentStreamIdx);
    }

    @Override
    public RecordClassDescriptor       getCurrentType () {
        return (currentType);
    }

    @Override
    public int                         getCurrentTypeIndex () {
        return (currentTypeIdx);
    }

    @Override
    public InstrumentMessage                getMessage () {
        return (curMsg);
    }

    @Override
    public boolean                          isAtEnd () {
        return (isAtEnd);        
    }

    @Override
    public int                              getCurrentEntityIndex () {
        return (currentEntityIdx);
    }

    public void                             setAvailabilityListener (
        final Runnable                          lnr
    )
    {
        callerListener = lnr;
        ds.setAvailabilityListener (lnr == null ? null : lnrAdapter);
    }

    /// RealTimeMessageSource

    @Override
    public boolean                          isRealTime() {
        return isRealTime;
    }

    @Override
    public boolean                          realTimeAvailable() {
        return options.realTimeNotification;
    }

    //
    //  Utilities
    //
    private void                        onX (IOException x) {
        if (x instanceof SocketException) {
            assertNotClosed();
        }

        if (x instanceof InterruptedIOException)
            throw new UncheckedInterruptedException(x);

        throw new com.epam.deltix.util.io.UncheckedIOException(x);
    }

    private void                        assertNotClosed () {
        if (isClosed ())
            throw new IllegalStateException (
                "Cursor is closed either by a client or upon a disconnection event."
            );
    }

    private String                      describe() {
        return "[" + this + "]";
    }
}