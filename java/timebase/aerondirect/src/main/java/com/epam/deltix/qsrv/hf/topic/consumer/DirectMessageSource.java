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
package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AeronClientThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AnyThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.ReaderThreadOnly;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import io.aeron.Aeron;
import io.aeron.ControlledFragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteOrder;
import java.util.List;

import static org.agrona.BitUtil.align;
import static org.agrona.BitUtil.isAligned;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class DirectMessageSource implements MessageSource<InstrumentMessage>, IntermittentlyAvailableCursor {
    private static final int MESSAGES_PER_POLL = 100;

    // Alignment of messages in the message buffer.
    // We align size heater to ints (4) and body to longs (8),
    // We align to long to make sure that we can read long value without alignment issues
    private static final int HEADER_ALIGNMENT = Integer.BYTES; // 4
    private static final int BODY_ALIGNMENT = Long.BYTES; // 8

    private static final byte SIZE_HEADER_SIZE = Integer.BYTES;
    private static final int INITIAL_BUFFER_SIZE = BitUtil.nextPowerOfTwo(128 * 1024); // 128kb

    private InstrumentMessage curMsg;

    // Contains multiple messages. Each message is 4 byte size header and binary body/
    private final UnsafeBuffer messageBuffer;
    private int bufferLimit = 0;
    private int bufferPos = 0;

    private final Subscription subscription;
    private final IdleStrategy idleStrategy;

    private volatile boolean closed = false;


    private final DirectBuffer arrayBuffer; // Contains only current message

    private final ControlledFragmentAssembler fragmentHandler;

    private final DirectMessageDecoder decoder;

    // Indicates that poller detected a data loss before graceful stop
    private volatile boolean dataLoss = false;

    DirectMessageSource(Aeron aeron, boolean raw, String channel, int dataStreamId, CodecFactory codecFactory,
                        TypeLoader typeLoader, List<RecordClassDescriptor> types, IdleStrategy idleStrategy,
                        MappingProvider mappingProvider) {
        // TODO: Implement loading of temp indexes from server

        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }

        this.subscription = aeron.addSubscription(channel, dataStreamId, null, this::onUnavailableImage);
        this.messageBuffer = new UnsafeBuffer(new byte[INITIAL_BUFFER_SIZE]);
        this.arrayBuffer = new UnsafeBuffer(this.messageBuffer.byteArray(), 0, 0);
        this.idleStrategy = idleStrategy;

        this.fragmentHandler = new ControlledFragmentAssembler(new FragmentHandler());
        // We must get mapping after we completed subscription
        ConstantIdentityKey[] mappingSnapshot = mappingProvider.getMappingSnapshot();

        this.decoder = new DirectMessageDecoder(arrayBuffer, raw, codecFactory, typeLoader, types, mappingSnapshot, mappingProvider);
    }

    @ReaderThreadOnly
    @Override
    public InstrumentMessage getMessage() {
        return curMsg;
    }

    /**
     * Note: this implementation will never return false.
     *
     * @throws CursorIsClosedException if this message source was closed
     */
    @ReaderThreadOnly
    @Override
    public boolean next() throws CursorIsClosedException {
        assertNotClosed();
        while (true) {
            while (!hasBufferedMessages()) {
                if (!pageDataIn()) {
                    idleStrategy.idle();
                    // We must re-check closed status because it might have changed during the idle period.
                    assertNotClosed();
                } else {
                    idleStrategy.reset();
                }
            }
            if (setupMessageFromBuffer()) {
                // We got data message
                return true;
            }
        }
    }

    @ReaderThreadOnly
    @Override
    public NextResult nextIfAvailable() {
        assertNotClosed();
        while (true) {
            if (!hasBufferedMessages()) {
                if (!pageDataIn()) {
                    return NextResult.UNAVAILABLE;
                }
            }
            if (setupMessageFromBuffer()) {
                return NextResult.OK;
            }
        }
    }

    private void assertNotClosed() {
        if (subscription.isClosed()) {
            if (dataLoss) {
                throw new ClosedDueToDataLossException();
            } else {
                throw new CursorIsClosedException();
            }
        }
    }

    /**
     * @return true if it was able to load at least one message.
     */
    @ReaderThreadOnly
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean pageDataIn() {
        assert remainingData() == 0;

        // Buffer is empty -> reset positions
        bufferPos = 0;
        bufferLimit = 0;

        return subscription.controlledPoll(fragmentHandler, MESSAGES_PER_POLL) > 0;
    }

    /**
     * Populates "curMsg" from buffer.
     */
    private boolean setupMessageFromBuffer() {
        assert hasBufferedMessages();

        assert isAligned(bufferPos, HEADER_ALIGNMENT);
        int messageSize = messageBuffer.getInt(bufferPos);
        bufferPos = align(bufferPos + SIZE_HEADER_SIZE, BODY_ALIGNMENT);

        assert isAligned(bufferPos, BODY_ALIGNMENT);
        arrayBuffer.wrap(messageBuffer.byteArray(), bufferPos, messageSize);
        InstrumentMessage result = decoder.processSingleMessageFromBuffer(messageSize);

        bufferPos = align(bufferPos + messageSize, HEADER_ALIGNMENT);

        if (result != null) {
            curMsg = result;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isAtEnd() {
        // This is live data stream. It will never end.
        return false;
    }

    private boolean hasBufferedMessages() {
        return bufferLimit > bufferPos;
    }

    private int remainingData() {
        return bufferLimit - bufferPos;
    }

    private int remainingSpace() {
        return messageBuffer.capacity() - bufferLimit;
    }


    private void cleanup() {
        subscription.close();
    }

    // Can be executed from different thread. So it can be used to stop message processing executed by one thread from another.
    @AnyThread
    public synchronized void close() {
        if (!closed) {
            cleanup();
            closed = true;
        }
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onUnavailableImage(Image image) {
        int sessionId = image.sessionId();
        // Note: decoder can be null during the initialization process
        if (!subscription.isClosed() && decoder != null && !decoder.checkIfSessionGracefullyClosed(sessionId)) {
            // Not a graceful close
            onDataLossDetected();
        }
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onDataLossDetected() {
        dataLoss = true;
        subscription.close();
    }

    private class FragmentHandler implements ControlledFragmentHandler {
        @Override
        public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
            //assert remainingSpace() >= 0;

            int expectedDataPosition = align(bufferLimit + SIZE_HEADER_SIZE, BODY_ALIGNMENT);
            int positionAfterAddition = align(expectedDataPosition + length, HEADER_ALIGNMENT);
            int spaceNeeded = positionAfterAddition - bufferLimit;

            if (remainingSpace() >= spaceNeeded) {
                copyMessageToBuffer(buffer, offset, length);
                return Action.CONTINUE;
            } else {
                // We are out of space
                if (hasBufferedMessages()) {
                    // We already have some buffered messages so we can abort loading current message and come back to it later
                    return Action.ABORT;
                } else {
                    // Seems like message does not fit the buffer. So we need to expand.
                    assert bufferPos == 0 && bufferLimit == 0 : "Buffer expected to be empty";
                    messageBuffer.wrap(new byte[BitUtil.nextPowerOfTwo(spaceNeeded)]);
                    copyMessageToBuffer(buffer, offset, length);
                    return Action.BREAK;
                }
            }
        }
    }

    private void copyMessageToBuffer(DirectBuffer buffer, int offset, int length) {
        writeSizeHeaderToBuffer(length);
        loadDataToBuffer(buffer, offset, length);
    }

    private void writeSizeHeaderToBuffer(int length) {
        assert isAligned(bufferLimit, HEADER_ALIGNMENT);
        messageBuffer.putInt(bufferLimit, length);
        bufferLimit = align(bufferLimit + SIZE_HEADER_SIZE, BODY_ALIGNMENT);
    }

    private void loadDataToBuffer(DirectBuffer buffer, int offset, int length) {
        assert isAligned(bufferLimit, BODY_ALIGNMENT);
        buffer.getBytes(offset, messageBuffer, bufferLimit, length);
        bufferLimit = align(bufferLimit + length, HEADER_ALIGNMENT);
    }
}
