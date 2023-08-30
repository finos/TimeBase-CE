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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.unicast;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.DownloadHandler;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.InsufficientCpuResourcesException;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.security.DataFilter;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Principal;


public class AeronDownloadHandler {
    public static final String CHANNEL = TDBProtocol.AERON_CHANNEL;

    public static void createAndStart(SelectionOptions options, Principal user, VSChannel ds, DXTickDB db, InstrumentMessageSource cursor, TickCursor tcursor, DataFilter<RawMessage> filter, boolean binary, DataOutputStream dout, int clientVersion, AeronThreadTracker aeronThreadTracker, Aeron aeron, String aeronDir, int aeronDataStreamId, int aeronCommandStreamId) throws IOException {
        AeronDownloadTask downloadTask = new AeronDownloadTask(aeron, aeronDataStreamId, aeronCommandStreamId, cursor, filter, binary, user, options.channelPerformance, ds, db, tcursor);

        Thread downloaderThread;
        try {
            downloaderThread = aeronThreadTracker.newDownloaderThread(downloadTask, options.channelPerformance == ChannelPerformance.LATENCY_CRITICAL);
        } catch (InsufficientCpuResourcesException e) {
            dout.writeBoolean (false);
            writeException(e, binary, dout);
            throw e;
        }

        dout.writeBoolean(true);  // OK
        DownloadHandler.writeSelectedTransport(clientVersion, dout, TDBProtocol.TRANSPORT_TYPE_AERON);
        dout.writeUTF(aeronDir);
        dout.writeInt(aeronDataStreamId);
        dout.writeInt(aeronCommandStreamId);
        dout.flush();

        ds.setAutoflush(true);

        ds.setNoDelay(options.channelPerformance == ChannelPerformance.LOW_LATENCY);

        // Empty runnable to trigger NextResult.UNAVAILABLE
        cursor.setAvailabilityListener(() -> {});

        downloaderThread.start();
    }

    public static void writeException(Throwable x, boolean binary, DataOutputStream dout) throws IOException {
        if (binary)
            TDBProtocol.writeBinary(dout, x);
        else
            TDBProtocol.writeError(dout, x);
    }
}