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
package com.epam.deltix.qsrv.hf.tickdb.http.rest;

import com.epam.deltix.qsrv.hf.pub.md.json.SchemaBuilder;
import com.epam.deltix.qsrv.hf.pub.util.SerializationUtils;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.GetBGProcessResponse;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.GetRangeResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerStreamWrapper;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.StreamState;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.ListEntitiesResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateListener;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.ByteArrayOutputStreamEx;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.LittleEndianDataInputStream;
import com.epam.deltix.util.time.Periodicity;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.*;

public class SessionHandler extends RestHandler implements StreamStateListener {

    private final Socket            socket;
    private final DataInput         din;
    private final DataOutputStream  out;
    private final DXTickDB          db;
    private final GUID              guid;
    private final short             clientVersion;
    private final ContextContainer contextContainer;

    private final ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx(8192);
    private final ByteArrayOutputStreamEx propertyBuffer = new ByteArrayOutputStreamEx(8192);
    //private final DataOutputStream dout = new DataOutputStream(buffer);

    private final Map<String, StreamState> states = new HashMap<>();

    private volatile boolean isClosed = false;

    private final class ControlTask extends QuickExecutor.QuickTask {
        final Runnable              avlnr =
                new Runnable () {
                    @Override
                    public void                 run () {
                        ControlTask.this.submit ();
                    }
                };

        public ControlTask (QuickExecutor exe) {
            super (exe);
        }

        @Override
        public String       toString () {
            return ("Control Task for " + SessionHandler.this);
        }

        @Override
        public void         run () {
            try {
                for (;;) {

                    if (processCommand() == -1)
                        break;
                }
            } catch (EOFException iox) {
                // valid close
                closeAll ();
            } catch (IOException iox) {
                HTTPProtocol.LOGGER.log (Level.INFO, "Exception in " + toString(), iox);

                closeAll ();
            }
        }
    }
    private volatile ControlTask controlTask;

    // Events processor
    public enum EventType {
        RENAMED, CREATED, DELETED, WRITER_CLOSED, WRITER_CREATED, PROPERTY_CHANGED, RELOAD,
        GET_STREAMS_REQ, GET_PROPERTY_REQ
    }

    public static class Event {
        public final EventType type;

        public Event(EventType type) {
            this.type = type;
        }
    }

    public static class ChangeEvent extends Event {
        public final String stream;

        public ChangeEvent(EventType type, String stream) {
            super(type);
            this.stream = stream;
        }
    }

    public static class PropertyChangedEvent extends ChangeEvent {
        public final int property;

        public PropertyChangedEvent(String stream, int property) {
            super(EventType.PROPERTY_CHANGED, stream);
            this.property = property;
        }
    }

    public static class WriterActionEvent extends ChangeEvent {
        public final IdentityKey[] ids;

        public WriterActionEvent(String stream, boolean created, IdentityKey[] ids) {
            super(created ? EventType.WRITER_CREATED : EventType.WRITER_CLOSED, stream);
            this.ids = ids;
        }
    }

    public static class RenamedActionEvent extends ChangeEvent {
        public final String oldKey;

        public RenamedActionEvent(String stream, String previous) {
            super(EventType.RENAMED, stream);
            this.oldKey = previous;
        }
    }

    public static class RequestEvent extends Event {
        public final long serial;

        public RequestEvent(EventType eventType, long serial) {
            super(eventType);
            this.serial = serial;
        }
    }

    public static class GetStreamsRequestEvent extends RequestEvent {
        public final List<String> streams;
        public final int chunkSize; // <= 0 means "no chunks"

        public GetStreamsRequestEvent(long serial, List<String> streams, int chunkSize) {
            super(EventType.GET_STREAMS_REQ, serial);
            this.streams = streams;
            this.chunkSize = chunkSize;
        }
    }

    public static class GetPropertyRequestEvent extends RequestEvent {
        public final String stream;
        public final int property;

        public GetPropertyRequestEvent(long serial, String stream, int property) {
            super(EventType.GET_PROPERTY_REQ, serial);
            this.stream = stream;
            this.property = property;
        }
    }

    private final class ProcessEventsTask extends QuickExecutor.QuickTask {
        public ProcessEventsTask(QuickExecutor exe) {
            super(exe);
        }

        @Override
        public String toString() {
            return ("Event Process Task for " + SessionHandler.this);
        }

        @Override
        public void run() {
            try {
                processEvents();
            } catch (EOFException iox) {
                // valid close
                closeAll ();
            } catch (IOException iox) {
                HTTPProtocol.LOGGER.log (Level.INFO, "Exception in " + toString(), iox);
                closeAll ();
            }
        }
    }

    private final ProcessEventsTask processEventsTask;

    private final LinkedList<Event> events = new LinkedList<>();

    private final Runnable reloadListener = new Runnable() {
        @Override
        public void run() {
            onStreamsReloaded();
        }
    };

    public SessionHandler(DXTickDB db, Socket socket, InputStream input, OutputStream output, short clientVersion,
                          ContextContainer contextContainer) throws IOException {
        super(contextContainer.getQuickExecutor());
        this.db = db;
        this.socket = socket;
        this.out = new DataOutputStream(output);
        this.clientVersion = clientVersion;
        this.contextContainer = contextContainer;
        this.processEventsTask = new ProcessEventsTask(contextContainer.getQuickExecutor());

        boolean useCompression = (input.read() == 1);
        InputStream is = useCompression ? new GZIPInputStream(input) : input;

        final int endianness = is.read();
        switch (endianness) {
            case 0: // Little-endian
                this.din = new LittleEndianDataInputStream(is);
                break;
            case 1: // Big-endian
                this.din = new DataInputStream(is);
                break;
            default:
                throw new ValidationException(String.format("invalid endianness field %d", endianness));
        }

        this.guid = new GUID();

        synchronized (out) {
            out.writeInt(SESSION_STARTED);
            out.writeUTF(guid.toString());
        }
    }

    private int        processCommand () throws IOException {
        int                 code = din.readInt ();

        switch (code) {
            case REQ_GET_STREAMS:
                processGetStreams(); break;

            case REQ_GET_STREAMS_CHUNKED:
                processGetStreamsChunked(); break;

            case REQ_GET_STREAM_PROPERTY:
                processGetProperty(); break;

            case REQ_CLOSE_SESSION:
                closeAll();
                return -1;
        }

        return 0;
    }

    private void processEvents() throws IOException {
        while (true) {
            Event event;
            synchronized (events) {
                event = events.poll();
            }

            if (event == null) {
                break;
            }

            // process actions
            switch (event.type) {
                case RENAMED:
                    sendRenamed(((ChangeEvent) event).stream, ((RenamedActionEvent) event).oldKey);
                    break;
                case CREATED:
                    sendCreated(((ChangeEvent) event).stream);
                    break;
                case DELETED:
                    sendDeleted(((ChangeEvent) event).stream);
                    break;
                case WRITER_CLOSED:
                    sendWriterClosed(((ChangeEvent) event).stream, ((WriterActionEvent) event).ids);
                    break;
                case WRITER_CREATED:
                    sendWriterCreated(((ChangeEvent) event).stream, ((WriterActionEvent) event).ids);
                    break;
                case PROPERTY_CHANGED:
                    sendChanged(((ChangeEvent) event).stream, ((PropertyChangedEvent) event).property);
                    break;
                case GET_STREAMS_REQ:
                    GetStreamsRequestEvent getStreams = (GetStreamsRequestEvent) event;
                    if (getStreams.chunkSize > 0) {
                        sendGetStreamsChunked(getStreams.serial, getStreams.streams, getStreams.chunkSize);
                    } else {
                        sendGetStreams(getStreams.serial, getStreams.streams);
                    }
                    break;
                case GET_PROPERTY_REQ:
                    GetPropertyRequestEvent getProperty = (GetPropertyRequestEvent) event;
                    sendGetProperty(getProperty.serial, getProperty.stream, getProperty.property);
                    break;
            }
        }
    }

    private void submitEvent(Event event) {
        synchronized (events) {
            events.add(event);
        }

        processEventsTask.submit();
    }

    private void processGetStreams() throws IOException {
        long serial = din.readLong();
        List<String> streams = readStreamKeys();
        submitEvent(new GetStreamsRequestEvent(serial, streams, -1));
    }

    private void sendGetStreams(long serial, List<String> streamKeys) throws IOException {
        List<DXTickStream> streams = getStreams(streamKeys);
        writeStreams(buffer, streams, 0, streams.size());
        synchronized (out) {
            out.writeInt(STREAMS_DEFINITION);
            out.writeLong(serial);
            out.writeInt(buffer.size());
            buffer.writeTo(out);
            buffer.reset();
            out.flush();
        }
    }

    private void processGetStreamsChunked() throws IOException {
        long serial = din.readLong();
        int chunkSize = din.readInt();
        List<String> streams = readStreamKeys();
        submitEvent(new GetStreamsRequestEvent(serial, streams, chunkSize));
    }

    private void sendGetStreamsChunked(long serial, List<String> streamKeys, int chunkSize) throws IOException {
        List<DXTickStream> streams = getStreams(streamKeys);

        synchronized (out) {
            out.writeInt(STREAMS_DEFINITION_CHUNKED);
            out.writeLong(serial);
            out.writeInt(streams.size());
            out.flush();
        }

        int offset = 0;
        while (offset < streams.size()) {
            writeStreams(buffer, streams, offset, chunkSize);

            synchronized (out) {
                out.writeInt(STREAMS_DEFINITION);
                out.writeLong(serial);
                out.writeInt(buffer.size());
                buffer.writeTo(out);
                buffer.reset();
                out.flush();
                offset += chunkSize;
            }
        }

        synchronized (out) {
            out.writeInt(END_STREAMS_DEFINITION_CHUNKED);
            out.writeLong(serial);
            out.flush();
        }
    }

    private List<String> readStreamKeys() throws IOException {
        List<String> streams = new ArrayList<>();
        int count = din.readInt();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                streams.add(din.readUTF());
            }
        }

        return streams;
    }

    private List<DXTickStream> getStreams(List<String> streamKeys) {
        List<DXTickStream> streams;
        if (streamKeys.isEmpty()) {
            streams = Arrays.asList(db.listStreams());
        } else {
            streams = new ArrayList<>();
            for (int i = 0; i < streamKeys.size(); i++) {
                DXTickStream stream = db.getStream(streamKeys.get(i));

                // protocol may ask for non-existent streams
                if (stream != null) {
                    streams.add(stream);
                }
            }
        }

        return streams;
    }

    private void        writeStreams(ByteArrayOutputStreamEx buffer, List<DXTickStream> streams, int offset, int length) {
        StringWriter writer = new StringWriter();
        HashMap<String, StreamDef> data = new HashMap<>();

        int size = Math.min(streams.size(), offset + length);
        for (int i = offset; i < size; i++) {
            DXTickStream stream = streams.get(i);

            try {
                String key = stream.getKey();

                StreamDef streamDef = new StreamDef(stream.getStreamOptions());

                writer.getBuffer().setLength(0);
                marshallUHF(stream.getStreamOptions().getMetaData(), writer);
                streamDef.metadata = writer.getBuffer().toString();
                streamDef.metadataJson = HTTPProtocol.JSON_MAPPER.writeValueAsString(
                    SchemaBuilder.toSchemaDef(
                        stream.getStreamOptions().getMetaData(), false
                    )
                );

                data.put(key, streamDef);

                synchronized (states) {
                    StreamState state = getState(key);
                    if (state == null)
                        state = new StreamState();
                    state.set(false);
                    states.put(key, state);
                }
            } catch (Throwable e) {
                HTTPProtocol.LOGGER.log(Level.WARNING, "Session: sending stream " + stream + " definition failed.", e);
            }
        }

        LoadStreamsResponse r = new LoadStreamsResponse();
        r.streams = data.keySet().toArray(new String[data.size()]);
        r.options = data.values().toArray(new StreamDef[data.size()]);

        marshall(r, buffer);
    }

    private void processGetProperty() throws IOException {
        long serial = din.readLong();
        String key = din.readUTF();
        int property = din.readByte();

        submitEvent(new GetPropertyRequestEvent(serial, key, property));
    }

    private void sendGetProperty(long serial, String key, int property) throws IOException {
        DXTickStream stream = db.getStream(key);

        //TickDBServer.LOGGER.info("getProperty(" + key + ", "  + property + ")");

        switch (property) {
            case TickStreamProperties.NAME:
                String name = stream != null ? stream.getName() : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(name != null ? name : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.DESCRIPTION:
                String description = stream != null ? stream.getDescription() : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(description != null ? description : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.PERIODICITY:
                Periodicity p = stream != null ? stream.getPeriodicity() : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(p != null ? p.toString() : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.SCHEMA:
                StreamOptions options = stream != null ? stream.getStreamOptions() : new StreamOptions();

                boolean polymorphic = options.isPolymorphic();
                RecordClassSet schema = options.getMetaData();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(polymorphic);

                    // write xml schema
                    propertyBuffer.reset();
                    marshallUHF(schema, propertyBuffer);
                    out.writeInt(propertyBuffer.size());
                    propertyBuffer.writeTo(out);
                    propertyBuffer.reset();

                    SerializationUtils.writeUTFString(out,
                        HTTPProtocol.JSON_MAPPER.writeValueAsString(
                            SchemaBuilder.toSchemaDef(schema, false)
                        )
                    );

                    out.flush();
                }
                break;

            case TickStreamProperties.ENTITIES:
                IdentityKey[] ids = stream.listEntities();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    marshalAndWrite(new ListEntitiesResponse(ids), out);
                    out.flush();
                }
                break;

            case TickStreamProperties.TIME_RANGE:
                long[] range = stream != null ? stream.getTimeRange() : null;

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    TimeRange timeRange = range != null ?
                        new TimeRange(range[0], range[1]) :
                        new TimeRange(Long.MIN_VALUE, Long.MAX_VALUE);
                    marshalAndWrite(new GetRangeResponse(timeRange), out);

                    out.flush();
                }
                break;

            case TickStreamProperties.BG_PROCESS:
                BackgroundProcessInfo process = stream != null ? stream.getBackgroundProcess() : null;

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(process != null);
                    if (process != null) {
                        marshalAndWrite(new GetBGProcessResponse(process), out);
                    }
                    out.flush();
                }
                break;

            case TickStreamProperties.OWNER:
                String owner = stream != null ? stream.getOwner() : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(owner != null ? owner : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.HIGH_AVAILABILITY:
                boolean ha = stream != null ? stream.getHighAvailability() : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(ha);
                    out.flush();
                }
                break;

            case TickStreamProperties.UNIQUE:
                boolean unique = stream != null && stream.getStreamOptions().unique;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(unique);
                    out.flush();
                }
                break;

            case TickStreamProperties.VERSIONING:
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(true);
                    out.flush();
                }
                break;

            case TickStreamProperties.BUFFER_OPTIONS:
                BufferOptions bufferOptions = stream != null ? stream.getStreamOptions().bufferOptions : null;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(bufferOptions != null);
                    if (bufferOptions != null) {
                        out.writeInt(bufferOptions.initialBufferSize);
                        out.writeInt(bufferOptions.maxBufferSize);
                        out.writeLong(bufferOptions.maxBufferTimeDepth);
                        out.writeBoolean(bufferOptions.lossless);
                    }
                    out.flush();
                }
                break;

            case TickStreamProperties.DATA_VERSION:
                long dataVersion = stream != null ? stream.getDataVersion() : -1;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeLong(dataVersion);
                    out.flush();
                }
                break;

            case TickStreamProperties.REPLICA_VERSION:
                long replicaVersion = stream != null ? stream.getReplicaVersion() : -1;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeLong(replicaVersion);
                    out.flush();
                }
                break;

            case TickStreamProperties.DF:
                int df = stream != null ? stream.getDistributionFactor() : 0;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeInt(df);
                    out.flush();
                }
                break;

            case TickStreamProperties.SCOPE:
                int scope = stream != null ? stream.getScope().ordinal() : -1;
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeInt(scope);
                    out.flush();
                }
                break;
        }

        resetState(key, property);
    }

    private void writePropertyHeader(String key, int property, long serial) throws IOException {
        out.writeInt(TDBProtocol.STREAM_PROPERTY);
        out.writeLong(serial);
        out.writeUTF(key);
        out.writeByte(property);
    }

    private void marshalAndWrite(Object o, DataOutputStream out) throws IOException {
        propertyBuffer.reset();
        marshall(o, propertyBuffer);
        out.writeInt(propertyBuffer.size());
        propertyBuffer.writeTo(out);
        propertyBuffer.reset();
    }

    private StreamState     getState(String key) {
        synchronized (states) {
            return states.get(key);
        }
    }

    private void resetState(String key, int property) {
        synchronized (states) {
            StreamState state = states.get(key);
            if (state != null) {
                state.reset(property);
            }
        }
    }

    @Override
    public void run() throws InterruptedException {
        if (controlTask != null)
            throw new IllegalStateException("Already started");

        processEventsTask.submit();

        controlTask = new ControlTask(contextContainer.getQuickExecutor());
        controlTask.submit();

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier)db).addStreamStateListener(this);
    }

    @Override
    public void sendKeepAlive() throws IOException {
        if(isClosed)
            throw new IOException("Connection is closed");
    }

    private void        closeAll () {
        //TickDBServer.LOGGER.info("Closing server session: " + ds);

        if (!markClosed()) {
            return;
        }

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier) db).removeStreamStateListener(this);

        try {
            socket.close();
        } catch (IOException e) {
            HTTPProtocol.LOGGER.log(Level.WARNING, "Session: Closing error:", e);
        }

        if (controlTask != null)
            controlTask.unschedule();
        processEventsTask.unschedule();

        String id = guid.toString();

        // clear all stream locks
        DXTickStream[] streams = db.listStreams();
        for (DXTickStream stream : streams) {
            if (stream instanceof ServerStreamWrapper)
                stream = ((ServerStreamWrapper)stream).getNestedInstance();

            if (stream instanceof TickStreamImpl)
                ((TickStreamImpl) stream).clearLocks(id);
        }

        // TODO: MODULARIZATION

//        if (GlobalQuantServer.MAC != null)
//            GlobalQuantServer.MAC.connected(user, ds.getRemoteAddress());

        //TickDBServer.LOGGER.info("Closing db session");
    }

    private synchronized boolean markClosed() {
        if (isClosed) {
            return false;
        }

        isClosed = true;
        return true;
    }

    @Override
    public void changed(DXTickStream stream, int property) {
        String key = stream.getKey();

        StreamState state = getState(key);

        if (state != null && state.set(property)) {
            submitEvent(new PropertyChangedEvent(key, property));
        }
    }

    private void sendChanged(String key, int property) {
        try {
            synchronized (out) {
                out.writeInt(STREAM_PROPERTY_CHANGED);
                out.writeUTF(key);
                out.writeByte(property);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed())
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(" + property + ") failed.", e);
        }
    }

    @Override
    public void renamed(DXTickStream stream, String oldKey) {
        String key = stream.getKey();

        synchronized (states) {
            StreamState state = states.remove(oldKey);
            states.put(key, state);
        }

        if (getState(key) != null) {
            submitEvent(new RenamedActionEvent(key, oldKey));
        }
    }

    private void sendRenamed(String key, String oldKey) {
        try {
            synchronized (out) {
                out.writeInt(STREAM_RENAMED);
                out.writeUTF(oldKey);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed())
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification (STREAM_RENAMED) failed.", e);
        }
    }

    @Override
    public void writerCreated(DXTickStream stream, IdentityKey[] ids) {
        if (clientVersion < CLIENT_SESSION_HANDLER_SUPPORT) {
            return;
        }

        submitEvent(new WriterActionEvent(stream.getKey(), true, ids));
    }

    private void sendWriterCreated(String key, IdentityKey[] ids) {
        try {
            synchronized (out) {
                writePropertyHeader(key, TickStreamProperties.WRITER_CREATED, -1);
                TDBProtocol.writeInstrumentIdentities(ids, out);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(WRITER_CREATED) failed.", e);
            }
        }
    }

    @Override
    public void writerClosed(DXTickStream stream, IdentityKey[] ids) {
        if (clientVersion < CLIENT_SESSION_HANDLER_SUPPORT) {
            return;
        }

        submitEvent(new WriterActionEvent(stream.getKey(), false, ids));
    }

    private void sendWriterClosed(String key, IdentityKey[] ids) {
        try {
            synchronized (out) {
                writePropertyHeader(key, TickStreamProperties.WRITER_CLOSED, -1);
                TDBProtocol.writeInstrumentIdentities(ids, out);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(WRITER_CLOSED) failed.", e);
            }
        }
    }

    @Override
    public void created(DXTickStream stream) {
        submitEvent(new ChangeEvent(EventType.CREATED, stream.getKey()));
    }

    private void sendCreated(String key) {
        try {
            synchronized (out) {
                out.writeInt(STREAM_CREATED);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed())
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification (STREAM_CREATED) failed.", e);
        }
    }

    @Override
    public void deleted(DXTickStream stream) {
        String key = stream.getKey();

        synchronized (states) {
            states.remove(key);
        }

        submitEvent(new ChangeEvent(EventType.DELETED, key));
    }

    private void sendDeleted(String key) {
        try {
            synchronized (out) {
                out.writeInt(STREAM_DELETED);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (!socket.isClosed())
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification (STREAM_DELETED) failed.", e);
        }
    }

    public void             onStreamsReloaded() {
        try {
            synchronized (states) {
                states.clear();
            }

            synchronized (out) {
                out.writeInt(STREAMS_CHANGED);
                out.flush();
            }

        } catch (IOException e) {
            if (!socket.isClosed())
                HTTPProtocol.LOGGER.log(Level.FINE, "Session notification (SECURITY_CONTROLLER_RELOADED) failed.", e);
        }
    }
}