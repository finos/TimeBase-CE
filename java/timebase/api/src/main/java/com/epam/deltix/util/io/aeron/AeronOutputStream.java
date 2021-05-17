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

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.epam.deltix.util.vsocket.VSProtocol.CHANNEL_MAX_BUFFER_SIZE;

@Deprecated
public class AeronOutputStream extends OutputStream {
    private final UnsafeBuffer          buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(CHANNEL_MAX_BUFFER_SIZE));

    private final Publication           publication;

    AeronOutputStream(Aeron aeron, String channel, int streamId) {
        publication = aeron.addPublication(channel, streamId);
    }

    @Override
    public void                         write(int b) throws IOException {
        long result = 0;
        do {
            if (result == Publication.CLOSED)
                throw new IOException("Stream is closed!");

            buffer.putByte(0, (byte) b);
        } while ((result = publication.offer(buffer, 0, 1)) < 0L);
    }

    @Override
    public void                         write(byte b[], int off, int len)
        throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
            ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        long result = 0;
        do {
            if (result == Publication.CLOSED)
                throw new IOException("Stream is closed!");

            // TODO: Should this be done outside of loop?
            buffer.putBytes(0, b, off, len);
        } while ((result = publication.offer(buffer, 0, len)) < 0L);
    }

    @Override
    public void                         close() {
        if (publication != null) {
            publication.close();
        }
    }
}