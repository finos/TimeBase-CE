/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TypeSet;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.util.collections.ElementsEnumeration;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.IntegerEntry;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.memory.MemoryDataInput;
import org.agrona.DirectBuffer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;

import static com.epam.deltix.qsrv.hf.topic.DirectProtocol.*;

/**
 * @author Alexei Osipov
 */
class DirectMessageDecoder {

    private final DirectBuffer arrayBuffer; // Contains only current message

    // Message decoding
    private final boolean raw;
    private final ObjectArrayList<BoundDecoder> decoders;
    private final RawMessage rawMessage;
    private final MemoryDataInput mdi;

    private final CodecFactory codecFactory;
    private final TypeLoader typeLoader;

    // Index mapping
    private final TypeSet types = new TypeSet(null);
    private final StringBuilder symbolSb = new StringBuilder(); // Flyweight for symbol

    // Aeron sessionId that represent gracefully closed publications.
    private final IntegerArrayList finishedSessions = new IntegerArrayList();

    // If true then only metadata processed (all data messages are skipped). This mode is used during the initialization process.
    // private boolean skipDataMode = false;

    /**
     * @param arrayBuffer array buffer that works as source of data. It's supposed to be shared with holding class
     */
    DirectMessageDecoder(DirectBuffer arrayBuffer, boolean raw, CodecFactory codecFactory, TypeLoader typeLoader,
                         List<RecordClassDescriptor> types) {
        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }
        this.arrayBuffer = arrayBuffer;

        // Decoders
        this.raw = raw;
        this.codecFactory = codecFactory;
        this.typeLoader = typeLoader;

        if (this.raw) {
            this.rawMessage = new RawMessage();
            this.decoders = null;
        } else {
            this.rawMessage = null;
            this.decoders = new ObjectArrayList<>();
        }
        this.mdi = new MemoryDataInput(arrayBuffer.byteArray(), 0, 0);

        initTypes(types);
    }

    /**
     * Processes single message from buffer.
     * It that's a data message then returns message value.
     * If that's a metadata message then returns {@code null}.
     */
    @Nullable
    InstrumentMessage processSingleMessageFromBuffer(int length) {
        byte code = arrayBuffer.getByte(CODE_OFFSET);

        switch (code) {
            case DirectProtocol.CODE_MSG:
                return processMessage(length);

            case DirectProtocol.CODE_END_OF_STREAM:
                processEndOfStream(length);
                return null;

            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }

    /*
    void setSkipDataMode(boolean skipDataMode) {
        this.skipDataMode = skipDataMode;
    }
    */

    /**
     * Checks if we got info from publisher that session is explicitly closed.
     * After the check data on that session gets removed.
     * So if you call that method two times with same sessionId then the second call will always return {@code false}.
     *
     * @param sessionId aeron publication sessionId
     * @return true if publisher properly closed that session
     */
    boolean checkIfSessionGracefullyClosed(int sessionId) {
        synchronized (finishedSessions) {
            return removeValueFromSet(finishedSessions, sessionId);
        }
    }

    /**
     * Removes value from list by moving list end to deleted position.
     *
     * @param list list to modify
     * @param searchValue value to search
     * @return true if value was found and removed
     */
    private static boolean removeValueFromSet(IntegerArrayList list, int searchValue) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            int currentValue = list.getIntegerNoRangeCheck(i);
            if (currentValue == searchValue) {
                if (i < size - 1) {
                    // Move last value to current position
                    list.set(i, list.get(size - 1));
                }
                list.setSize(size - 1);
                return true;
            }
        }
        return false;
    }

    private InstrumentMessage processMessage(int messageLength) {
        /*
        if (skipDataMode) {
            return null;
        }
        */
        assert messageLength >= DATA_MSG_MIN_HEAD_SIZE;
        long nanoTime = arrayBuffer.getLong(TIME_OFFSET);
        int typeIndex = arrayBuffer.getByte(TYPE_OFFSET);
        //int instrumentIndex = arrayBuffer.getByte(INSTRUMENT_OFFSET);
        int symbolAndDataLength = messageLength - SYMBOL_OFFSET;


        int variableLengthDataOffset = arrayBuffer.wrapAdjustment() + SYMBOL_OFFSET;
        mdi.setBytes(arrayBuffer.byteArray(), variableLengthDataOffset, symbolAndDataLength);
        @Nullable
        StringBuilder symbolValue = mdi.readStringBuilder(symbolSb);
        RecordClassDescriptor type = getTypeByTypeIndex(typeIndex);

        InstrumentMessage curMsg;
        if (raw) {
            int dataPosition = mdi.getPosition(); // Effectively contains number of bytes taken for "symbol" field
            int dataLength = symbolAndDataLength - dataPosition;

            rawMessage.type = type;
            rawMessage.setBytes(arrayBuffer.byteArray(), variableLengthDataOffset + dataPosition, dataLength);
            curMsg = rawMessage;
        } else {
            BoundDecoder decoder;

            if (typeIndex >= decoders.size()) {
                decoders.setSize(typeIndex + 1);
                decoder = null;
            } else {
                decoder = decoders.getObjectNoRangeCheck(typeIndex);
            }

            if (decoder == null) {
                decoder = codecFactory.createFixedBoundDecoder(typeLoader, type);
                decoders.set(typeIndex, decoder);
            }

            assert decoder != null;

            curMsg = (InstrumentMessage) decoder.decode(mdi);
        }

        curMsg.setNanoTime(nanoTime);
        //curMsg.setInstrumentType(instrumentType);
        curMsg.setSymbol(symbolValue);

        return curMsg;
    }

    private void processEndOfStream(int length) {
        assert length == END_OF_STREAM_MSG_SIZE;

        int sessionId = arrayBuffer.getInt(SESSION_ID_OFFSET);

        synchronized (finishedSessions) {
            finishedSessions.add(sessionId);
        }
    }


    private void addType(int typeIndex, RecordClassDescriptor type) {
        try {
            types.addType(typeIndex, type);
        } catch (IOException e) {
            // TODO: Get rid of exception
            throw new UncheckedIOException(e);
        }
    }

    private RecordClassDescriptor getTypeByTypeIndex(int typeIndex) {
        return types.getConcreteTypeByIndex(typeIndex);
    }

    private void initTypes(List<RecordClassDescriptor> types) {
        for (int i = 0; i < types.size(); i++) {
            addType(i, types.get(i));
        }
    }
}