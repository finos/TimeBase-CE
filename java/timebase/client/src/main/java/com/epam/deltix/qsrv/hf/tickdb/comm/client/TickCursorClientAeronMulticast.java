package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.TypeSet;
import com.epam.deltix.qsrv.hf.tickdb.impl.IdleStrategyProvider;
import com.epam.deltix.qsrv.hf.tickdb.impl.InternalByteArrayInputStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.StreamMessageSource;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.collections.IndexedArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DisposableResourceTracker;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;
import io.aeron.Aeron;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteOrder;
import java.util.LinkedList;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

/**
 * @author Alexei Osipov
 */
public class TickCursorClientAeronMulticast implements MessageSource<InstrumentMessage>, RealTimeMessageSource<InstrumentMessage>, ControlledFragmentHandler, StreamMessageSource {
    private static final boolean    DEBUG_COMM = false;
    private static final boolean    DEBUG_COMM_EVERY_MSG = false;
    private static final boolean    DEBUG_SUBSCRIPTION = false;
    private static final boolean    DEBUG_MSG_DISCARD = false;

    private static final int PARTIAL_MSG_HEADER_CODE_SIZE = Byte.BYTES; // Size of header that contains TDBProtocol.CURRESP_MSG_MULTIPART_BODY
    private static final int PARTIAL_MSG_HEADER_SIZE_FIELD_SIZE = Integer.BYTES;

    private final CursorAeronClient cursorAeronClient;
    //private final AeronPublicationDSAdapter aeronPublisher;
    private final boolean lowLatency;
    private int nextExpectedPos = -1;

    private enum ReceiverState {
        NORMAL,
        FAST_FORWARD
    }

    private enum NextMessageState {
        NOT_QUERIED,
        READY,
        END_OF_STREAM,
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
    private final SelectionOptions options;
    private final boolean                                   raw;
    private final boolean                                   allowOutOfOrder;
    private final VSChannel ds;
    //
    //  Objects cleared in close () but otherwise immutable. Guarded by interlock.
    //
    private DisposableResourceTracker tracker;
    //
    //  Objects implicitly guarded by the virtual receiving thread
    //
    private final IndexedArrayList<String> streamKeys =
            new IndexedArrayList<>();

    private final ObjectArrayList<TickStream> streamInstanceCache =
            new ObjectArrayList<>();

    private int                                             currentStreamIdx = -1;
    private String                                          currentStreamKey = null;
    private int                                             currentEntityIdx = -1;
    private int                                             currentTypeIdx = -1;
    private RecordClassDescriptor currentType = null;
    private final TypeSet types = new TypeSet (null);
    private final ObjectArrayList <BoundDecoder>            decoders;
    private final RawMessage rawMessage;
    private final ObjectArrayList <ConstantIdentityKey>   entities =
            new ObjectArrayList<>();

    private InstrumentMessage                       curMsg = null;
    private volatile boolean                        isAtEnd = false;

    private NextMessageState nextStreamMsgState = NextMessageState.NOT_QUERIED;
    private byte []                 streamMsgBuffer = new byte [256];
    private final MemoryDataInput   streamMsgInput = new MemoryDataInput (streamMsgBuffer);

    private long                    streamMsgNanoTime;

    // live support

    private boolean                 realtimeAvailable = false;
    private boolean                 isRealTime = false;

    //
    //  Objects responsible for the communication between virtual control and
    //  receiver threads, all guarded by interlock, except for currentTime,
    //  which is volatile.
    //


    private long                                    requestSerial = 0;
    private long                                    lastAckSerial = 0;
    private ReceiverState state = ReceiverState.NORMAL;

    private final LinkedList<SubscriptionAction> actions = new LinkedList<>();

    // state for SubscriptionManager
    /*
    private final InstrumentToObjectMap<Long> subscribedEntities;
    private boolean                             allEntitiesSubscribed = false;
    private final Set <String>                  subscribedTypes;
    private boolean                    allTypesSubscribed = false;
    private final Set<TickStream> subscribedStreams;
    */

    //
    //  Objects guarded by the virtual control thread.
    //
    private long                    timeForNewSubscriptions =
            TimeConstants.USE_CURSOR_TIME;



    private final int TARGET_BUFFER_SIZE = (128-16) * 1024; // How many bytes we want to bulk load into buffer before starting processing
    private final int BIG_MESSAGE_SIZE = 16 * 1024; // How much space me may reserve for incomplete messages. This value should be bigger than 99.99% of messages.
    private final int INITIAL_BUFFER_SIZE = BitUtil.nextPowerOfTwo(TARGET_BUFFER_SIZE + BIG_MESSAGE_SIZE);
    private final ExpandableArrayBuffer bigInputArrayBuffer = new ExpandableArrayBuffer(INITIAL_BUFFER_SIZE); //
    private int bigBufferPos = 0; // Index of first unread byte in bigInputArrayBuffer
    private int bigBufferLimit = 0; // Index of first byte after readable data in bigInputArrayBuffer. if bigBufferPos==bigBufferLimit there is no data.

    private static final int NO_PARTIAL_MESSAGE = -1;
    private int incompletePartialMessageOffset = NO_PARTIAL_MESSAGE;
    private int incompletePartialMessageSize = 0;

    private final ExpandableArrayBuffer inputArrayBuffer = new ExpandableArrayBuffer();
    private final InternalByteArrayInputStream arrayInput = new InternalByteArrayInputStream(inputArrayBuffer.byteArray());
    private final DataInputStream dataInputWrapper = new DataInputStream(arrayInput);

    private volatile boolean closed = false;

    //
    //  Non-blocking access
    //
    private final DXAeronSubscriptionChecker subscriptionChecker;
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

    public TickCursorClientAeronMulticast(DXRemoteDB db, VSChannel channel, boolean raw, TickStream stream, DXAeronSubscriptionChecker subscriptionChecker, int aeronDataStreamId, Aeron aeron, String aeronChannel) {
        this.options = new SelectionOptions();
        this.options.raw = raw;


        this.raw = raw;
        this.allowOutOfOrder = options.allowLateOutOfOrder;
        this.realtimeAvailable = options.realTimeNotification;

        this.subscriptionChecker = subscriptionChecker;

        if (raw) {
            this.rawMessage = new RawMessage();
            this.decoders = null;
            this.curMsg = rawMessage;
        } else {
            this.rawMessage = null;
            this.decoders = new ObjectArrayList<>();
        }
        this.conn = db;

        //this.allEntitiesSubscribed = allEntitiesSubscribed;
        //this.subscribedEntities = subscribedEntities;
        //this.allTypesSubscribed = allTypesSubscribed;
        //this.subscribedTypes = subscribedTypes;
        //this.subscribedStreams = Collections.singleton(stream);

        this.cursorAeronClient = CursorAeronClient.create(aeronDataStreamId, this, aeron, aeronChannel);
        IdleStrategy publicationIdleStrategy = IdleStrategyProvider.getIdleStrategy(options.channelPerformance);
        //this.aeronPublisher = AeronPublicationDSAdapter.create(aeronCommandStreamId, aeron, publicationIdleStrategy);

        this.ds = channel;
        this.tracker = new DisposableResourceTracker(this);
        this.idleStrategy = IdleStrategyProvider.getIdleStrategy(this.options.channelPerformance);
        this.lowLatency = options.channelPerformance.isLowLatency();

        this.currentStreamIdx = 0;
        this.currentStreamKey = stream.getKey();
    }

    public boolean         isClosed () {
        if (closed) {
            return true;
        }
        synchronized (interlock) {
            return ds != null && (ds.getState() == VSChannelState.Closed ||
                    ds.getState() == VSChannelState.Removed);
        }
    }

    public void            close () {
        closed = true;

        /*
        if (!aeronPublisher.isClosed()) {
            try {
                synchronized (sendlock) {
                    DataOutputStream out = aeronPublisher.getDataOutputStream();
                    out.writeShort (CURREQ_DISCONNECT);
                    out.flush ();
                }
            } catch (IOException iox) {
                TickDBClient.LOGGER.log (Level.WARNING, "Error disconnecting from server - ignored.", iox);
            }
        }
        */

        synchronized (interlock) {
            Util.close(ds);
            cursorAeronClient.close();
            //aeronPublisher.close();
        }
        setAvailabilityListener(null);
        Util.close(tracker);
        tracker = null;
    }

    @Override
    public boolean                      next () {
        return next(true) == NextResult.OK;
    }

    private NextResult                  next (boolean throwable) {

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

            if (nextStreamMsgState == NextMessageState.END_OF_STREAM) {
                nextStreamMsgState = NextMessageState.NOT_QUERIED;
                isAtEnd = true;
                return NextResult.END_OF_CURSOR;
            }
            else {
                assert nextStreamMsgState == NextMessageState.READY;

                nextStreamMsgState = NextMessageState.NOT_QUERIED;

                boolean success = setupMessage(streamMsgInput, streamMsgNanoTime, -1);
                if (!success) {
                    // In case of failure we can have some missing data
                    streamMsgInput.seek(streamMsgInput.getLength());
                }
                advanceBufferPos(streamMsgInput.getPosition() + streamMsgInput.getStart());
                if (success) {
                    return NextResult.OK;
                }
            }
        }
    }

    // TODO: Remove this method
    private void advanceBufferPos(int position) {
        assert position > bigBufferPos;
        //System.out.println("Advance: " + (position - bigBufferPos));
        if (nextExpectedPos != -1) {
            if (position != nextExpectedPos) {
                throw new AssertionError();
            }
            nextExpectedPos = -1;
        }

        bigBufferPos = position;
    }

    private boolean hasBufferedMessages() {
        return bigBufferLimit > bigBufferPos && (incompletePartialMessageOffset == NO_PARTIAL_MESSAGE || incompletePartialMessageOffset > bigBufferPos);
    }

    private int remainingData() {
        return bigBufferLimit - bigBufferPos;
    }

    private int remainingSpace() {
        return bigInputArrayBuffer.capacity() - bigBufferLimit;
    }

    @Override
    public ControlledFragmentHandler.Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        assert remainingSpace() >= 0;
        if (remainingSpace() >= length + Integer.BYTES) {
            loadDataToBufferWithPartialCheck(buffer, offset, length);
            //System.out.println("Got: " + length);
            if (bigBufferLimit >= TARGET_BUFFER_SIZE && hasBufferedMessages()) {
                // We got enough, stop for now
                return ControlledFragmentHandler.Action.BREAK;
            } else {
                // We can get some more
                return ControlledFragmentHandler.Action.CONTINUE;
            }
        } else {
            // We are out of space
            if (hasBufferedMessages()) {
                // We already have some buffered messages so we can abort loading current message and come back to it later
                return ControlledFragmentHandler.Action.ABORT;
            } else {
                // Seems like message does not fit the buffer. So we need to expand
                loadDataToBufferWithPartialCheck(buffer, offset, length);
                //System.out.println("Got big: " + length);
                if (incompletePartialMessageOffset != NO_PARTIAL_MESSAGE) {
                    // We got big normal message => stop
                    return ControlledFragmentHandler.Action.BREAK;
                } else {
                    // This is only part and we need more parts => try to get next fragment
                    return ControlledFragmentHandler.Action.CONTINUE;
                }
            }
        }
    }

    //private int fragmentCounter = 0;

    private void loadDataToBufferWithPartialCheck(DirectBuffer buffer, int offset, int length) {
        // Peek header byte
        byte code = buffer.getByte(offset);
        if (code == TDBProtocol.CURRESP_MSG_MULTIPART_HEAD) {
            processMultipartHead(buffer, offset, length, code);
            //fragmentCounter = 1;
        } else if (code == TDBProtocol.CURRESP_MSG_MULTIPART_BODY) {
            processMultipartBody(buffer, offset, length);
            //fragmentCounter ++;
        } else {
            assert code <= CURRESP_MSG;
            assert incompletePartialMessageOffset == NO_PARTIAL_MESSAGE;
            // Common full message => just write it's size
            writeSizeToBuffer(length);
            loadDataToBuffer(buffer, offset, length);
        }
    }

    private void processMultipartHead(DirectBuffer buffer, int offset, int length, byte code) {
        assert incompletePartialMessageOffset == NO_PARTIAL_MESSAGE;

        // Read size header
        int fullMessageSize = buffer.getInt(offset + PARTIAL_MSG_HEADER_CODE_SIZE, ByteOrder.BIG_ENDIAN);
        incompletePartialMessageOffset = bigBufferLimit;
        incompletePartialMessageSize = fullMessageSize;

        writeSizeToBuffer(fullMessageSize);

        // Write byte
        bigInputArrayBuffer.putByte(bigBufferLimit, (byte) TDBProtocol.CURRESP_MSG);
        bigBufferLimit += PARTIAL_MSG_HEADER_CODE_SIZE;

        // Do not copy code byte and size
        offset += PARTIAL_MSG_HEADER_CODE_SIZE + PARTIAL_MSG_HEADER_SIZE_FIELD_SIZE;
        length -= PARTIAL_MSG_HEADER_CODE_SIZE + PARTIAL_MSG_HEADER_SIZE_FIELD_SIZE;

        assert bigBufferLimit + length < incompletePartialMessageOffset + incompletePartialMessageSize : "Message should not be partitioned (too small)";
        loadDataToBuffer(buffer, offset, length);
    }

    private void processMultipartBody(DirectBuffer buffer, int offset, int length) {
        assert incompletePartialMessageOffset != NO_PARTIAL_MESSAGE;

        // This is part of big message => skip first byte
        offset += PARTIAL_MSG_HEADER_CODE_SIZE;
        length -= PARTIAL_MSG_HEADER_CODE_SIZE;

        loadDataToBuffer(buffer, offset, length);
        int bytesRemaining = incompletePartialMessageOffset + PARTIAL_MSG_HEADER_SIZE_FIELD_SIZE + incompletePartialMessageSize - bigBufferLimit;
        assert bytesRemaining >= 0;
        if (bytesRemaining == 0) {
            incompletePartialMessageOffset = NO_PARTIAL_MESSAGE;
            incompletePartialMessageSize = 0;
        }
    }

    private void writeSizeToBuffer(int length) {
        bigInputArrayBuffer.putInt(bigBufferLimit, length);
        bigBufferLimit += Integer.BYTES;
    }

    private void loadDataToBuffer(DirectBuffer buffer, int offset, int length) {
        buffer.getBytes(offset, bigInputArrayBuffer, bigBufferLimit, length);
        bigBufferLimit += length;
    }

    private DataInputStream bufferToDataInputStream(DirectBuffer buffer, int offset, int length) {
        byte[] bytes = buffer.byteArray();
        InternalByteArrayInputStream arrayInput = this.arrayInput;
        if (bytes != null) {
            // Has backing array => use array directly
            arrayInput.setBuffer(bytes, offset + buffer.wrapAdjustment(), length);
        } else {
            // No backing array. We have to copy data to inputArrayBuffer
            buffer.getBytes(offset, inputArrayBuffer, 0, length);
            arrayInput.setBuffer(inputArrayBuffer.byteArray(), 0, length);
        }
        return dataInputWrapper;
    }

    private MemoryDataInput bufferToMemoryDataInput(DirectBuffer buffer, int offset, int length) {
        byte[] bytes = buffer.byteArray();
        MemoryDataInput mdi = this.streamMsgInput;
        if (bytes != null) {
            // Has backing array => use array directly
            mdi.setBytes (bytes, offset + buffer.wrapAdjustment(), length);
        } else {
            // No backing array. We have to copy data to inputArrayBuffer
            buffer.getBytes(offset, inputArrayBuffer, 0, length);
            mdi.setBytes(inputArrayBuffer.byteArray(), 0, length);
        }
        return mdi;
    }

    private final IdleStrategy idleStrategy;

    private int nextMessagePos = 0;

    /**
     *  Keep processing responses until a message comes in or end of
     *  stream is reached. If end of stream is reached,
     *  set nextStreamMsgState = NextMessageState.END_OF_STREAM.
     *  Otherwise: receive message bytes;
     *  set nextStreamMsgState = NextMessageState.READY; set up
     *  streamMsgInput (positioned right after the initial timestamp);
     *  set streamMsgTicks to the timestamp of the received message.
     */
    private NextResult queryStream() {
        assert bigBufferPos <= nextMessagePos : "Processing of previous message read data of next message";
        bigBufferPos = nextMessagePos; // Discard any unprocessed data
        nextExpectedPos = -1; // TODO: Remove "nextExpectedPos" entirely
        while (true) {
            if (!hasBufferedMessages()) {
                int remainingDataSize = remainingData();
                if (remainingDataSize == 0) {
                    assert bigBufferPos == nextMessagePos;
                    bigBufferPos = 0;
                    bigBufferLimit = 0;
                    nextMessagePos = 0;
                } else if (bigBufferPos > 0) { // TODO: Put higher threshold here to avoid moving big messages over short distance
                    byte[] bytes = bigInputArrayBuffer.byteArray();
                    System.arraycopy(bytes, bigBufferPos, bytes, 0, remainingDataSize);
                    if (incompletePartialMessageOffset != NO_PARTIAL_MESSAGE) {
                        incompletePartialMessageOffset -= bigBufferPos;
                    }
                    bigBufferPos = 0;
                    nextMessagePos = 0;
                    bigBufferLimit = remainingDataSize;
                }

                // Note: we have to call hasBufferedMessages here because we might get only part of big message
                boolean gotMessages = cursorAeronClient.pageDataIn() && hasBufferedMessages();
                if (!gotMessages) {
                    // No messages
                    if (callerListener != null) {
                        // TODO: Check why whe need lock here and if it's correct now.
                        synchronized (asyncLock) {
                            listenerIsArmed = true;
                            subscriptionChecker.addSubscriptionToCheck(cursorAeronClient.getSubscription(), lnrAdapter);
                            return NextResult.UNAVAILABLE;
                        }
                    } else {
                        if (isClosed()) {
                            throw new CursorIsClosedException();
                        }
                        idleStrategy.idle();
                        continue;
                    }
                } else {
                    idleStrategy.reset();
                }
            }
            assert hasBufferedMessages();
            if (bigBufferPos != nextMessagePos) {
                throw new AssertionError("Current buffer position does not match expected message boundary start");
            }
            int length = bigInputArrayBuffer.getInt(bigBufferPos); // WRITE_MESSAGE_SIZE
            nextMessagePos += Integer.BYTES + length;
            byte code = bigInputArrayBuffer.getByte(bigBufferPos + Integer.BYTES);

            if (code < 0) {
                throw new com.epam.deltix.util.io.UncheckedIOException(new EOFException());
            }
            bigBufferPos += Integer.BYTES + Byte.BYTES;
            int size = length - Byte.BYTES;
            try {

                switch (code) {
                    case CURRESP_END_OF_CURSOR:
                        assert length == Byte.BYTES;
                        if (processEOC()) {
                            return NextResult.END_OF_CURSOR;
                        }

                        break;


                    case CURRESP_ERROR:
                        try {
                            processError(bufferToDataInputStream(bigInputArrayBuffer, bigBufferPos, size));
                        } finally {
                            advanceBufferPos(this.arrayInput.getPosition());
                        }
                        break;

                    case CURRESP_LOAD_TYPE: {
                        // We can get more than one type in single message. TODO: We might consider changing this.
                        try {
                            DataInputStream dataInputStream = bufferToDataInputStream(bigInputArrayBuffer, bigBufferPos, size);
                            processLoadType(dataInputStream);
                            while (dataInputStream.available() > 0) {
                                int code2 = dataInputStream.read();
                                assert code2 == CURRESP_LOAD_TYPE;
                                processLoadType(dataInputStream);
                            }
                        } finally {
                            advanceBufferPos(this.arrayInput.getPosition());
                        }
                        break;
                    }
                    case CURRESP_LOAD_ENTITY:
                        try {
                            processLoadEntity(bufferToDataInputStream(bigInputArrayBuffer, bigBufferPos, size));
                        } finally {
                            advanceBufferPos(this.arrayInput.getPosition());
                        }
                        //assert dataInputWrapper.available() == 0;

                        break;
                    /*
                    case CURRESP_LOAD_STREAM:
                        try {
                            processLoadStream(bufferToDataInputStream(bigInputArrayBuffer, bigBufferPos, size));
                            //assert dataInputWrapper.available() == 0;
                        } finally {
                            advanceBufferPos(this.arrayInput.getPosition());
                        }
                        break;
                    */
                    case TDBProtocol.CURRESP_MSG:
                        if (processMessage(bufferToMemoryDataInput(bigInputArrayBuffer, bigBufferPos, size), size)) {
                            return NextResult.OK;
                        } else {
                            advanceBufferPos(nextMessagePos);
                            //assert streamMsgInput.getAvail() == 0;
                        }

                        break;


                    default:
                        throw new IllegalStateException();
                }


            } catch (EOFException e) {
                throw new CursorIsClosedException(e);
            } catch (IOException e) {
                if (cursorAeronClient.isClosed()) {
                    throw new CursorIsClosedException(e);
                } else {
                    onX(e);
                    throw new IllegalStateException();
                }
            }
        }
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

    private void                    processLoadType(DataInputStream dataInputStream) throws IOException {
        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_TYPE; state=" + state);
        }

        int typeIndex = dataInputStream.readShort();
        ClassDescriptor cd = ClassDescriptor.readFrom(dataInputStream, types, 0);

        // Note: typeIndex == -1 is special case: this means type without concrete index. We just add such types without checks.
        if (typeIndex < 0 || typeIndex == types.count()) {
            // New type in correct order => Just add it
            types.addType(typeIndex, cd);
        } else if (typeIndex > entities.size()) {
            // We missed some data => reload it from server
            reloadMetadataSync();
        } else {
            // We got data for type that we already have (due to reload).
            // Just ignore it
        }
    }

    private void                    processLoadEntity(DataInputStream dataInputStream) throws IOException {
        int entityIndex = dataInputStream.readInt();

        ConstantIdentityKey  id = readIdentityKey (dataInputStream);

        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_ENTITY (" + id + "); state=" + state);
        }

        if (entityIndex == entities.size()) {
            // New entity in correct order => Just add it
            entities.add(id);
        } else if (entityIndex > entities.size()) {
            // We missed some data => reload it from server
            reloadMetadataSync();
        } else {
            // We got data for entity that we already have (due to reload).
            // Just ignore it
        }
    }

    private void                    processLoadStream(DataInputStream dataInputStream) throws IOException {
        String  key = dataInputStream.readUTF();

        if (DEBUG_COMM) {
            TickDBClient.LOGGER.info (describe() + ": RECEIVED CURRESP_LOAD_STEAM (" + key + "); state=" + state);
        }

        streamKeys.add (key);
    }




    private boolean                 processMessage(MemoryDataInput streamMsgInput, int size) throws IOException {
        if (nextExpectedPos != -1) {
            throw new AssertionError();
        }
        this.nextExpectedPos = bigBufferPos + size;
        if (USE_MAGIC) {
            int a = streamMsgInput.readUnsignedByte();
            int b = streamMsgInput.readUnsignedByte();

            if (a != 35 || b != 214)
                throw new IOException ("magic wrong: " + a + ", " + b);
        }


        long        sequence = -1;

        if (SEND_SEQUENCE) {
            sequence = streamMsgInput.readLong ();
        }
        //int size = streamMsgInput.getAvail();

        // TODO: Add sync back!
        synchronized (interlock) {
            if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                TickDBClient.LOGGER.info (describe() + ": RECEIVED MESSAGE (size=" + size + "; sequence=" + sequence + "); state=" + state);
            }

            if (state == ReceiverState.FAST_FORWARD) {
                if (DEBUG_COMM && DEBUG_MSG_DISCARD) {
                    TickDBClient.LOGGER.info (describe() + ":     state=FAST_FORWARD - message ignored (timestamp unknown).");
                }
                return (false); // keep reading
            }

            if (TDBProtocol.USE_TIME_CODEC_FOR_AERON) {
                streamMsgNanoTime = TimeCodec.readNanoTime(streamMsgInput);
            } else {
                streamMsgNanoTime = streamMsgInput.readLong();
            }


            if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
                TickDBClient.LOGGER.info (describe() + ":     nanos=" + streamMsgNanoTime + "; state=NORMAL - message returned.");
            }
        }

        nextStreamMsgState = NextMessageState.READY;
        return (true);
    }

    private boolean         isRealTimeMessage(RecordClassDescriptor type) {
        return RealTimeStartMessage.DESCRIPTOR_GUID.equals(type.getGuid());
    }

    private boolean        setupMessage (MemoryDataInput mdi, long nanos, long serial) {
        //currentStreamIdx = mdi.readShort ();
        //currentStreamKey = currentStreamIdx != -1 ? streamKeys.get (currentStreamIdx) : null;

        currentEntityIdx = mdi.readInt ();
        if (currentEntityIdx >= entities.size()) {
            // We miss entity data
            reloadMetadataSync();
        }

        final ConstantIdentityKey     entity = entities.get (currentEntityIdx);
        currentTypeIdx = mdi.readUnsignedByte ();
        if (!types.isIndexPresent(currentTypeIdx)) {
            // We messed some type data
            reloadMetadataSync();
        }
        currentType = types.getConcreteTypeByIndex (currentTypeIdx);

        // TODO: Add sync back!
        boolean rtStarted = realtimeAvailable && isRealTimeMessage(currentType);
        if (rtStarted) {
            isRealTime = true;
        }


        if (raw) {
            rawMessage.type = currentType;
            rawMessage.setBytes (mdi.getBytes (), mdi.getCurrentOffset (), mdi.getAvail ());
            mdi.seek(mdi.getPosition() + mdi.getAvail ());// Mark data as consumed
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
        curMsg.setSymbol (entity.symbol);

        if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG)
            TickDBClient.LOGGER.info (describe() + ": setupMessage=" + curMsg + "; isRealtime=" + isRealTime + "; serial=" + serial);

        return (true);
    }

    private void reloadMetadataSync() {
        DataOutputStream out = ds.getDataOutputStream();
        try {
            out.writeShort(CURREQ_GET_MULTICAST_CURSOR_METADATA);
            out.flush();
            DataInputStream din = ds.getDataInputStream();
            int entityDataLen = din.readInt();
            int typeDataLen = din.readInt();

            // Note: allocation. TODO: reuse?
            byte[] buffer = new byte[Math.max(entityDataLen, typeDataLen)];
            InternalByteArrayInputStream arrayInputStream = new InternalByteArrayInputStream(buffer);
            DataInputStream bufferedData = new DataInputStream(arrayInputStream);

            readEntityData(din, entityDataLen, buffer, arrayInputStream, bufferedData);

            readTypeData(din, typeDataLen, buffer, arrayInputStream, bufferedData);
        } catch (IOException iox) {
            onX(iox);
        }
    }

    private void readEntityData(DataInputStream din, int entityDataLen, byte[] buffer, InternalByteArrayInputStream arrayInputStream, DataInputStream bufferedData) throws IOException {
        din.readFully(buffer, 0, entityDataLen);
        arrayInputStream.setBuffer(buffer, 0, entityDataLen);
        int currentEntityCount = 0;
        int numberOfAlreadyLoadedEntities = entities.size(); // Usually should be 0.
        while (arrayInputStream.available() > 0) {
            currentEntityCount++;
            ConstantIdentityKey id = readIdentityKey(bufferedData);
            // Add only new entities
            if (currentEntityCount > numberOfAlreadyLoadedEntities) {
                entities.add(id);
            }
        }
    }

    private void readTypeData(DataInputStream din, int typeDataLen, byte[] buffer, InternalByteArrayInputStream arrayInputStream, DataInputStream bufferedData) throws IOException {
        din.readFully(buffer, 0, typeDataLen);
        arrayInputStream.setBuffer(buffer, 0, typeDataLen);
        int currentTypeIndex = 0;
        int lastAlreadyKnownIndex = types.count() - 1; // Usually should be -1.
        while (arrayInputStream.available() > 0) {
            int code = bufferedData.read();
            if (code != CURRESP_LOAD_TYPE) {
                throw new IllegalStateException();
            }

            int idx = bufferedData.readShort();
            ClassDescriptor cd = ClassDescriptor.readFrom(bufferedData, types, 0);

            // Add only new entities
            if (idx < 0 || currentTypeIndex > lastAlreadyKnownIndex) {
                types.addType(idx, cd);
            }
            if (idx >= 0) {
                assert idx == currentTypeIndex;
                currentTypeIndex++;
            }
        }
    }

    private void                 processError (DataInputStream in) {
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

    @Override
    public String                      getCurrentStreamKey () {
        return (currentStreamKey);
    }

    @Override
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

    @Override
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
        Runnable prevLnr = this.callerListener;
        if (this.lowLatency) {
            if (prevLnr == null && lnr != null) {
                subscriptionChecker.registerCritical(this);
            } else if (prevLnr != null && lnr == null) {
                subscriptionChecker.unregisterCritical(this);
            }
        }
        this.callerListener = lnr;
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
