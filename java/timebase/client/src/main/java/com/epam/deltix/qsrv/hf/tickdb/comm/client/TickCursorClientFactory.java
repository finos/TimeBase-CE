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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.comm.SelectionOptionsCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeInstrumentIdentities;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

/**
 * @author Alexei Osipov
 */
public class TickCursorClientFactory {

    public static TickCursor create(
            DXRemoteDB                 db,
            SelectionOptions           inOptions,
            long                       time,
            String                     query,
            Parameter[]                parameters,
            IdentityKey[]              ids,
            String[]                   types,
            DXClientAeronContext       aeronContext,
            TickStream...              streams
    ) {
        return create(db, inOptions, time, Long.MAX_VALUE, query, parameters, ids, types, aeronContext, streams);
    }

    public static TickCursor create(
            DXRemoteDB                 db,
            SelectionOptions           inOptions,
            long                       startTimestamp,
            long                       endTimestamp,
            String                     query,
            Parameter[]                parameters,
            IdentityKey[]              ids,
            String[]                   types,
            DXClientAeronContext       aeronContext,
            TickStream...              streams
    ) {
        SelectionOptions options = inOptions == null ? new SelectionOptions () : inOptions;
        assert !options.restrictStreamType || query == null : "restrictStreamType and query are not compatible";

        boolean     ok = false;
        VSChannel tmpds = null;

        try {
            tmpds = db.connect (ChannelType.Input, false, false, options.compression, options.channelBufferSize);

            DataOutputStream out = tmpds.getDataOutputStream ();
            out.writeInt (REQ_CREATE_CURSOR);

            boolean aeronSupported = db.getServerProtocolVersion() >= TDBProtocol.AERON_SUPPORT_VERSION;
            if (aeronSupported) {
                byte preferredTransport = options.channelPerformance == ChannelPerformance.HIGH_THROUGHPUT ||
                                options.channelPerformance == ChannelPerformance.LATENCY_CRITICAL
                        ? TRANSPORT_TYPE_AERON : TRANSPORT_TYPE_SOCKET;
                out.write(preferredTransport);
            }

            out.writeBoolean(true); // binary serialization
            SelectionOptionsCodec.write (out, options, db.getServerProtocolVersion());
            out.writeLong (startTimestamp);

            writeInstrumentIdentities (ids, out);

            boolean allEntitiesSubscribed = (ids == null);
            InstrumentToObjectMap<Long> subscribedEntities = new InstrumentToObjectMap<>();
            if (!allEntitiesSubscribed) {
                for (IdentityKey id : ids) {
                    subscribedEntities.put(id, 0L);
                }
            }

            out.writeBoolean(types != null);
            if (types != null) {
                writeNonNullableStrings(out, types);
            }

            boolean allTypesSubscribed = (types == null);
            HashSet<String> subscribedTypes = new HashSet<>();
            if (!allTypesSubscribed) {
                subscribedTypes.addAll(Arrays.asList(types));
            }

            TickCursorClient.writeStreamKeys(out, streams);

            HashSet<TickStream> subscribedStreams = new HashSet<>();
            if (streams != null) {
                subscribedStreams.addAll(Arrays.asList(streams));
            }

            out.writeBoolean (query != null);
            if (query != null) {
                out.writeUTF (query);
                if (db.getServerProtocolVersion() >= 131) { // added in protocol version 114
                    out.writeLong(endTimestamp);
                }
                writeParameters (parameters, out);
            }

            out.flush ();

            final DataInputStream in = tmpds.getDataInputStream();
            boolean success = in.readBoolean ();

            if (TickCursorClient.DEBUG_COMM) {
                TickDBClient.LOGGER.info (TickCursorClientFactory.class + ": CREATE {" +
                        Arrays.toString(streams) + ";" + Arrays.toString(ids) + ";" + Arrays.toString(types) + "}");
            }

            //conn.onReconnected ();

            if (!success) {
                TickCursorClient.processError(in);
                throw new AssertionError("Unreachable");
            } else {

                int transportType = TRANSPORT_TYPE_SOCKET;
                if (aeronSupported) {
                    transportType = in.read();
                }

                TickCursor result;
                if (transportType == TRANSPORT_TYPE_SOCKET) {
                    result = new TickCursorClient(db, tmpds, options, startTimestamp, allEntitiesSubscribed, allTypesSubscribed, subscribedEntities, subscribedTypes, subscribedStreams);
                } else if (transportType == TRANSPORT_TYPE_AERON) {
                    String aeronDir = in.readUTF();
                    Aeron aeron = aeronContext.getServerSharedAeronInstance(aeronDir);
                    int aeronDataStreamId = in.readInt();
                    int aeronCommandStreamId = in.readInt();
                    result = new TickCursorClientAeron(db, tmpds, options, startTimestamp, allEntitiesSubscribed, allTypesSubscribed, subscribedEntities, subscribedTypes, subscribedStreams, aeron, aeronDataStreamId, aeronCommandStreamId, aeronContext.getSubscriptionChecker());
                } else {
                    throw new IllegalStateException("Unknown transport type code: " + transportType);
                }
                ok = true;
                return result;
            }
        } catch (IOException x) {
            if (x instanceof SocketException) {
                if (TickCursorClient.isChannelClosed(tmpds)) {
                    throw new IllegalStateException (
                            "Cursor is closed either by a client or upon a disconnection event."
                    );
                }
            }

            if (x instanceof InterruptedIOException) {
                throw new UncheckedInterruptedException(x);
            }

            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        } finally {
            if (!ok) {
                Util.close(tmpds);
            }
        }
    }

    public static TickCursor create(
            DXRemoteDB                 db,
            SelectionOptions           inOptions,
            long                       time,
            String                     query,
            Parameter[]                parameters,
            String[]                   types,
            DXClientAeronContext       aeronContext,
            TickStream...              streams
    ) {
        IdentityKey[] ids = null;
        return create(db, inOptions, time, query, parameters, ids, types, aeronContext, streams);
    }

    public static TickCursor create(
            DXRemoteDB                 db,
            SelectionOptions           inOptions,
            long                       time,
            String                     query,
            Parameter[]                parameters,
            DXClientAeronContext       aeronContext,
            TickStream...              streams
    ) {
        String[] types = null;
        return create(db, inOptions, time, query, parameters, types, aeronContext, streams);
    }

    public static TickCursor create(
            DXRemoteDB                 db,
            SelectionOptions           inOptions,
            long                       startTime,
            long                       endTime,
            String                     query,
            Parameter[]                parameters,
            CharSequence[]             symbols,
            String[]                   types,
            DXClientAeronContext       aeronContext,
            TickStream...              streams
    ) {
        SelectionOptions options = inOptions == null ? new SelectionOptions () : inOptions;
        assert !options.restrictStreamType || query == null : "restrictStreamType and query are not compatible";

        boolean     ok = false;
        VSChannel tmpds = null;

        try {
            tmpds = db.connect (ChannelType.Input, false, false, options.compression, options.channelBufferSize);

            DataOutputStream out = tmpds.getDataOutputStream ();
            out.writeInt (REQ_CREATE_CURSOR);

            boolean aeronSupported = db.getServerProtocolVersion() >= TDBProtocol.AERON_SUPPORT_VERSION;
            if (aeronSupported) {
                byte preferredTransport = options.channelPerformance == ChannelPerformance.HIGH_THROUGHPUT ||
                        options.channelPerformance == ChannelPerformance.LATENCY_CRITICAL
                        ? TRANSPORT_TYPE_AERON : TRANSPORT_TYPE_SOCKET;
                out.write(preferredTransport);
            }

            out.writeBoolean(true); // binary serialization
            SelectionOptionsCodec.write (out, options, db.getServerProtocolVersion());
            out.writeLong (startTime);

            IdentityKey[] ids = writeSymbols(symbols, out);

            boolean allEntitiesSubscribed = (symbols == null);
            InstrumentToObjectMap<Long> subscribedEntities = new InstrumentToObjectMap<>();
            if (!allEntitiesSubscribed) {
                for (IdentityKey id : ids) {
                    subscribedEntities.put(id, 0L);
                }
            }

            out.writeBoolean(types != null);
            if (types != null) {
                writeNonNullableStrings(out, types);
            }

            boolean allTypesSubscribed = (types == null);
            HashSet<String> subscribedTypes = new HashSet<>();
            if (!allTypesSubscribed) {
                subscribedTypes.addAll(Arrays.asList(types));
            }

            TickCursorClient.writeStreamKeys(out, streams);

            HashSet<TickStream> subscribedStreams = new HashSet<>();
            if (streams != null) {
                subscribedStreams.addAll(Arrays.asList(streams));
            }

            out.writeBoolean (query != null);
            if (query != null) {
                out.writeUTF (query);
                if (db.getServerProtocolVersion() >= 131) { // added in protocol version 114
                    out.writeLong(endTime);
                }
                writeParameters (parameters, out);
            }

            out.flush ();

            final DataInputStream in = tmpds.getDataInputStream();
            boolean success = in.readBoolean ();

            if (TickCursorClient.DEBUG_COMM) {
                TickDBClient.LOGGER.info (TickCursorClientFactory.class + ": CREATE {" +
                        Arrays.toString(streams) + ";" + Arrays.toString(ids) + ";" + Arrays.toString(types) + "}");
            }

            //conn.onReconnected ();

            if (!success) {
                TickCursorClient.processError(in);
                throw new AssertionError("Unreachable");
            } else {

                int transportType = TRANSPORT_TYPE_SOCKET;
                if (aeronSupported) {
                    transportType = in.read();
                }

                TickCursor result;
                if (transportType == TRANSPORT_TYPE_SOCKET) {
                    result = new TickCursorClient(db, tmpds, options, startTime, allEntitiesSubscribed, allTypesSubscribed, subscribedEntities, subscribedTypes, subscribedStreams);
                } else if (transportType == TRANSPORT_TYPE_AERON) {
                    String aeronDir = in.readUTF();
                    Aeron aeron = aeronContext.getServerSharedAeronInstance(aeronDir);
                    int aeronDataStreamId = in.readInt();
                    int aeronCommandStreamId = in.readInt();
                    result = new TickCursorClientAeron(db, tmpds, options, startTime, allEntitiesSubscribed, allTypesSubscribed, subscribedEntities, subscribedTypes, subscribedStreams, aeron, aeronDataStreamId, aeronCommandStreamId, aeronContext.getSubscriptionChecker());
                } else {
                    throw new IllegalStateException("Unknown transport type code: " + transportType);
                }
                ok = true;
                return result;
            }
        } catch (IOException x) {
            if (x instanceof SocketException) {
                if (TickCursorClient.isChannelClosed(tmpds)) {
                    throw new IllegalStateException (
                            "Cursor is closed either by a client or upon a disconnection event."
                    );
                }
            }

            if (x instanceof InterruptedIOException) {
                throw new UncheckedInterruptedException(x);
            }

            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        } finally {
            if (!ok) {
                Util.close(tmpds);
            }
        }
    }
}