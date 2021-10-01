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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.upload;

import com.epam.deltix.data.stream.MessageDecoder;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.RequestHandler;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.SimpleRawDecoder;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TransportNegotiation;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.unicast.AeronDownloadHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockHandler;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;
import io.aeron.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Principal;

public class AeronUploadHandler {

    public static void start(Principal user, VSChannel ds, int clientVersion, Aeron aeron, int aeronServerMessageStreamId, int aeronLoaderDataStreamId, AeronThreadTracker aeronThreadTracker, String aeronDir, boolean binary, @Nonnull LoadingOptions options, @Nonnull DXTickStream stream, @Nullable DBLock lock)
        throws IOException
    {

        DataOutputStream out = ds.getDataOutputStream ();

        AeronUploadLockHolder lockHolder;
        // we should abort writer when 'write' lock added.
        if (lock == null && stream instanceof LockHandler) {
            // TODO: Impl

            LockHandler lockHandler = (LockHandler) stream;
            lockHolder = new AeronUploadLockHolder(lockHandler);
            lockHandler.addEventListener(lockHolder);
        } else {
            lockHolder = new AeronUploadLockHolder(null);
        }

        RecordClassSet      md = RequestHandler.getMetaData (stream);

        MessageDecoder decoder = new SimpleRawDecoder(md.getTopTypes ());
        TickLoader loader = stream.createLoader (options);


        Subscription subscription = aeron.addSubscription(AeronDownloadHandler.CHANNEL, aeronLoaderDataStreamId);
        AeronUploadTask uploadTask = new AeronUploadTask(user, subscription, loader, stream.getKey(), aeronServerMessageStreamId, decoder, binary, options.channelPerformance, lockHolder, ds);

        Thread uploaderThread;
        try {
            uploaderThread = aeronThreadTracker.newUploaderThread(uploadTask, options.channelPerformance == ChannelPerformance.LATENCY_CRITICAL);
        } catch (InsufficientCpuResourcesException e) {
            out.writeInt (TDBProtocol.RESP_ERROR);
            AeronDownloadHandler.writeException(e, binary, out); // TODO: Test
            throw e;
        }

        //ds.setAvailabilityListener (avlnr);
        out.writeInt (TDBProtocol.RESP_OK);
        TransportNegotiation.writeSelectedTransport(clientVersion, out, TDBProtocol.TRANSPORT_TYPE_AERON);
        out.writeUTF(aeronDir);
        out.writeInt(aeronServerMessageStreamId); // Direction: from server to client
        out.writeInt(aeronLoaderDataStreamId); // Direction: from client to server
        out.flush();

        uploadTask.installListeners();

        uploaderThread.start();
    }

}