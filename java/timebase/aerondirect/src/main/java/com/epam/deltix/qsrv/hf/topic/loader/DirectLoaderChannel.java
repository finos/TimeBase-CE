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
package com.epam.deltix.qsrv.hf.topic.loader;

import com.epam.deltix.qsrv.hf.pub.TimeSource;
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

import static com.epam.deltix.qsrv.hf.topic.DirectProtocol.SYMBOL_OFFSET;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class DirectLoaderChannel implements MessageChannel<InstrumentMessage> {
    private static final Log LOG = LogFactory.getLog(DirectLoaderChannel.class.getName());

    private final RecordTypeMap<Class<?>> typeMap;
    private final RecordTypeMap<RecordClassDescriptor> rawTypeMap;

    private final FixedBoundEncoder[] encoders;

    private final AeronPublicationMDOAdapter publicationAdapter;
    private final UnsafeBuffer buffer = new UnsafeBuffer(new byte[0]);

    private final boolean raw;

    private final Runnable closeCallback;
    private final boolean addTimestamp;
    private final TimeSource timeSource;

    DirectLoaderChannel(ExclusivePublication publication, CodecFactory factory, boolean raw, TypeLoader typeLoader,
                        RecordClassDescriptor[] types, @Nullable Runnable closeCallback, IdleStrategy publicationIdleStrategy,
                        TimeSource timeSource, boolean preserveNullTimestamp) {
        this.raw = raw;
        this.closeCallback = closeCallback;
        this.addTimestamp = !preserveNullTimestamp;
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
        this.timeSource = timeSource;
    }


    @Override
    public void send(InstrumentMessage msg) {
        RawMessage rawMsg;
        int typeIndex;
        if (this.raw) {
            rawMsg = (RawMessage) msg;
            typeIndex = rawTypeMap.getCode(rawMsg.type);
        } else {
            rawMsg = null;
            typeIndex = typeMap.getCode(msg.getClass());
        }

        //InstrumentType instrumentType = msg.getInstrumentType();
        CharSequence symbol = msg.getSymbol();

        long nanoTime = msg.getNanoTime();
        if (addTimestamp && nanoTime == TimeStampedMessage.TIMESTAMP_UNKNOWN) {
            // For topics API we must set timestamp on the sender side. There is no "server" time for topics.
            nanoTime = timeSource.currentTimeNanos();
        }


        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        // Ensure space
        int maxSymbolBytes = symbol.length() * 2 + Short.BYTES;

        // WARNING! This not just ensures that we have enough capacity but also increases SIZE value despite this is NOT needed.
        // However, we do not use "size" field later, so this is not a problem.
        mdo.ensureSize(DirectProtocol.DATA_MSG_MIN_HEAD_SIZE + maxSymbolBytes);

        // Write common header
        buffer.wrap(mdo.getBuffer(), 0, DirectProtocol.DATA_MSG_MIN_HEAD_SIZE);
        //buffer.verifyAlignment();
        buffer.putLong(DirectProtocol.TIME_OFFSET, nanoTime);
        buffer.putByte(DirectProtocol.CODE_OFFSET, DirectProtocol.CODE_MSG);
        buffer.putByte(DirectProtocol.TYPE_OFFSET, (byte) typeIndex);
        //buffer.putByte(DirectProtocol.INSTRUMENT_OFFSET, (byte) instrumentType.getNumber());

        // Write symbol
        mdo.seek(SYMBOL_OFFSET);
        mdo.writeStringNonNull(symbol);
        // Write message body
        if (raw) {
            rawMsg.writeTo(mdo);
        } else {
            encoders[typeIndex].encode(msg, mdo);
        }

        publicationAdapter.sendBufferIfConnected();
    }

    private void sendEndOfStream() {
        MemoryDataOutput mdo = publicationAdapter.getMemoryDataOutput();
        mdo.ensureSize(DirectProtocol.END_OF_STREAM_MSG_SIZE);
        buffer.wrap(mdo.getBuffer(), 0, DirectProtocol.END_OF_STREAM_MSG_SIZE);

        buffer.putLong(DirectProtocol.TIME_OFFSET, 0); // Clear the message time
        buffer.putByte(DirectProtocol.CODE_OFFSET, DirectProtocol.CODE_END_OF_STREAM);
        buffer.putInt(DirectProtocol.SESSION_ID_OFFSET, publicationAdapter.getAeronSessionId());

        mdo.seek(DirectProtocol.END_OF_STREAM_MSG_SIZE); // This is needed to tell the api the length of data to send

        publicationAdapter.sendBufferIfConnected(); // Send to data channel so clients could use that new temp id
        LOG.debug("Sent EOS for session %s").with(publicationAdapter.getAeronSessionId());
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