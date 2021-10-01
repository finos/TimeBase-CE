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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.comm.SelectionOptionsCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.unicast.AeronDownloadHandler;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickCursorWrapper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.security.DataFilter;
import com.epam.deltix.util.security.TimebaseAccessController;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Principal;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_AERON;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_SOCKET;

/**
 * Reads content of cursor creation request, decides on client type and creates corresponding client (socket-based or Aeron-based).
 *
 * @author Alexei Osipov
 */
public class DownloadHandlerFactory {
    public static void start(Principal user, VSChannel ds, DXTickDB db, QuickExecutor executor, TimebaseAccessController ac, int clientVersion, AeronThreadTracker aeronThreadTracker, DXServerAeronContext aeronContext) throws IOException {
        boolean aeronSupported = clientVersion >= TDBProtocol.AERON_SUPPORT_VERSION;
        int requestedTransportType = TRANSPORT_TYPE_SOCKET;
        if (aeronSupported) {
            DataInputStream din = ds.getDataInputStream();
            requestedTransportType = din.read();
        }

        SelectionOptions options = new SelectionOptions();

        String remoteAddress = ds.getRemoteAddress();
        DataFilter<RawMessage> filter = (ac != null ? ac.createFilter(user, remoteAddress) : null);

        DataOutputStream dout = ds.getDataOutputStream();
        DataInputStream din = ds.getDataInputStream ();

        boolean binary = true;
        if (clientVersion > 99)
            binary = din.readBoolean ();

        SelectionOptionsCodec.read (din, options, clientVersion);

        long initTime = din.readLong ();
        CharSequence[]   ids = TDBProtocol.readSymbols (din);

        String[] messageTypes = null;
        boolean  hasTypes = din.readBoolean();
        if (hasTypes)
            messageTypes = TDBProtocol.readNonNullableStrings(din);

        DXTickStream[]       streams;
        try {
            streams = DownloadHandler.readStreamList (ds, db);
        } catch (RuntimeException e) {
            dout.writeBoolean (false);
            AeronDownloadHandler.writeException(e, binary, dout);
            throw e;
        }

        boolean                 hasQuery = din.readBoolean ();
        InstrumentMessageSource cursor;
        TickCursor tcursor;
        if (hasQuery) {
            assert !options.restrictStreamType;

            String              qql = din.readUTF ();
            long                endTimestamp = clientVersion >= 131 ? din.readLong(): Long.MIN_VALUE; // added in 114 protocol version
            Parameter[]        params = TDBProtocol.readParameters (din);

            if (UserLogger.canTrace(user))
                UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CREATE_CURSOR_PATTERN, qql);

            try {
                cursor = db.executeQuery (qql, options, streams, ids, initTime, endTimestamp, params);
            } catch (CompilationException x) {
                UserLogger.severe(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "Query stream error: ", x);

                dout.writeBoolean (false);
                AeronDownloadHandler.writeException(new CompilationException(x.getClass().getSimpleName() + ": " + x.diag, x.location), binary, dout);
                throw new DownloadHandler.QueryCompilationFailed();
            } catch (RuntimeException x) {
                UserLogger.severe(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "Query stream error: ", x);

                dout.writeBoolean (false);
                AeronDownloadHandler.writeException(x, binary, dout);
                throw x;
            }

            tcursor = null;
        } else {

            try {
                if (streams == null)
                    cursor = tcursor = db.select (initTime, options, messageTypes, ids);
                else
                    cursor = tcursor = db.select (initTime, options, messageTypes, ids, streams);
            } catch (Exception e) {
                dout.writeBoolean (false);
                AeronDownloadHandler.writeException(e, binary, dout);
                throw new RuntimeException(e);
            }

            if (UserLogger.canTrace(user)) {
                StringBuilder sb = new StringBuilder();

                UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CREATE_CURSOR_PATTERN, cursor);
                sb.setLength(0);
                //noinspection ResultOfMethodCallIgnored
                DownloadHandler.toString(sb, streams);
                UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, sb.toString());

                sb.setLength(0);
                //noinspection ResultOfMethodCallIgnored
                DownloadHandler.toString(sb, ids);
                UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, sb.toString());
            }
        }

        int selectedTransportType = TRANSPORT_TYPE_SOCKET;
        if (requestedTransportType == TRANSPORT_TYPE_AERON && TDBProtocol.ALLOW_AERON_FOR_CURSOR && RequestHandler.isLocal(ds)) {
            // We will not use Aeron for MIN_LATENCY mode because it performs not as good as Sockets.
            // It will provide better latency only in LATENCY_CRITICAL mode.
            if (options.channelPerformance != ChannelPerformance.LOW_LATENCY) {
                selectedTransportType = TRANSPORT_TYPE_AERON;
            }
        }

        setCursorMonitorInfo(tcursor, user, ds.getRemoteApplication());

        if (selectedTransportType == TRANSPORT_TYPE_SOCKET) {
            DownloadHandler.createAndStart(options, user, ds, db, executor, clientVersion, filter, dout, binary, cursor, tcursor);
        } else if (selectedTransportType == TRANSPORT_TYPE_AERON) {
            Aeron aeron = aeronContext.getAeron();
            int aeronDataStreamId = aeronContext.getNextStreamId();
            int aeronCommandStreamId = aeronContext.getNextStreamId();
            String aeronDir = aeronContext.getAeronDir();
            AeronDownloadHandler.createAndStart(options, user, ds, db, cursor, tcursor, filter, binary, dout, clientVersion, aeronThreadTracker, aeron, aeronDir, aeronDataStreamId, aeronCommandStreamId);
        } else {
            throw new IllegalStateException("Unknown transport type code: " + selectedTransportType);
        }
    }

    private static void setCursorMonitorInfo(TickCursor cursor, Principal user, String application) {
        TickCursor tbCursor = cursor;
        if (tbCursor instanceof TickCursorWrapper)
            tbCursor = ((TickCursorWrapper) tbCursor).getNestedInstance();

        if (tbCursor instanceof TBObject) {
            TBObject tbObject = (TBObject) tbCursor;
            tbObject.setUser(user != null ? user.getName() : null);
            tbObject.setApplication(application);
        }
    }
}