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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.util.Util;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;
import com.epam.deltix.qsrv.hf.tickdb.impl.TransientStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.TransientMessageQueue;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.util.concurrent.QuickExecutor;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Message queue for {@link TransientStreamImpl} based on LMAX Disruptor.
 * <p>
 * <b>WARNING</b>: Lossless version for this queue has major issue. See {@link DisruptorQueueWriterLossless}.
 * </p>
 *
 * @author Alexei Osipov
 */
public class DisruptorMessageQueue implements TransientMessageQueue {
    final TransientStreamImpl stream;
    private final boolean lossless;
    private final ByteRingBuffer ringBuffer;

    protected volatile DisruptorQueueReader[] readers = new DisruptorQueueReader[0];
    private final Object readerListLock = new Object();

    /**
     * <p>Contract 1: waitingReaderCount is updated only reader.waiting flag was updated.
     * <p>Contract 2: order of update: set/clear reader.waiting flag, then increment/decrement waitingReaderCount.
     */
    protected final AtomicInteger waitingReaderCount = new AtomicInteger(0);

    volatile boolean closed = false;
    protected final Object writeLock = new Object();

    private final QuickExecutor.QuickTask notifier;

    public DisruptorMessageQueue(TransientStreamImpl stream, boolean lossless) {
        this.stream = stream;
        this.lossless = lossless;
        int ringBufferSize = getRingBufferSize(stream.getBufferOptions());
        notifier =  new QuickExecutor.QuickTask(stream.getQuickExecutor()) {
            @Override
            public void run() {
                DisruptorQueueReader[] readers = DisruptorMessageQueue.this.readers;
                int length = readers.length;
                if (length > 0) {
                    for (DisruptorQueueReader reader : readers) {
                        reader.notifyListenerIfWaiting();
                    }
                }
            }
        };

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.ringBuffer = ByteRingBuffer.createSingleProducer(ringBufferSize, new DisruptorQueueWaitStrategy(this, new BlockingWaitStrategy()));
    }

    private int getRingBufferSize(BufferOptions bufferOptions) {
        int maxBufferSize = bufferOptions.maxBufferSize;
        int ringBufferSize = Util.ceilingNextPowerOfTwo(maxBufferSize);
        if (ringBufferSize > maxBufferSize) {
            // maxBufferSize was not a power of 2. Divide it by 2.
            ringBufferSize = ringBufferSize >>> 1;
        }
        return ringBufferSize;
    }


    @Override
    public MessageChannel<InstrumentMessage> getWriter(MessageEncoder<InstrumentMessage> encoder) {
        if (closed) {
            throw new IllegalStateException("Queue closed");
        }
        if (lossless) {
            return new DisruptorQueueWriterLossless(this, ringBuffer, encoder);
        } else {
            return new DisruptorQueueWriterLossy(this, ringBuffer, encoder);
        }
    }

    /**
     * This method may block in case of full lossless queue.
     */
    @Override
    public QueueMessageReader getMessageReader(QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification) {
        if (closed) {
            throw new IllegalStateException("Queue closed");
        }
        DisruptorQueueReader reader = lossless ?
            new DisruptorQueueReaderLossless(this, ringBuffer, filter, polymorphic, realTimeNotification) :
            new DisruptorQueueReaderLossy(this, ringBuffer, filter, polymorphic, realTimeNotification,
                    stream.createEncoder(Messages.DATA_LOSS_MESSAGE_DESCRIPTOR));

        synchronized (writeLock) {
            Sequence sequence = reader.getSequence();
            ringBuffer.addGatingSequences(sequence);
            reader.updateConsumed();

            long offset = sequence.get();
            if (offset >= 0) {
                if (DebugFlags.DEBUG_MSG_LOSS) {
                    DebugFlags.loss("Reader started from offset=" + offset);
                }
            }

            addReader(reader);
        }
        return reader;
    }

    private void addReader(DisruptorQueueReader reader) {
        synchronized (readerListLock) {
            // Copy on write
            int length = readers.length;
            DisruptorQueueReader[] temp = new DisruptorQueueReader[length + 1];
            System.arraycopy(readers, 0, temp, 0, length);
            temp[length] = reader;
            readers = temp;
        }
    }

    private void removeReader(DisruptorQueueReader reader) {
        synchronized (readerListLock) {
            // Copy on write
            int length = readers.length;
            int index = ArrayUtils.indexOf(readers, reader);
            if (index < 0) {
                // This reader was never added or already was removed
                return;
            }

            if (length > 1) {
                DisruptorQueueReader[] temp = new DisruptorQueueReader[length - 1];
                if (index > 0) {
                    // Copy elements before deleted (if any)
                    System.arraycopy(readers, 0, temp, 0, index);
                }
                int elementsAfter = length - index - 1;
                if (elementsAfter > 0) {
                    // Copy elements after deleted (if any)
                    System.arraycopy(readers, index + 1, temp, index, elementsAfter);
                }
                readers = temp;
            } else {
                readers = new DisruptorQueueReader[0];
            }
        }
    }

    /**
     * This method may block in case of full lossless queue.
     */
    @Override
    public void close() {
        closed = true;
        synchronized (readerListLock) {
            DisruptorQueueReader[] readers = this.readers;
            for (DisruptorQueueReader reader : readers) {
                reader.close();
            }
            this.readers = new DisruptorQueueReader[0];
        }
    }

    protected String describe() {
        return getClass().getSimpleName() + "@" + hashCode() + " [" + this.stream.getKey() + "]";
    }

    protected void unregisterReader(DisruptorQueueReader reader) {
        removeReader(reader);
        //waitingReaders.remove(reader);
        ringBuffer.removeGatingSequence(reader.getSequence());
    }

    private long timeSpentToWakeWaiting = 0;
    private long timeSpentToWakeWaitingLastReport = 0;
    private long callCount = 0;
    private long fastCount = 0;
    private long slowCount = 0;
    private static final boolean MEASURE_READER_WAKE_UP_OVERHEAD = false;

    protected void notifyWaitingReaders() {

        long t0;
        if (MEASURE_READER_WAKE_UP_OVERHEAD) {
            t0 = System.nanoTime();
        }

        if (waitingReaderCount.get() > 0) {
            notifier.submit();
            if (MEASURE_READER_WAKE_UP_OVERHEAD) {
                slowCount++;
            }
        } else {
            if (MEASURE_READER_WAKE_UP_OVERHEAD) {
                fastCount++;
            }
        }

        if (MEASURE_READER_WAKE_UP_OVERHEAD) {
            long t1 = System.nanoTime();
            timeSpentToWakeWaiting += t1 - t0;
            callCount++;
            if ((t1 - timeSpentToWakeWaitingLastReport) > TimeUnit.SECONDS.toNanos(10)) {
                timeSpentToWakeWaitingLastReport = t1;
                long msWaited = TimeUnit.NANOSECONDS.toMillis(timeSpentToWakeWaiting);
                System.out.println("Total time spent in waking readers: " + msWaited + " (ms). callCount: " + callCount + ". Fast/slow ratio: " + (fastCount / ((float) slowCount + 1)) + ". Nanos per call: " + (timeSpentToWakeWaiting / callCount));
            }
        }
    }

}