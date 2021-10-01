/*
 * Copyright 2021 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteDataReceiver;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.InteractiveEventWriter;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;

/**
 * @author Alexei Osipov
 */
final class InstrumentMessageInteractiveEventWriter implements InteractiveEventWriter<InstrumentMessage> {
    private final MessageEncoder<InstrumentMessage> encoder;

    private final MemoryDataOutput buffer = new MemoryDataOutput(128); // Size will increase dynamically if needed
    private int bufferOffset;

    private boolean timestampIsNotSet;

    InstrumentMessageInteractiveEventWriter(MessageEncoder<InstrumentMessage> encoder) {
        this.encoder = encoder;
    }

    @Override
    public int prepare(InstrumentMessage msg) {
        long timestamp = msg.getTimeStampMs();
        buffer.reset();

        // Reserve space for size header.
        // It's length is variable and will be determined when full message is written to the buffer.
        buffer.skip(MessageSizeCodec.MAX_SIZE);

        // Write timestamp header.
        timestampIsNotSet = timestamp == TimeStampedMessage.TIMESTAMP_UNKNOWN;
        if (timestampIsNotSet) {
            // Timestamp is not set. Reserve space for it till the moment when we get slot in queue and will be eligible to write timestamp.
            buffer.skip(TimeCodec.MAX_SIZE);
        } else {
            TimeCodec.writeTime(msg, buffer);
        }

        encoder.encode(msg, buffer);

        int sizeWithoutSizeHeader = buffer.getSize() - MessageSizeCodec.MAX_SIZE;
        int sizeHeaderByteLength = MessageSizeCodec.fieldSize(sizeWithoutSizeHeader);
        bufferOffset = MessageSizeCodec.MAX_SIZE - sizeHeaderByteLength;

        // Write size header
        buffer.seek(bufferOffset);
        MessageSizeCodec.write(sizeWithoutSizeHeader, buffer);

        return sizeHeaderByteLength + sizeWithoutSizeHeader;
    }

    @Override
    public void write(ByteDataReceiver byteDataReceiver, long startSequence, int length) {
        if (timestampIsNotSet) {
            // Set time value
            assert buffer.getPosition() == MessageSizeCodec.MAX_SIZE;
            //buffer.seek(MessageSizeCodec.MAX_SIZE); // Time is always comes after size header
            long nanoTime = TimeKeeper.currentTimeNanos;
            TimeCodec.writeNanoTimeNoScale(nanoTime, buffer);
        }
        byteDataReceiver.write(buffer.getBuffer(), bufferOffset, startSequence, length);
    }
}