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

import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;
import com.epam.deltix.timebase.messages.service.DataLossMessage;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Reader for Lossy message queue.
 * <p>
 * If {@code dataLossMessageEncoder} provided then this reader will generate DataLossMessage message each time it detects data loss.
 *
 * @author Alexei Osipov
 */
final public class DisruptorQueueReaderLossy extends DisruptorQueueReader {
    private int incompleteMessageBytes = 0;

    private long bytesLost = 0; // How many bytes we skipped since previous read

    // Fields that is used to produce DataLoss message
    private final MemoryDataInput dataLossInput = new MemoryDataInput(0);
    private final MemoryDataOutput dataLossOutput = new MemoryDataOutput(64);
    private final MessageEncoder<InstrumentMessage> dataLossMessageEncoder;
    private final DataLossMessage dataLossMessage;

    DisruptorQueueReaderLossy(DisruptorMessageQueue disruptorMessageQueue, ByteRingBuffer ringBuffer,
                              QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification,
                              @Nullable MessageEncoder<InstrumentMessage> dataLossMessageEncoder) {
        super(disruptorMessageQueue, ringBuffer, filter, polymorphic, realTimeNotification);
        this.dataLossMessageEncoder = dataLossMessageEncoder;
        this.dataLossMessage = dataLossMessageEncoder != null ? createDataLossMessage(disruptorMessageQueue.stream.getName()) : null;
    }

    private static DataLossMessage createDataLossMessage(String streamName) {
        DataLossMessage result = new DataLossMessage();
        result.setSymbol(streamName);
        return result;
    }

    @Override
    public boolean read() throws IOException {
        long prevTime = time.timestamp;
        boolean hasNewMessage = super.read();

        if (dataLossMessageEncoder != null) {
            if (bytesLost > 0) {
                // We lost some data. Instead of returning next message we must return DataLoss message.
                hasNewMessage = true;
                fillDataLossMessage(prevTime, bytesLost);
                bytesLost = 0;
            } else {
                // Clear out current data loss message (if any).
                dataLossInput.reset(0);
            }
        }

        return hasNewMessage;
    }

    private void fillDataLossMessage(long prevTime, long bytesLost) {
        // Fill message object
        dataLossMessage.setBytes(bytesLost);
        dataLossMessage.setFromTime(prevTime);
        dataLossMessage.setNanoTime(time.getNanoTime());

        // Write message to dataLossOutput
        dataLossOutput.reset();
        dataLossMessageEncoder.encode(dataLossMessage, dataLossOutput);
        // And make it available as dataLossInput
        dataLossInput.setBytes(dataLossOutput);
    }

    @Override
    public MemoryDataInput getInput() {
        if (dataLossInput.hasAvail()) {
            // We have data loss message for the reader. Return it.
            return dataLossInput;
        }

        return super.getInput();
    }

    @Override
    boolean pageDataIn() {
        boolean dataLoaded;
        long currentSeq;
        try {
            // Mark which sequence number we are going to use to load data.
            seqInUse = consumedSequence + 1;
            currentSeq = sequence.get();
            // TODO: Investigate possible race condition: is it possible that writer not sees new value "seqInUse" even after updating "sequence"?
            // TODO: If this case is possible then we have to add additional check on volatile variable.
            while (currentSeq + 1 > seqInUse) {
                // We lost data. Update seqInUse and re-check.
                seqInUse = currentSeq + 1;
                currentSeq = sequence.get();
            }

            long missedBytes = currentSeq + incompleteMessageBytes - consumedSequence;
            if (missedBytes < 0) {
                throw new IllegalStateException("Seq decreased!");
            }

            if (missedBytes > 0) {
                bytesLost += missedBytes;
                // We missed some data!

                if (DebugFlags.DEBUG_MSG_LOSS) {
                    DebugFlags.loss(
                        "TB DEBUG: read (): JUMP OVER HOLE; file=" + this +
                            "; readAtOffset=" + (consumedSequence - incompleteMessageBytes) +
                            "; committedLength=" + currentSeq
                    );
                }

                // Discard what we had in buffer
                bufferPosition = 0;
                bufferEnd = 0;

                // Update cursors
                consumedSequence = currentSeq;
                cachedAvailableSequence = consumedSequence;
                incompleteMessageBytes = 0;
            }
            if (isAsynchronous()) {
                dataLoaded = asyncPageIn();
            } else {
                dataLoaded = syncPageIn();
            }
        } finally {
            // Release mark
            seqInUse = Long.MAX_VALUE;
        }

        // Confirm consumption
        if (dataLoaded) {
            // In lossy mode we confirm only full messages
            incompleteMessageBytes = bufferEnd - getIndexAfterLastFullMessage();
            long newValue = consumedSequence - incompleteMessageBytes;
            assert newValue > currentSeq;
            while (currentSeq < newValue) {
                if (sequence.compareAndSet(currentSeq, newValue)) {
                    break;
                } else {
                    currentSeq = sequence.get();
                }
            }
        }

        // We always have data in live mode
        assert dataLoaded || !isLive();
        return dataLoaded;
    }

    private int getIndexAfterLastFullMessage() {
        int messageStartPos = bufferPosition;
        int pos = messageStartPos;
        while (pos + MessageSizeCodec.MAX_SIZE <= bufferEnd) {
            int size = MessageSizeCodec.read(buffer, pos);
            pos += MessageSizeCodec.fieldSize(size) + size;
            if (pos <= bufferEnd) {
                messageStartPos = pos;
            }
        }
        return messageStartPos;
    }
}