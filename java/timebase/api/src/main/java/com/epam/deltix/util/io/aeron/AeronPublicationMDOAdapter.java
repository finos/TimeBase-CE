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
package com.epam.deltix.util.io.aeron;

import com.epam.deltix.util.memory.MemoryDataOutput;
import io.aeron.ExclusivePublication;
import io.aeron.Publication;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Provides {@link MemoryDataOutput} API for Aeron's {@link Publication}.
 *
 * @author Alexei Osipov
 */
public class AeronPublicationMDOAdapter {
    private final ExclusivePublication publication;

    // Publication buffers
    private final MemoryDataOutput mdi = new MemoryDataOutput();


    private final UnsafeBuffer outUnsafeBuffer = new UnsafeBuffer(mdi.getBuffer(), 0, 0);

    private final IdleStrategy publicationIdleStrategy;

    private boolean flushNeeded = false;


    public AeronPublicationMDOAdapter(ExclusivePublication publication, IdleStrategy publicationIdleStrategy) {
        this.publication = publication;
        this.publicationIdleStrategy = publicationIdleStrategy;
    }

    public MemoryDataOutput getMemoryDataOutput() {
        assert !flushNeeded;
        mdi.reset();
        flushNeeded = true;
        return mdi;
    }

    /**
     * Sends data from buffer as is. Buffer content must be smaller than Aeron's buffer.
     *
     * @throws PublicationClosedException if publication is already closed
     */
    public void sendBuffer() {
        sendBuffer(0, true);
    }

    /**
     * Sends data from buffer as is. Buffer content must be smaller than Aeron's buffer.
     * If there is no connected clients then just discard data.
     *
     * @throws PublicationClosedException if publication is already closed
     */
    public boolean sendBufferIfConnected() {
        return sendBuffer(0, false);
    }

    private boolean sendBuffer(int startingOffset, boolean retryIfNotConnected) {
        assert flushNeeded;
        int dataLength = mdi.getPosition() - startingOffset;
        assert dataLength <= publication.maxMessageLength();

        outUnsafeBuffer.wrap(mdi.getBuffer(), startingOffset, dataLength);

        flushNeeded = false;
        publicationIdleStrategy.reset();

        while (true) {
            long result = publication.offer(outUnsafeBuffer);
            if (result < 0) {
                if (result == Publication.NOT_CONNECTED && !retryIfNotConnected) {
                    // Just discard message if no subscribers connected.
                    return false;
                }
                // May throw exception
                handlePublicationError(result);
            } else {
                // Success
                return true;
            }
        }
    }

    /**
     * Depending on message size sends it as as or splits it into multiple parts. See {@link #sendMultipartMessage(int, byte, byte)}
     *
     * @throws PublicationClosedException if publication is already closed
     */
    public void sendMessage(int offset, byte multipartMessageHeader, byte multipartAdditionalPartHeader) {
        int length = mdi.getPosition();
        int maxMessageLength = publication.maxMessageLength();
        if (length > maxMessageLength) {
            sendMultipartMessage(offset, multipartMessageHeader, multipartAdditionalPartHeader);
        } else {
            sendBuffer(offset, true);
        }
    }

    /**
     * Sends message in multiple parts.
     * <p>
     * First part starts with {@code multipartMessageHeader} 1 byte code.
     * <b>First byte of original message data is discarded (byte at {@code offset}.</b>
     * Remaining parts start with {@code multipartAdditionalPartHeader} 1 byte code.
     *
     * Data offset must be at least 4. First 4 bytes of buffer content will be overwritten.
     *
     * <p>
     * Expected input buffer content:
     * <pre>
     * [4(+) offset bytes] [1 header byte (discarded)] [data part 1] [data part 2] [data part 3] ...
     * </pre>
     * Sent data:
     * <pre>
     * msg 1: [1 byte header with value "multipartMessageHeader"] [4 byte data size] [data part 1]
     * msg 2: [1 byte header with value "multipartAdditionalPartHeader"] [data part 2]
     * msg 3: [1 byte header with value "multipartAdditionalPartHeader"] [data part 3]
     * ...
     * </pre>
     * "4 byte data size" is sum length of all data parts.
     */
    private void sendMultipartMessage(int offset, byte multipartMessageHeader, byte multipartAdditionalPartHeader) {
        assert flushNeeded;
        int extraSpaceForMessageSize = Integer.BYTES;
        int partHeaderSize = Byte.BYTES;
        if (offset < extraSpaceForMessageSize) {
            throw new IllegalArgumentException("At least " + extraSpaceForMessageSize + " of extra padding for message size required");
        }
        int maxMessageLength = publication.maxMessageLength();
        byte[] buffer = mdi.getBuffer();

        int endPosition = mdi.getPosition();
        int originalMessageLength = endPosition - offset; // Original message length (including first header byte)
        int dataLength = originalMessageLength - partHeaderSize; // Data length

        offset -= extraSpaceForMessageSize; // Expand buffer backwards to make space for message size
        assert endPosition - offset == partHeaderSize + extraSpaceForMessageSize + dataLength;
        outUnsafeBuffer.wrap(buffer);

        mdi.seek(offset);
        mdi.writeByte(multipartMessageHeader);
        mdi.writeInt(dataLength);


        int sendOffset = offset;
        int sendLength = maxMessageLength;
        while (sendOffset + partHeaderSize < endPosition) {
            long result = publication.offer(outUnsafeBuffer, sendOffset, sendLength);
            if (result < 0) {
                handlePublicationError(result);
            } else {
                publicationIdleStrategy.reset();
                sendOffset += sendLength - partHeaderSize; // Step one byte back so we have space for header byte
                if (sendOffset + sendLength > endPosition) {
                    // Last part of message
                    sendLength = endPosition - sendOffset;
                }
                mdi.seek(sendOffset);
                mdi.writeByte(multipartAdditionalPartHeader); // Write header byte. Note: we override data we already sent.
            }
        }

        flushNeeded = false;
    }

    private void handlePublicationError(long result) {
        if (result == Publication.BACK_PRESSURED || result == Publication.NOT_CONNECTED || result == Publication.ADMIN_ACTION) {
            publicationIdleStrategy.idle();
        } else if (result == Publication.CLOSED) {
            throw new PublicationClosedException();
        } else if (result == Publication.MAX_POSITION_EXCEEDED) {
            // TODO: Re-create publication OR ensure that term buffer length is big enough.
            throw new RuntimeException("Max position exceeded. Publication should be closed and re-created.");
        } else {
            throw new RuntimeException("Unknown exception code: " + result);
        }
    }

    public int getAeronSessionId() {
        return publication.sessionId();
    }

    public void cancelSend() {
        assert flushNeeded;
        flushNeeded = false;
    }

    public boolean isClosed() {
        return publication.isClosed();
    }

    public void close() {
        publication.close();
    }
}
