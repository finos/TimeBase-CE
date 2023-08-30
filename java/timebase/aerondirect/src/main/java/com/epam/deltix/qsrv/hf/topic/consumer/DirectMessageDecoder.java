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
    private static final Log LOG = LogFactory.getLog(DirectMessageDecoder.class.getName());

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
    private final ObjectArrayList<ConstantIdentityKey> entities = new ObjectArrayList<>();
    private final IntegerToObjectHashMap<ConstantIdentityKey> tempEntities = new IntegerToObjectHashMap<>();

    // Aeron sessionId that represent gracefully closed publications.
    private final IntegerArrayList finishedSessions = new IntegerArrayList();
    private final MappingProvider mappingProvider;

    // If true then only metadata processed (all data messages are skipped). This mode is used during the initialization process.
    // private boolean skipDataMode = false;

    /**
     * @param arrayBuffer array buffer that works as source of data. It's supposed to be shared with holding class
     * @param mappingProvider
     */
    DirectMessageDecoder(DirectBuffer arrayBuffer, boolean raw, CodecFactory codecFactory, TypeLoader typeLoader,
                         List<RecordClassDescriptor> types, ConstantIdentityKey[] mapping, MappingProvider mappingProvider) {
        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }
        this.arrayBuffer = arrayBuffer;

        // Decoders
        this.raw = raw;
        this.codecFactory = codecFactory;
        this.typeLoader = typeLoader;
        this.mappingProvider = mappingProvider;

        if (this.raw) {
            this.rawMessage = new RawMessage();
            this.decoders = null;
        } else {
            this.rawMessage = null;
            this.decoders = new ObjectArrayList<>();
        }
        this.mdi = new MemoryDataInput(arrayBuffer.byteArray(), 0, 0);

        initTypes(types);
        initEntities(mapping);
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

            case DirectProtocol.CODE_METADATA:
                processMetadata(length);
                return null;

            case DirectProtocol.CODE_TEMP_INDEX_REMOVED:
                processIndexRemoved(length);
                return null;

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
        int typeIndex = arrayBuffer.getByte(TYPE_OFFSET);
        int entityIndex = arrayBuffer.getInt(ENTITY_OFFSET);
        long nanoTime = arrayBuffer.getLong(TIME_OFFSET);
        int dataLength = messageLength - DATA_OFFSET;

        ConstantIdentityKey entity = getEntityByEntityIndex(entityIndex);

        InstrumentMessage curMsg;
        if (raw) {
            rawMessage.type = getTypeByTypeIndex(typeIndex);
            rawMessage.setBytes(arrayBuffer.byteArray(), arrayBuffer.wrapAdjustment() + DATA_OFFSET, dataLength);
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
                decoder = codecFactory.createFixedBoundDecoder(typeLoader, getTypeByTypeIndex(typeIndex));
                decoders.set(typeIndex, decoder);
            }

            assert decoder != null;

            mdi.setBytes(arrayBuffer.byteArray(), arrayBuffer.wrapAdjustment() + DATA_OFFSET, dataLength);
            curMsg = (InstrumentMessage) decoder.decode(mdi);
        }

        curMsg.setNanoTime(nanoTime);
        curMsg.setSymbol(entity.symbol);

        return curMsg;
    }

    private void processMetadata(int length) {
        mdi.setBytes(arrayBuffer.byteArray(), arrayBuffer.wrapAdjustment() + METADATA_OFFSET, length - METADATA_OFFSET);
        int recordCount = mdi.readInt();
        for (int i = 0; i < recordCount; i++) {
            int entityIndex = mdi.readInt();
            String symbol = mdi.readCharSequence().toString().intern();

            ConstantIdentityKey key = new ConstantIdentityKey(symbol);
            addEntity(entityIndex, key);
        }
    }

    private void processIndexRemoved(int length) {
        mdi.setBytes(arrayBuffer.byteArray(), arrayBuffer.wrapAdjustment() + TEMP_INDEX_REMOVED_DATA_OFFSET, length - TEMP_INDEX_REMOVED_DATA_OFFSET);
        int recordCount = mdi.readInt();
        for (int i = 0; i < recordCount; i++) {
            int entityIndex = mdi.readInt();
            tempEntities.remove(entityIndex);
        }
    }

    private void processEndOfStream(int length) {
        mdi.setBytes(arrayBuffer.byteArray(), arrayBuffer.wrapAdjustment() + END_OF_STREAM_DATA_OFFSET, length - END_OF_STREAM_DATA_OFFSET);
        int sessionId = mdi.readInt();

        synchronized (finishedSessions) {
            finishedSessions.add(sessionId);
        }
    }

    private void addEntity(int entityIndex, ConstantIdentityKey key) {
        if (DirectProtocol.isTempIndex(entityIndex)) {
            // This is temp index

            //int pos = -entityIndex;
            //int prodNum = pos >> 24;
            //int indx = pos & 0xFFFFFF;
            tempEntities.put(entityIndex, key);
        } else {
            // Permanent index
            if (entityIndex >= entities.size()) {
                entities.setSize(entityIndex + 1);
            }
            entities.set(entityIndex, key);
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

    private ConstantIdentityKey getEntityByEntityIndex(int entityIndex) {
        if (DirectProtocol.isTempIndex(entityIndex)) {
            ConstantIdentityKey instrumentKey = tempEntities.get(entityIndex, null);
            if (instrumentKey == null) {
                // This means we missed temp key during registration
                return handleMissingTempIndex(entityIndex);
            }
            return instrumentKey;
        }
        if (entityIndex < entities.size()) {
            return entities.get(entityIndex);
        } else {
            // No mapping for that entity index
            return missingEntityIndex(entityIndex);
        }
    }

    private ConstantIdentityKey handleMissingTempIndex(int entityIndex) {
        assert DirectProtocol.isTempIndex(entityIndex);

        // Warning: this is blocking operation
        IntegerToObjectHashMap<ConstantIdentityKey> tempMappingSnapshot = mappingProvider.getTempMappingSnapshot(entityIndex);

        LOG.debug("Got temp index snapshot with %s records").with(tempMappingSnapshot.size());
        addTempMapping(tempMappingSnapshot);

        ConstantIdentityKey instrumentKey = tempEntities.get(entityIndex, null);
        if (instrumentKey != null) {
            // We successfully loaded needed entry
            return instrumentKey;
        } else {
            // "Failed to obtain data for entry with temp index"
            LOG.warn("Topic consumer closed due to failure to get temp index mapping");
            throw new ClosedDueToDataLossException();
        }
    }

    private void addTempMapping(IntegerToObjectHashMap<ConstantIdentityKey> tempMappingSnapshot) {
        ElementsEnumeration<ConstantIdentityKey> elements = tempMappingSnapshot.elements();
        IntegerEntry entry = (IntegerEntry) elements;
        while (elements.hasMoreElements()) {
            int key = entry.keyInteger();
            ConstantIdentityKey value = elements.nextElement();
            if (!DirectProtocol.isValidTempIndex(key)) {
                throw new IllegalArgumentException("Invalid temp index: " + key);
            }
            addEntity(key, value);
        }
    }

    private ConstantIdentityKey missingEntityIndex(int entityIndex) {
        // TODO: Handle the case when a new entity id is added just after the consumer registration (but before consumer can start to listen data). In this case consumer may miss mapping data.
        // This case is rare and may happen only during short period just after the start but we still have to address it.
        throw new IllegalStateException("Unexpected entityIndex: " + entityIndex);
    }

    private RecordClassDescriptor getTypeByTypeIndex(int typeIndex) {
        return types.getConcreteTypeByIndex(typeIndex);
    }

    private void initTypes(List<RecordClassDescriptor> types) {
        for (int i = 0; i < types.size(); i++) {
            addType(i, types.get(i));
        }
    }

    /**
     * Initializes or updates entity mapping.
     *
     * @param mapping arrays of instrument keys, each key have same mapping index as position in that array
     */
    private void initEntities(ConstantIdentityKey[] mapping) {
        if (mapping.length >= entities.size()) {
            entities.setSize(mapping.length);
        }
        for (int i = 0; i < mapping.length; i++) {
            addEntity(i, mapping[i]);
        }
    }
}