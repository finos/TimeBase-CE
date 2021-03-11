package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue.DisruptorMessageQueue;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.TransientMessageQueue;
import com.epam.deltix.util.collections.ByteQueue;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

/**
 *  
 */
abstract class MessageQueue implements TransientMessageQueue {

    static TransientMessageQueue forStream(TransientStreamImpl stream) {
        boolean lossless = stream.getBufferOptions().lossless;
        boolean useDisruptor = Boolean.parseBoolean(System.getProperty(TickStreamImpl.USE_DISRUPTOR_QUEUE_PROPERTY_NAME, "false"));
        if (useDisruptor) {
            return new DisruptorMessageQueue(stream, lossless);
        } else if (lossless) {
            return new LosslessMessageQueue(stream);
        } else {
            return new LossyMessageQueue(stream);
        }
    }

    protected final TransientStreamImpl stream;
    private final BufferOptions         options;    
    private final byte []               headBuffer =
        new byte [MessageSizeCodec.MAX_SIZE + TimeCodec.MAX_SIZE];
    private final MemoryDataInput       headIn =
        new MemoryDataInput (headBuffer);
    
    /**
     *  The abstract offset of the first byte in the queue,
     *  counted since queue creation.
     */
    private long                        virtualOffsetOfBuffer;

    /**
     *  The byte queue.
     */
    private ByteQueue                   writeBuffer = null;

    protected final WaitingReaders      waitingReaders = new WaitingReaders ();

    volatile boolean                    closed = false;

    MessageQueue (TransientStreamImpl stream) {
        this.stream = stream;
        this.options = stream.getBufferOptions ();
        
        writeBuffer = new ByteQueue (options.initialBufferSize);      
    }

    final TickStreamImpl            getStream () {
        return stream;
    }

    private int                     extendBuffer (int requiredSize) {
        int     max = options.maxBufferSize;

        //assert requiredSize <= max : "requiredSize (" + requiredSize + ") > maxBufferSize (" + max + ")";
        
        int     desiredSize = Util.doubleUntilAtLeast (writeBuffer.capacity (), requiredSize);

        if (desiredSize > max)
            desiredSize = max;
        
        writeBuffer.setCapacity (desiredSize);
        return (desiredSize);
    }

    abstract boolean                hasNoReaders ();

    public abstract MessageChannel<InstrumentMessage> getWriter(
        MessageEncoder<InstrumentMessage> encoder
    );

    /**
     *
     * @param timestamp
     * @param src
     * @param srcOffset
     * @param srcLength
     * @return The global offset of the message just written.
     * @throws UnavailableResourceException
     *              When no-loss flag is set,
     *              and there is no room in the write buffer. The caller should
     *              then wait for notification.
     */
    long               writeMessage (
        long                            timestamp,
        byte []                         src,
        int                             srcOffset,
        int                             srcLength
    )
        throws UnavailableResourceException
    {
        long                offset;         

        if (DebugFlags.DEBUG_MSG_WRITE)
            DebugFlags.write (
                "TB DEBUG: writeMessage () file=" + this +
                "; timestamp=" + timestamp + "; length=" + srcLength
            );

        synchronized (this) {
            if (hasNoReaders ()) {
                if (DebugFlags.DEBUG_MSG_DISCARD)
                    DebugFlags.discard (
                        "TB DEBUG: writeMessage () file=" + describe() +
                        "; DISCARDING MESSAGE DUE TO NO READERS: timestamp=" +
                        timestamp + "; length=" + srcLength
                    );

                return (-1);
            }

            int     needRoom = srcLength + MessageSizeCodec.MAX_SIZE;

            if (needRoom > options.maxBufferSize)
                throw new IllegalArgumentException (
                    "Message will never fit in buffer, msg size: " +
                    needRoom + "; max buffer capacity: " + options.maxBufferSize
                );
            //
            // writeBufferSize: how much data is already in the buffer.
            // currentCapacity: how much data can fit in the buffer.
            //
            int     writeBufferSize = getWriteBufferSize ();
            int     requiredCapacity = writeBufferSize + needRoom;
            int     currentCapacity = writeBuffer.capacity ();
            //
            //  Ensure there is enough room
            //
            if (requiredCapacity > currentCapacity) {
                //
                //  Extend buffer if maxBufferTimeDepth allows
                //
                if (writeBufferSize == 0 || (currentCapacity < options.maxBufferSize &&
                    timestamp - getBufferFirstTime () <= options.maxBufferTimeDepth))
                        currentCapacity = extendBuffer (requiredCapacity);

                if (requiredCapacity > currentCapacity) {
                    if (options.lossless)
                        throw UnavailableResourceException.INSTANCE;

                    do {
                        int     sizeBefore = writeBufferSize;

                        int     size = MessageSizeCodec.read (writeBuffer);
                        writeBuffer.poll (null, 0, size);

                        writeBufferSize = writeBuffer.size ();

                        int     discarded = sizeBefore - writeBufferSize;

                        virtualOffsetOfBuffer += discarded;
                        requiredCapacity -= discarded;

                        if (DebugFlags.DEBUG_MSG_DISCARD)
                            DebugFlags.discard (
                                "TB DEBUG: discardMessage () file=" + describe() +
                                "; length=" + size
                            );
                    } while (requiredCapacity > currentCapacity);
                }
            }

            offset = virtualOffsetOfBuffer + writeBufferSize;

            MessageSizeCodec.write (srcLength, writeBuffer);
            writeBuffer.offer (src, srcOffset, srcLength);

            waitingReaders.launchNotifiers ();
        }

        return (offset);
    }

    
    /**
     *  Return the logical length of the data, including both the persisted
     *  and the buffered portions.
     */
    public long                     getUncommittedLength() {
        return (virtualOffsetOfBuffer + getWriteBufferSize ());
    }

    private int                     getWriteBufferSize () {
        return (writeBuffer == null ? 0 : writeBuffer.size ());
    }

    private long                    getBufferFirstTime() {
         // readTransient buffer first message time
        int         s = Math.min (headBuffer.length, getWriteBufferSize());

        writeBuffer.get (0, headBuffer, 0, s);
        headIn.seek (0);

        MessageSizeCodec.read(headIn);// skip size

        try {
            return (TimeCodec.readTime (headIn));
        } catch (Throwable e) {
            e.printStackTrace();
            return 0;
        }
    }

    abstract void                   advanceBegin (MessageQueueReader s);

    abstract boolean                advanceEnd (MessageQueueReader s);

    NextResult                      read (MessageQueueReader s) {
        boolean     queueWasRolled;
        
        synchronized (this) {
            try {
                advanceBegin (s);

                long            readAtOffset = s.currentFileOffset.get();
                long            bytesLost = virtualOffsetOfBuffer - readAtOffset;
                int             writeBufferSize = getWriteBufferSize ();

                if (bytesLost > 0) {
                    if (DebugFlags.DEBUG_MSG_LOSS)
                        DebugFlags.loss (
                            "TB DEBUG: read (): JUMP OVER HOLE; file=" + this +
                            "; readAtOffset=" + readAtOffset +
                            "; committedLength=" + virtualOffsetOfBuffer
                        );

                    readAtOffset = virtualOffsetOfBuffer;
                    s.currentFileOffset.set(readAtOffset);
                    ((LossyMessageQueueReader) s).onBytesLost(bytesLost);
                }
                //
                //  Read entry from the buffer
                //
                long            bufferOffset = readAtOffset - virtualOffsetOfBuffer;

                if (bufferOffset > writeBufferSize)
                    throw new IllegalStateException (
                        "Error while reading " + this + ": " +
                        " Reader out of bounds. Reading at " + readAtOffset +
                        " but writeBuffer ends at " + getUncommittedLength () + " (exclusive)"
                    );

                if (bufferOffset == writeBufferSize) {
                    if (DebugFlags.DEBUG_MSG_READ)
                        DebugFlags.read (
                            "TB DEBUG: read (): EOF: file=" + this +
                            "; readAtOffset=" + readAtOffset
                        );

                    if (s.live) {
                        waitingReaders.add (s);
                        s.isRealTime = false;
                        return NextResult.UNAVAILABLE;
                    }
                    else
                        return NextResult.END_OF_CURSOR;
                }

                assert      bufferOffset <= writeBufferSize;
                int         intBufferOffset = (int) bufferOffset; // No truncation possible
                int         dataLength = Math.min(writeBufferSize - intBufferOffset, s.getBufferSize()); // How much data we can get

                //System.out.println("Data length = " + dataLength);
                s.ensureBufferCapacity (dataLength); // TODO: This call is useless because dataLength <= buffer size.
                writeBuffer.get (intBufferOffset, s.buffer, s.getBufferOffset(), dataLength);
                s.currentFileOffset.set(readAtOffset + dataLength);

                // RSTM should be fired on start reading for transient streams
                if (s.isRealTime && s.live) {
                    waitingReaders.add (s);
                    s.isRealTime = false;
                    return NextResult.UNAVAILABLE;
                }

            } finally {
                queueWasRolled = advanceEnd (s);
            }
        }

        if (queueWasRolled)
            notifyWaitingWriters ();

        return NextResult.OK;
    }
    
    void                            notifyWaitingWriters () {
        throw new UnsupportedOperationException ();
    }

    abstract MessageQueueReader     getRawReader ();

    @Override
    public QueueMessageReader getMessageReader(QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification) {
        MessageQueueReader rawReader = getRawReader();
        if (filter != null) {
            rawReader.setFilter(filter, polymorphic);
        }
        if (realTimeNotification) {
            rawReader.enableRealTimeNotification();
        }
        return rawReader;
    }

    final boolean                       clearBuffer () {
        assert Thread.holdsLock (this);

        int         size = writeBuffer.size ();

        if (size == 0)  // it was empty already
            return (false);

        writeBuffer.clear ();
        virtualOffsetOfBuffer += size;
        return (true);
    }

    final boolean                       discardHead (long offset) {
        assert Thread.holdsLock (this);

        long    discard = offset - virtualOffsetOfBuffer;

        if (discard <= 0)
            return (false);

        // discard full messages only
        int total = (int) discard;

        while (total > MessageSizeCodec.MAX_SIZE) {
            int size = MessageSizeCodec.readNoPoll (writeBuffer, 0);
            size += MessageSizeCodec.fieldSize(size);
            if (total >= size)
                writeBuffer.poll (null, 0, size);
            else
                break;
            total -= size;
        }

        virtualOffsetOfBuffer = offset - total;
        return (true);
    }

    protected String                    describe() {
        return getClass().getSimpleName() + "@" + hashCode() + " [" + this.stream.getKey() + "]";
    }

    @Override
    public String                       toString() {
        return describe();
    }

    final synchronized long             getHeadVirtualOffset () {
        return (virtualOffsetOfBuffer);
    }

    public synchronized void                   close() {
        clearBuffer();
        closed = true;
    }
}
