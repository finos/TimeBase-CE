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
package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.lmax.disruptor.*;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;
import com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue.util.PaddedAtomicBoolean;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.IOException;

/**
 * @author Alexei Osipov
 */
abstract class DisruptorQueueReader implements QueueMessageReader {
    private static final int BUFFER_SIZE =
        Util.getIntSystemProperty("TimeBase.queueReaderBufferSize", 8192, 256, Integer.MAX_VALUE);

    private static final int MIN_MESSAGE_BODY_SIZE = TimeCodec.MAX_SIZE + 1;
    // Note: this implementation always expects max size for time field.
    private static final int MIN_MESSAGE_SIZE = MessageSizeCodec.MIN_SIZE + MIN_MESSAGE_BODY_SIZE;


    private final DisruptorMessageQueue queue;
    private final ByteRingBuffer ringBuffer;
    private final SequenceBarrier sequenceBarrier;

    protected final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);

    protected long consumedSequence = Sequencer.INITIAL_CURSOR_VALUE;
    protected long cachedAvailableSequence = Sequencer.INITIAL_CURSOR_VALUE;
    private final QuickMessageFilter filter;
    private final boolean polymorphic;
    //private final boolean isRealTime;

    private volatile boolean running = true;

    private final PaddedAtomicBoolean waiting = new PaddedAtomicBoolean(false);

    private volatile Runnable availabilityListener = null;

    // Buffer
    // Contract: 0 <= bufferPosition <= bufferEnd <= buffer.length
    protected byte[] buffer;
    protected int bufferPosition = 0;// Index of position where data in buffer starts (inclusive)
    protected int bufferEnd = 0; // Index of position where data in buffer ends (exclusive)

    //protected final AtomicBoolean readLock = new AtomicBoolean(false);

    // Contains sequence number that is "blocked" by reader for data transfer operation (from RingBuffer to Reader buffer).
    // Writer is not permitted to write to any position equal or greater than this number.
    // This fields is not used by LOSSLESS reader and writer.
    protected volatile long seqInUse = Long.MAX_VALUE;

    // Fields for output
    private final MemoryDataInput mdi = new MemoryDataInput();
    protected final TimeStamp time = new TimeStamp();
    private final StringBuilder symbol = new StringBuilder();

    private final QuickExecutor.QuickTask notifier;

    DisruptorQueueReader(DisruptorMessageQueue disruptorMessageQueue, ByteRingBuffer ringBuffer, QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification) {
        this.queue = disruptorMessageQueue;
        this.ringBuffer = ringBuffer;
        this.sequenceBarrier = ringBuffer.newBarrier();
        this.filter = filter;
        this.polymorphic = polymorphic;
        //this.isRealTime = realTimeNotification;
        this.buffer = new byte[BUFFER_SIZE];

        notifier = new QuickExecutor.QuickTask(queue.stream.getQuickExecutor()) {
            @Override
            public void run() {
                final Runnable lnr = availabilityListener;
                if (lnr != null) {
                    boolean stopping = !running;
                    lnr.run();
                    if (stopping) {
                        // Help GC and release ref to the listener.
                        availabilityListener = null;
                    }
                } else {
                    synchronized (DisruptorQueueReader.this) {
                        DisruptorQueueReader.this.notifyAll();
                    }
                }

            }
        };
    }

    /**
     * This method must be executed after the reader sequence was added to RingBuffer and before any attempt to {@link #read()} from this buffer.
     */
    protected void updateConsumed() {
        this.consumedSequence = sequence.get();
        this.cachedAvailableSequence = this.consumedSequence;
    }

    /**
     * @return true if buffer contains at least one full message
     */
    private boolean hasDataAvailable() {
        long available = available();

        if (available < MIN_MESSAGE_SIZE) {
            return false;
        }

        int bodyLength = MessageSizeCodec.read(buffer, bufferPosition);
        assert bodyLength >= MIN_MESSAGE_BODY_SIZE;
        return available >= MessageSizeCodec.fieldSize(bodyLength) + bodyLength;
    }


    @Override
    public boolean read() throws IOException {
        assert running;

        while (true) {
            // assuming that buffer not contains only full messages
            while (!hasDataAvailable()) {
                if (!pageDataIn()) {
                    assert !(isLive() && isAsynchronous());
                    return false;
                }
            }

            int bodyLength = MessageSizeCodec.read(buffer, bufferPosition);

            bufferPosition += MessageSizeCodec.fieldSize(bodyLength);
            mdi.setBytes(buffer, bufferPosition, bodyLength);
            bufferPosition += bodyLength;

            TimeCodec.readTime(mdi, time);

            if (accept()) {
                if (DebugFlags.DEBUG_MSG_READ)
                    DebugFlags.read(
                        "TB DEBUG: read (): OK: file=" + describe() +
                            "; readAtOffset=" + getCurrentMessageOffset() +
                            "; num=" + bodyLength
                    );
                return true;
            }

            if (DebugFlags.DEBUG_MSG_DISCARD)
                DebugFlags.discard(
                    "TB DEBUG: read (): REJECTED: file=" + describe() +
                        "; readAtOffset=" + getCurrentMessageOffset() +
                        "; num=" + bodyLength
                );
        }
    }

    private long getCurrentMessageOffset() {
        return consumedSequence - bufferEnd + bufferPosition;
    }

    /**
     * Load data from RingBuffer to buffer of this reader.
     *
     * @return true if any data was loaded
     */
    abstract boolean pageDataIn();

    /**
     * Executes actual data transfer from {@link RingBuffer} to local buffer.
     */
    private void executeDataLoad(long availableSequence) {
        assert availableSequence > consumedSequence;

        prepareBuffer();
        int freeSpaceAtEndOfBuffer = buffer.length - bufferEnd;
        assert freeSpaceAtEndOfBuffer > 0; // We expect to have free space here

        int newDataAvailable = (int) (availableSequence - consumedSequence);
        int lengthToGet = Math.min(newDataAvailable, freeSpaceAtEndOfBuffer);

        assert lengthToGet > 0;
        ringBuffer.read(buffer, bufferEnd, consumedSequence + 1, lengthToGet);
        bufferEnd += lengthToGet;

        // Advance local cursor
        consumedSequence = consumedSequence + lengthToGet;
    }

    /**
     * @throws UnavailableResourceException if no data available
     */
    protected boolean asyncPageIn() {
        assert cachedAvailableSequence >= consumedSequence;
        if (cachedAvailableSequence == consumedSequence) {
            long cursor = sequenceBarrier.getCursor();
            if (cursor <= consumedSequence) {
                // No new data
                boolean waitWasSet = waiting.compareAndSet(false, true);
                if (waitWasSet) {
                    queue.waitingReaderCount.incrementAndGet();
                }

                // We have to double check cursor position. There is a chance that we already got new data.
                cursor = sequenceBarrier.getCursor();
                if (cursor <= consumedSequence) {
                    throw UnavailableResourceException.INSTANCE;
                }
                // Try to undo wait flag
                if (waitWasSet) {
                    boolean undoSuccess = waiting.compareAndSet(true, false);
                    if (undoSuccess) {
                        queue.waitingReaderCount.decrementAndGet();
                    }
                    // ...and continue
                }
            }
        }

        // We already checked that there is some data is available in buffer. So syncPageIn will not block.
        return syncPageIn(true);
    }

    protected boolean syncPageIn() {
        return syncPageIn(false);
    }

    /**
     * Waits for new data if no new data available.
     */
    protected boolean syncPageIn(boolean oneByteIsSufficient) {

        //sequenceBarrier.clearAlert();
        if (cachedAvailableSequence > consumedSequence) {
            executeDataLoad(cachedAvailableSequence);
            return true;
        }


        // If we get less then this number of bytes then we can be sure then there is NO full message
        int dataInBuffer = bufferEnd - bufferPosition;
        int minBytesToGet = oneByteIsSufficient ? 1 : Math.max(MIN_MESSAGE_SIZE - dataInBuffer, 1);
        long nextSequence = consumedSequence + minBytesToGet;
        while (true) {
            try {
                cachedAvailableSequence = sequenceBarrier.waitFor(nextSequence);
                if (cachedAvailableSequence >= nextSequence) {
                    executeDataLoad(cachedAvailableSequence);
                    return true;
                }
            } catch (AlertException e) {
                if (!running) {
                    // TODO: Check if this correct behaviour
                    throw new UncheckedInterruptedException(e);
                }
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            } catch (TimeoutException e) {
                // ignore
            }
        }
    }


    protected Sequence getSequence() {
        return sequence;
    }

    @Override
    public void setAvailabilityListener(Runnable listener) {
        this.availabilityListener = listener;
        //queue.registerNewDataListener(new ReaderListener(this, listener));
    }

    @Override
    public DXTickStream getStream() {
        return queue.stream;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public MemoryDataInput getInput() {
        return mdi;
    }

    @Override
    public long getTimestamp() {
        return time.timestamp;
    }

    @Override
    public long getNanoTime() {
        return time.getNanoTime();
    }

    /**
     * @return number of bytes loaded to reader buffer
     */
    @Override
    public long available() {
        return bufferEnd - bufferPosition;
    }

    @Override
    public void close() {
        running = false;
        queue.unregisterReader(this);
        sequenceBarrier.alert();
        if (availabilityListener != null) {
            notifyListener();
        }
    }

    protected boolean isAsynchronous() {
        return availabilityListener != null;
    }


    // TODO: This is coped from MessageQueueReader#accept. Make it DRY.
    protected boolean accept() {
        if (filter == null) {
            return true;
        }

        if (filter.acceptAllEntities()) {
            return true;
        }

        int pos = mdi.getPosition();

        if (polymorphic) {
            mdi.skipBytes(1);
        }

        mdi.readStringBuilder(symbol);

        boolean accepted = filter.acceptEntity(symbol);

        if (accepted) {
            mdi.seek(pos);
        }

        return accepted;
    }

    private void prepareBuffer() { // Former invalidateBuffer
        if (bufferPosition == bufferEnd) {
            // Buffer is empty
            if (bufferPosition > 0) {
                bufferPosition = 0;
                bufferEnd = 0;
            }
        } else if (bufferEnd == buffer.length) { // TODO: Possible optimization: we can relax this condition to avoid situation when just few bytes at the end of buffer are free.
            // Buffer is filled till the end
            int length = bufferEnd - bufferPosition;
            if (bufferPosition > (buffer.length / 2 + 1)) { // TODO: Possible optimization: Consider more strict condition to avoid buffer copy after each message when message size is close to 1/2 of the buffer.
                // We have half of buffer free. Shift data in the buffer to left.
                System.arraycopy(buffer, bufferPosition, buffer, 0, length);
                bufferEnd = length;
                bufferPosition = 0;
                //shiftedAtMessage = messageCount;
            } else {
                // Seems like we out of free space. Double buffer size.
                byte[] temp = new byte[buffer.length * 2];
                System.arraycopy(buffer, bufferPosition, temp, 0, length);
                buffer = temp;
                bufferEnd = length;
                bufferPosition = 0;
                //extendedAtMessage = messageCount;
            }
        }
/*        if (bufferPosition < bufferEnd) {
            if (buffer[bufferPosition] != 121) {
                throw new IllegalStateException("Bad state");
            }
        }*/

        assert buffer.length - bufferEnd > 1; // We expect to have at least 1 byte of free space at the end of buffer as result of this operation
    }

    // TODO: Copied from MessageQueReader. Make it DRY!
    protected String describe() {
        return getClass().getSimpleName() + "@" + hashCode() + ": [" + queue.describe() + "]";
    }

    public void notifyListenerIfWaiting() {
        boolean clearedFlag = waiting.compareAndSet(true, false);
        if (clearedFlag) {
            // We will do the notification
            notifier.submit();
            queue.waitingReaderCount.decrementAndGet();
        }
    }

    public void notifyListener() {
        notifier.submit();
    }
}