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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.upload;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.InternalByteArrayInputStream;
import com.epam.deltix.util.memory.MemoryDataInput;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;

import javax.annotation.Nonnull;
import java.io.DataInputStream;

/**
 * @author Alexei Osipov
 */
public class AeronInputBufferAdapter {
    private final ExpandableArrayBuffer inputArrayBuffer = new ExpandableArrayBuffer();
    private final InternalByteArrayInputStream arrayInputStream = new InternalByteArrayInputStream(inputArrayBuffer.byteArray(), 0, 0);
    private final DataInputStream dataInputStream = new DataInputStream(arrayInputStream);
    private final MemoryDataInput mdi = new MemoryDataInput(arrayInputStream.getBuffer());

    private boolean multipart = false;
    private int multipartDataSize;
    private int multipartDataOffset;
    private int multipartDataNextOffset;
    private boolean multipartRemove;

    @Nonnull
    public DataInputStream copyFromDirect(DirectBuffer buffer, int offset, int length) {
        assert !multipart;
        assert arrayInputStream.available() == 0;
        buffer.getBytes(offset, inputArrayBuffer, 0, length);
        arrayInputStream.setBuffer(inputArrayBuffer.byteArray(), 0, length);
        return dataInputStream;
    }

    /**
     * @return true if that was the last part and data is ready to be processed
     */
    public boolean copyFromDirectForMultipart(DirectBuffer buffer, int offset, int length) {
        assert multipart;
        byte code = buffer.getByte(offset);
        if (code != TDBProtocol.LOAD_MULTIPART_BODY) {
            throw new IllegalStateException("Next message part code was expected");
        }
        int dataLength = length - 1;
        buffer.getBytes(offset + 1, inputArrayBuffer, multipartDataNextOffset, dataLength);
        multipartDataNextOffset += dataLength;

        int bytesRemaining = multipartDataOffset + multipartDataSize - multipartDataNextOffset;
        if (bytesRemaining > 0) {
            // Need more data
            return false;
        } else if (bytesRemaining == 0) {
            // We got all parts
            arrayInputStream.setBuffer(inputArrayBuffer.byteArray(), multipartDataOffset, multipartDataSize);
            multipart = false;
            return true;
        } else {
            throw new IllegalStateException("Got more data then expected from message part");
        }
    }

    public MemoryDataInput getAsMdi() {
        int available = arrayInputStream.available();
        mdi.setBytes(arrayInputStream.getBuffer(), arrayInputStream.getPosition(), available);
        long skipped = arrayInputStream.skip(available);
        assert skipped == available;
        return mdi;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public boolean isMultipartRemove() {
        return multipartRemove;
    }

    public void startMultipart(int dataSize, boolean multipartRemove) {
        this.multipart = true;
        this.multipartDataSize = dataSize;
        this.multipartDataOffset = arrayInputStream.getPosition();
        this.multipartDataNextOffset = this.multipartDataOffset + arrayInputStream.available();
        this.multipartRemove = multipartRemove;
    }
}