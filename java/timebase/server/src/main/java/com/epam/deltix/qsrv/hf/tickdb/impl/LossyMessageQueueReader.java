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

import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.service.DataLossMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
final class LossyMessageQueueReader 
    extends MessageQueueReader <LossyMessageQueue>
{
    long                                                bytesLost;
    protected final MemoryDataInput                     input = new MemoryDataInput(0);
    protected final StaticEncoder                       dlEncoder; // encoder for ByteLossMessages
    
    private final byte[]                                empty = new byte[0];
    private final DataLossMessage dlm;

    static class StaticEncoder {
        final MemoryDataInput                     in;
        final MemoryDataOutput                    out;
        final MessageEncoder<InstrumentMessage>   encoder; // encoder for ByteLossMessages

        StaticEncoder(MessageEncoder<InstrumentMessage> encoder, MemoryDataInput input) {
            this.encoder = encoder;
            this.out = new MemoryDataOutput(64);
            this.in = input;
        }

        void encode(InstrumentMessage msg) {
            out.reset();
            encoder.encode(msg, out);
            in.setBytes(out);
        }
    }

    LossyMessageQueueReader (LossyMessageQueue mq,
                             MessageEncoder<InstrumentMessage> dlEncoder) {
        super (mq);

        this.dlEncoder = dlEncoder != null ? new StaticEncoder(dlEncoder, input) : null;
        if (dlEncoder != null) {
            this.dlm = new DataLossMessage();
            //this.dlm.setInstrumentType(InstrumentType.STREAM);
            this.dlm.setSymbol(mq.getStream().getName());
        } else {
            this.dlm = null;
        }
    }

    @Override
    protected void              invalidateBuffer() {
        long available = available();
        if (available == buffer.length) // whole message size > buffer size
            buffer = new byte[buffer.length * 2];

        bufferFileOffset = currentFileOffset.addAndGet(-available);
        bufferPosition = 0;
    }

    @Override
    public boolean              read () {
        long        prevTime = time.timestamp;

        boolean advance = super.read();

        if (dlEncoder != null) {
            if (bytesLost != 0) {
                advance = true; // make sure that consumer get data loss messages
                
                dlm.setBytes(bytesLost);
                dlm.setFromTime(prevTime);
                dlm.setNanoTime(time.getNanoTime());

                dlEncoder.encode(dlm);
                bytesLost = 0;
            } else if (input.hasAvail()) {
                input.setBytes(empty, 0, 0); // reset input
            }
        }

        return (advance);
    }

    void                        onBytesLost(long bytes) {
        bufferFileOffset += bytes;
        bytesLost = bytes;
    }

    @Override
    public MemoryDataInput getInput() {
        if (input.hasAvail())
            return input;
        
        return super.getInput();
    }

    @Override
    public void                 close () {
        mfile.rawReaderClosed (this);
    }
}