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

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.Sequence;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
final class DisruptorQueueWriterLossy extends DisruptorQueueWriter {
    // Defines minimum portion of buffer to be released during clean up.
    private static final int MIN_RELEASE_BUFFER_PORTION = 32; // 1/32-th of buffer size.

    DisruptorQueueWriterLossy(DisruptorMessageQueue queue, ByteRingBuffer ringBuffer, MessageEncoder<InstrumentMessage> encoder) {
        super(queue, ringBuffer, encoder);
    }

    @Override
    protected void writeDataToRingBuffer(int length) {
        synchronized (queue.writeLock) {
            while (true) {
                try {
                    long hi = ringBuffer.tryNext(length);
                    ringBuffer.writeToAllocatedRange(writer, hi, length);
                    return;
                } catch (InsufficientCapacityException e) {
                    freeSpace(length);
                }
            }
        }
    }

    // Fields for debugging and performance measurement
    private long totalCleanupWaitTime = 0;
    private long lastTimeReport = 0;
    private static final boolean measureWaitTime = false;

    private void freeSpace(int length) {
        long lastUsedPosition = ringBuffer.getCursor();
        int bufferSize = ringBuffer.getBufferSize();
        long spaceToGet = Math.max(length, bufferSize / MIN_RELEASE_BUFFER_PORTION);
        long targetPosition = lastUsedPosition + spaceToGet - bufferSize; // Position, that we want to make free, inclusive
        long minimumNotConsumedSeq = ringBuffer.getMinimumGatingSequence();

        if (targetPosition < minimumNotConsumedSeq) {
            // Already enough space
            return;
        }

        long newLastProcessedMessageSeq = findNextMessageAfter(minimumNotConsumedSeq + 1, targetPosition) - 1;
        assert targetPosition <= newLastProcessedMessageSeq;


        DisruptorQueueReader[] readers = queue.readers;
        for (DisruptorQueueReader reader : readers) {
            Sequence readerSeq = reader.getSequence();
            long readerSeqVal = readerSeq.get();
            if (readerSeqVal <= targetPosition) {
                // This reader is behind targetPosition. So we have to advance it's cursor.
                while (readerSeqVal <= targetPosition) {
                    assert readerSeqVal < newLastProcessedMessageSeq;
                    if (readerSeq.compareAndSet(readerSeqVal, newLastProcessedMessageSeq)) {
                        break;
                    } else {
                        readerSeqVal = readerSeq.get();
                    }
                }
            }
        }
        long waitStartTs;
        if (measureWaitTime) {
            waitStartTs = System.nanoTime();
        }

        // Now we must ensure that all readers completed reading of region that we want to free up.
        for (DisruptorQueueReader reader : readers) {
            while (reader.seqInUse <= targetPosition) {
                // This reader still uses that region. Wait.
                Thread.yield();
                //LockSupport.parkNanos(1L);
            }
        }
        if (measureWaitTime) {
            long waitEndTs = System.nanoTime();
            // System.nanoTime();
            long waitTime = waitEndTs - waitStartTs;
            if (waitTime > 0) {
                totalCleanupWaitTime += waitTime;
                if ((waitEndTs - lastTimeReport) > TimeUnit.SECONDS.toNanos(10)) {
                    lastTimeReport = waitEndTs;
                    long msWaited = TimeUnit.NANOSECONDS.toMillis(totalCleanupWaitTime);
                    System.out.println("Total wait: " + msWaited);
                }
            }
        }
    }

    /**
     * Finds sequence number for position on the queue that matches beginning of the first message after
     * specified {@code targetPosition}.
     *
     * @param messageStartSeq known position of message (first message byte)
     * @param targetPosition  sequence number for position that we want to make free
     * @return sequence number for the first byte of message
     */
    private long findNextMessageAfter(long messageStartSeq, long targetPosition) {
        long newStart = messageStartSeq;
        while (newStart <= targetPosition) {
            int messageSize = MessageSizeCodec.readNoPoll(ringBuffer, (int) (newStart % ringBuffer.getBufferSize()));
            newStart += MessageSizeCodec.fieldSize(messageSize) + messageSize;
        }
        return newStart;
    }
}