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

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.InternalByteArrayOutputStream;
import io.aeron.Aeron;
import io.aeron.ExclusivePublication;
import io.aeron.Publication;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 *  Provides {@link DataOutputStream} API for Aeron's {@link Publication}.
 *
 * @author Alexei Osipov
 */
public class AeronPublicationDSAdapter {
    public static final String CHANNEL = TDBProtocol.AERON_CHANNEL;

    private final ExclusivePublication publication;

    // Publication buffers
    private final InternalByteArrayOutputStream internalBytes = new InternalByteArrayOutputStream();
    private final DataOutputStream dataOutputStream = new DataOutputStream(internalBytes) {
        /**
         * @throws PublicationClosedException if publication is already closed
         */
        @Override
        public void flush() throws IOException {
            // TODO: We probably should wrap PublicationClosedException into IOException but out client is already relies on PublicationClosedException. Refactor?
            AeronPublicationDSAdapter.this.flushDataOutputStream();
        }
    };

    private final UnsafeBuffer outUnsafeBuffer = new UnsafeBuffer(internalBytes.getInternalBuffer(), 0, 0);

    private final IdleStrategy publicationIdleStrategy;

    private boolean flushNeeded = false;


    public AeronPublicationDSAdapter(ExclusivePublication publication, IdleStrategy publicationIdleStrategy) {
        this.publication = publication;
        this.publicationIdleStrategy = publicationIdleStrategy;
    }

    public static AeronPublicationDSAdapter create(int publicationStreamId, Aeron aeron, IdleStrategy publicationIdleStrategy) {
        ExclusivePublication publication = aeron.addExclusivePublication(CHANNEL, publicationStreamId);
        return new AeronPublicationDSAdapter(publication, publicationIdleStrategy);
    }

    public DataOutputStream getDataOutputStream() {
        assert !flushNeeded;
        internalBytes.reset();
        flushNeeded = true;
        return dataOutputStream;
    }

    /**
     * @throws PublicationClosedException if publication is already closed
     */
    private void flushDataOutputStream() {
        assert flushNeeded;
        outUnsafeBuffer.wrap(internalBytes.getInternalBuffer(), 0, internalBytes.size());
        while (true) {
            long result = publication.offer(outUnsafeBuffer);
            if (result < 0) {
                if (result == Publication.BACK_PRESSURED || result == Publication.NOT_CONNECTED || result == Publication.ADMIN_ACTION) {
                    publicationIdleStrategy.idle();
                } else if (result == Publication.CLOSED) {
                    throw new PublicationClosedException();
                } else {
                    throw new RuntimeException("Unknown exception code: " + result);
                }
            } else {
                publicationIdleStrategy.reset();
                flushNeeded = false;
                return;
            }
        }
    }

    public boolean isClosed() {
        return publication.isClosed();
    }

    public void close() {
        publication.close();
    }

}
