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
package com.epam.deltix.qsrv.hf.tickdb.http.rest;

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

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.marshallUHF;
import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.marshall;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

public class SessionHandler extends RestHandler implements StreamStateListener {

    private final Socket            socket;
    private final DataInput         din;
    private final DataOutputStream  out;
    private final DXTickDB          db;
    private final GUID              guid;
    private final ContextContainer contextContainer;

    private final ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx(8192);
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

    private final Runnable reloadListener = new Runnable() {
        @Override
        public void run() {
            onStreamsReloaded();
        }
    };

    public SessionHandler(DXTickDB db, Socket socket, InputStream input, OutputStream output, ContextContainer contextContainer) throws IOException {
        super(contextContainer.getQuickExecutor());
        this.db = db;
        this.socket = socket;
        this.out = new DataOutputStream(output);
        this.contextContainer = contextContainer;

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

            case REQ_GET_STREAM_PROPERTY:
                processGetProperty(); break;

            case REQ_CLOSE_SESSION:
                closeAll();
                return -1;
        }

        return 0;
    }

    private void processGetStreams() throws IOException {

        long serial = din.readLong();
        int count = din.readInt();

        List<DXTickStream> streams;

        if (count == 0)
            streams = Arrays.asList(db.listStreams());
        else {
            streams = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                DXTickStream stream = db.getStream(din.readUTF());

                // protocol may ask for non-existent streams
                if (stream != null)
                    streams.add(stream);
            }
        }

        writeStreams(buffer, streams);

        synchronized (out) {
            out.writeInt(STREAMS_DEFINITION);
            out.writeLong(serial);
            out.writeInt(buffer.size());
            buffer.writeTo(out);
            buffer.reset();
            out.flush();
        }
    }

    /*
        Returns number of valid stream written into buffer
     */
    private void        writeStreams(ByteArrayOutputStreamEx buffer, List<DXTickStream> streams) throws IOException {
        StringWriter writer = new StringWriter();

        HashMap<String, StreamDef> data = new HashMap<String, StreamDef>();

        for (DXTickStream stream : streams) {
            try {
                String key = stream.getKey();

                StreamDef streamDef = new StreamDef(stream.getStreamOptions());

                writer.getBuffer().setLength(0);
                marshallUHF(stream.getStreamOptions().getMetaData(), writer);
                streamDef.metadata = writer.getBuffer().toString();

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

    private void writePropertyHeader(String key, int property, long serial) throws IOException {
        out.writeInt(TDBProtocol.STREAM_PROPERTY);
        out.writeLong(serial);
        out.writeUTF(key);
        out.writeByte(property);
    }

    private void processGetProperty() throws IOException {
        long serial = din.readLong();

        String key = din.readUTF();
        int property = din.readByte();

        DXTickStream stream = db.getStream(key);

        //TickDBServer.LOGGER.info("getProperty(" + key + ", "  + property + ")");

        switch (property) {
            case TickStreamProperties.NAME:
                String name = stream.getName();
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(name != null ? name : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.DESCRIPTION:
                String description = stream.getDescription();
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(description != null ? description : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.PERIODICITY:
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(stream.getPeriodicity().toString());
                    out.flush();
                }
                break;

            case TickStreamProperties.SCHEMA:
                boolean polymorphic = stream.isPolymorphic();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeBoolean(polymorphic);
                    RecordClassSet rcs = stream.getStreamOptions().getMetaData();
                    marshallUHF(rcs, out);
                    out.flush();
                }
                break;

            case TickStreamProperties.ENTITIES:
                IdentityKey[] ids = stream.listEntities();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    marshall(new ListEntitiesResponse(ids), out);
                    out.flush();
                }
                break;

            case TickStreamProperties.TIME_RANGE:
                long[] range = stream.getTimeRange();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    if (range != null)
                        marshall(new TimeRange(range[0], range[1]), out);
                    else
                        marshall(new TimeRange(Long.MIN_VALUE, Long.MAX_VALUE), out);

                    out.flush();
                }
                break;

            case TickStreamProperties.BG_PROCESS:
                BackgroundProcessInfo process = stream.getBackgroundProcess();

                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    marshall(process, out);
                    out.flush();
                }
                break;

            case TickStreamProperties.OWNER:
                String owner = stream.getOwner();
                synchronized (out) {
                    writePropertyHeader(key, property, serial);
                    out.writeUTF(owner != null ? owner : "<NULL>");
                    out.flush();
                }
                break;
        }

        getState(key).reset(property);
    }

    private StreamState     getState(String key) {
        synchronized (states) {
            return states.get(key);
        }
    }

    @Override
    public void run() throws InterruptedException {
        if (controlTask != null)
            throw new IllegalStateException("Already started");

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier)db).addStreamStateListener(this);

        controlTask = new ControlTask(contextContainer.getQuickExecutor());
        controlTask.submit();
    }

    @Override
    public void sendKeepAlive() throws IOException {
        if(isClosed)
            throw new IOException("Connection is closed");
    }

    private void        closeAll () {
        //TickDBServer.LOGGER.info("Closing server session: " + ds);
        if(!isClosed) {
            isClosed = true;
            if (db instanceof StreamStateNotifier)
                ((StreamStateNotifier) db).removeStreamStateListener(this);

            try {
                socket.close();
            } catch (IOException e) {
                HTTPProtocol.LOGGER.log(Level.WARNING, "Session: Closing error:", e);
            }

            if (controlTask != null)
                controlTask.unschedule();

            String id = guid.toString();

            // clear all stream locks
            DXTickStream[] streams = db.listStreams();
            for (DXTickStream stream : streams)
                ((TickStreamImpl) stream).clearLocks(id);
        }
        // TODO: MODULARIZATION
//        if (GlobalQuantServer.MAC != null)
//            GlobalQuantServer.MAC.connected(user, ds.getRemoteAddress());

        //TickDBServer.LOGGER.info("Closing db session");
    }

    @Override
    public void changed(DXTickStream stream, int property) {
        String key = stream.getKey();

        StreamState state = getState(key);

        if (state != null && state.set(property)) {
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
    }

    @Override
    public void renamed(DXTickStream stream, String oldKey) {
        String key = stream.getKey();

        synchronized (states) {
            StreamState state = states.remove(oldKey);
            states.put(key, state);
        }

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

    }

    @Override
    public void writerClosed(DXTickStream stream, IdentityKey[] ids) {

    }

    @Override
    public void created(DXTickStream stream) {
        String key = stream.getKey();
//        synchronized (states) {
//            states.put(stream.getKey(), new StreamState());
//        }

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
