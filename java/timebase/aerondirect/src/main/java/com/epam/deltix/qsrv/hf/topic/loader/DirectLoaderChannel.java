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
package com.epam.deltix.qsrv.hf.topic.loader;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.blocks.InstrumentKeyToIntegerHashMap;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.io.aeron.AeronPublicationMDOAdapter;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.adapter.IdleStrategyAdapter;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteOrder;
import java.util.List;

import static com.epam.deltix.qsrv.hf.topic.DirectProtocol.*;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class DirectLoaderChannel implements MessageChannel<InstrumentMessage>, FragmentHandler {
    private static final Log LOG = LogFactory.getLog(DirectLoaderChannel.class.getName());

    private static final int NOT_FOUND_VALUE = -1;
    private final RecordTypeMap<Class> typeMap;
    private final RecordTypeMap<RecordClassDescriptor> rawTypeMap;

    private final FixedBoundEncoder[] encoders;

    private final AeronPublicationMDOAdapter publicationAdapter;
    private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[0]);

    private final InstrumentKeyToIntegerHashMap entities;
    private final FragmentAssembler fragmentAssembler;
    private int nextTempEntityIndex;

    private final boolean raw;
    private final MessageChannel<MemoryDataOutput> serverPublicationChannel;

    private final Subscription serverMetadataUpdates;
    private final ExpandableArrayBuffer arrayBuffer = new ExpandableArrayBuffer(); // Contains only current message
    private final MemoryDataInput mdi;

    private final Runnable closeCallback;

    private final int presetEntityCount; // Number of predefined entities (provided from mapping)

    /**
     * An array of bit flags to check if an entity with specific index was sent to consumers at least once by this publisher.
     *
     * This works just like {@link java.util.BitSet}.
     */
    private final long[] unsentEntities; // TODO: Instead of tracking of entities on one-by-one basis we can send all of them at once at registration time.


    DirectLoaderChannel(ExclusivePublication publication, CodecFactory factory, boolean raw, TypeLoader typeLoader, int firstTempEntityIndex, MessageChannel<MemoryDataOutput> serverPublicationChannel, Subscription serverMetadataUpdates, RecordClassDescriptor[] types, List<ConstantIdentityKey> mapping, @Nullable Runnable closeCallback, IdleStrategy publicationIdleStrategy) {
        this.raw = raw;
        this.serverPublicationChannel = serverPublicationChannel;
        this.serverMetadataUpdates = serverMetadataUpdates;
        this.closeCallback = closeCallback;
        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }

        this.publicationAdapter = new AeronPublicationMDOAdapter(publication, IdleStrategyAdapter.adapt(publicationIdleStrategy));

        if (!raw) {
            Class<?>[] classes = new Class<?>[types.length];
            this.encoders = new FixedBoundEncoder[types.length];
            for (int i = 0; i < types.length; i++) {
                FixedBoundEncoder encoder = factory.createFixedBoundEncoder(typeLoader, types[i]);
                this.encoders[i] = encoder;
                classes[i] = encoder.getClassInfo().getTargetClass();
            }
            this.typeMap = new RecordTypeMap<>(classes);
            this.rawTypeMap = null;
        } else {
            this.encoders = null;
            this.typeMap = null;
            this.rawTypeMap = new RecordTypeMap<>(types);
        }


        this.nextTempEntityIndex = firstTempEntityIndex;

        this.fragmentAssembler = new FragmentAssembler(this);
        this.mdi = new MemoryDataInput(arrayBuffer.byteArray());

        this.presetEntityCount = mapping.size();
        this.entities = new InstrumentKeyToIntegerHashMap(Math.max(16, BitUtil.nextPowerOfTwo(presetEntityCount)));
        this.unsentEntities = new long[bitIndexToWordIndex(this.presetEntityCount - 1) + 1];
        for (int i = 0; i < presetEntityCount; i++) {
            ConstantIdentityKey key = mapping.get(i);
            entities.put(key, i);
        }
    }


    @Override
    public void send(InstrumentMessage msg) {
        /*
        if (newDataFlag.getAndSet(false)) {
            checkForMappingUpdates();
        }
        */
        RawMessage rawMsg;
        int typeIndex;
        if (this.raw) {
            rawMsg = (RawMessage) msg;
            typeIndex = rawTypeMap.getCode(rawMsg.type);
        } else {
            rawMsg = null;
            typeIndex = typeMap.getCode(msg.getClass());
        }

        CharSequence symbol = msg.getSymbol();
        int entityIndex = determineEntityIndex(symbol); // Note: we may send additional message here


        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        // Ensure space
        mdo.ensureSize(DirectProtocol.REQUIRED_HEADER_SIZE);

        // Write common header
        buffer.wrap(mdo.getBuffer(), 0, DirectProtocol.REQUIRED_HEADER_SIZE);
        buffer.putByte(DirectProtocol.CODE_OFFSET, DirectProtocol.CODE_MSG);
        buffer.putByte(DirectProtocol.TYPE_OFFSET, (byte) typeIndex);
        buffer.putInt(DirectProtocol.ENTITY_OFFSET, entityIndex);
        long nanoTime = msg.getNanoTime();
        if (nanoTime == TimeStampedMessage.TIMESTAMP_UNKNOWN) {
            nanoTime = TimeKeeper.currentTimeNanos;
        }

        buffer.putLong(DirectProtocol.TIME_OFFSET, nanoTime);

        // Write message body
        mdo.seek(DirectProtocol.DATA_OFFSET);
        if (raw) {
            rawMsg.writeTo(mdo);
        } else {
            encoders[typeIndex].encode(msg, mdo);
        }

        publicationAdapter.sendBufferIfConnected();
    }

    private boolean checkForMappingUpdates() {
        return serverMetadataUpdates.poll(this.fragmentAssembler, Integer.MAX_VALUE) > 0;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        // Handles metadata from server
        buffer.getBytes(offset, arrayBuffer, 0, length);
        byte code = arrayBuffer.getByte(CODE_OFFSET);

        switch (code) {
            case DirectProtocol.CODE_METADATA:
                processMetadataFromServer(length);
                break;

            default:
                throw new IllegalArgumentException("Unknown code");
        }
    }

    private void processMetadataFromServer(int length) {
        mdi.setBytes(arrayBuffer.byteArray(), METADATA_OFFSET, length - METADATA_OFFSET);
        int recordCount = mdi.readInt();
        for (int i = 0; i < recordCount; i++) {
            int entityIndex = mdi.readInt();
            String symbol = mdi.readCharSequence().toString().intern();

            ConstantIdentityKey key = new ConstantIdentityKey(symbol);
            updateEntityMappingFromServer(key, entityIndex);
        }
    }

    private void updateEntityMappingFromServer(ConstantIdentityKey key, int entityIndex) {
        assert entityIndex >= 0;
        int prevValue = entities.putAndGet(key, entityIndex, NOT_FOUND_VALUE);
        if (entityIndex != prevValue) {
            // We got new mapping => we must send it to clients
            sendMappingMetadata(key.getSymbol(), entityIndex, false);
            if (prevValue != NOT_FOUND_VALUE) {
                sendTempIndexRemoved(prevValue);
            }
        }
    }

    private int determineEntityIndex(CharSequence symbol) {
        int entityIndex = entities.get(symbol, NOT_FOUND_VALUE);
        if (entityIndex < 0) {
            // Not found OR temp value
            assert DirectProtocol.isValidTempIndex(entityIndex) || entityIndex == NOT_FOUND_VALUE;

            if (checkForMappingUpdates()) {
                // We got updates
                entityIndex = entities.get(symbol, NOT_FOUND_VALUE);
            }
        } else {
            // Already known entity
            if (entityIndex < presetEntityCount && !isSent(entityIndex)) {
                // This loader never sent this specific entity before. Send it now.
                // We have to do this because there is no guarantee that other publishers sent this entity.
                sendMappingMetadata(symbol, entityIndex, false);
                setSent(entityIndex);
            }
        }
        if (entityIndex == NOT_FOUND_VALUE) {
            // This is unknown combination.
            // Generate temp value and use it
            entityIndex = generateAndSendTempIndex(symbol);
        }
        return entityIndex;
    }

    private int generateAndSendTempIndex(CharSequence symbol) {
        int entityIndex;
        entityIndex = getTempIndex();
        assert isValidTempIndex(entityIndex);
        entities.put(new ConstantIdentityKey(symbol), entityIndex);
        sendMappingMetadata(symbol, entityIndex, true);
        return entityIndex;
    }

    private int getTempIndex() {
        int result = this.nextTempEntityIndex;
        nextTempEntityIndex --; // Note: we decrease value because its negative and we want to increase absolute value
        // Bounds check. We must insure that new value is withing min/max bounds
        if (DirectProtocol.getPublisherNumberFromTempIndex(result) != DirectProtocol.getPublisherNumberFromTempIndex(nextTempEntityIndex)) {
            throw new IllegalStateException("Temporary value is out of range dedicated for current producer (too many temp values for producer)");
        }
        return result;
    }

    private void sendMappingMetadata(CharSequence symbol, int entityIndex, boolean sendToServer) {
        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        DirectTopicLoaderCodec.writeSingleEntryInstrumentMetadata(mdo, symbol, entityIndex);

        if (sendToServer) {
            serverPublicationChannel.send(mdo); // Send to server so it would give us permanent id
            LOG.debug().append("Sent index to server: ").appendLast(entityIndex);
        }
        boolean sent = publicationAdapter.sendBufferIfConnected();// Send to data channel so clients could use that new temp id
        LOG.debug("Sent index to client: %s %s").with(entityIndex).with(sent);
    }

    private void sendTempIndexRemoved(int tempIndex) {
        assert isValidTempIndex(tempIndex);
        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        mdo.writeByte(DirectProtocol.CODE_TEMP_INDEX_REMOVED);
        mdo.writeInt(1); // Record count
        mdo.writeInt(tempIndex);
        serverPublicationChannel.send(mdo); // Send to server so it would give us permanent id
        publicationAdapter.sendBufferIfConnected(); // Send to data channel so clients could use that new temp id
        LOG.debug().append("Temp index removed: ").appendLast(tempIndex);
    }

    private void sendEndOfStream() {
        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        mdo.writeByte(DirectProtocol.CODE_END_OF_STREAM);
        mdo.writeInt(publicationAdapter.getAeronSessionId());

        serverPublicationChannel.send(mdo); // Notify server about graceful shutdown of that publisher
        publicationAdapter.sendBufferIfConnected(); // Send to data channel so clients could use that new temp id
        LOG.debug("Sent EOS for session %s").with(publicationAdapter.getAeronSessionId());
    }


    /**
     * See {@link java.util.BitSet#wordIndex(int)}
     */
    private static int bitIndexToWordIndex(int bitIndex) {
        // Returns -1 for -1
        return bitIndex >> 6; // Divide by 64
    }

    /**
     * @return true if metadata for this entityIndex was sent.
     */
    private boolean isSent(int entityIndex) {
        int wordIndex = bitIndexToWordIndex(entityIndex);
        return (unsentEntities[wordIndex] & 1L << entityIndex) != 0;
    }

    /**
     * Mark metadata for this entityIndex as sent.
     */
    private void setSent(int entityIndex) {
        int wordIndex = bitIndexToWordIndex(entityIndex);
        unsentEntities[wordIndex] |= (1L << entityIndex);
    }

    @Override
    public synchronized void close() {
        if (!publicationAdapter.isClosed()) {
            sendEndOfStream();
        }
        publicationAdapter.close();
        if (closeCallback != null) {
            closeCallback.run();
        }
    }
}
