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

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.StreamState;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.ByteArrayOutputStreamEx;
import com.epam.deltix.util.security.SecurityReloadListener;
import com.epam.deltix.util.security.SecurityReloadNotifier;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.*;
import java.util.logging.Level;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;


/**
 *
 */
public class SessionHandler implements StreamStateListener {

    private final VSChannel         ds;
    private final DXTickDB          db;
    private final DataInputStream   din;
    private final DataOutputStream  out;

    private final ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx(8192);
    private final DataOutputStream      dout = new DataOutputStream(buffer);

    private final Map<String, StreamState> states = new HashMap<>();

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
                    if (ds.getState() == VSChannelState.Closed)
                        break;

                    if (din.available () == 0)
                        return;

                    processCommand ();
                }
            } catch (EOFException iox) {
                // valid close
                closeAll ();
            } catch (IOException iox) {
                TickDBServer.LOGGER.log (Level.INFO, "Exception in " + toString() + " Remote address: " + ds.getRemoteAddress(), iox);

                closeAll ();
            }
        }
    }
    private ControlTask controlTask;
    private final int   clientVersion;

    private final SecurityReloadListener reloadListener = new SecurityReloadListener() {
        @Override
        public void reloaded() {
            onStreamsReloaded();
        }
    };

    public SessionHandler(VSChannel ds, DXTickDB db, QuickExecutor exe, int clientVersion) {
        this.ds = ds;
        this.db = db;

        this.din = ds.getDataInputStream();
        this.out = ds.getDataOutputStream();

        this.controlTask = new ControlTask(exe);
        this.clientVersion = clientVersion;
        ds.setAvailabilityListener(controlTask.avlnr);
        controlTask.submit();

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier) db).addStreamStateListener(this);

        if (db instanceof SecurityReloadNotifier)
            ((SecurityReloadNotifier) db).addReloadListener(reloadListener);

        //TickDBServer.LOGGER.info("Starting db session");
    }

    private void        processCommand () throws IOException {
        int                 code = din.readInt ();

        switch (code) {
            case REQ_GET_STREAMS:           processGetStreams(); break;
            case REQ_GET_STREAM_PROPERTY:   processGetProperty(); break;
            case REQ_CLOSE_SESSION:         closeAll(); break;
        }
    }

    private void processGetStreams() throws IOException {

        int count = din.readInt();

        List<DXTickStream> streams;

        if (count == 0)
            streams = Arrays.asList(db.listStreams());
        else {
            streams = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                DXTickStream stream = null;
                String name = din.readUTF();
                try {
                    stream = db.getStream(name);
                } catch (AccessControlException e) {
                    TickDBServer.LOGGER.log(Level.WARNING, "Can not access stream '" + name + "'");
                }

                // protocol may ask for non-existent (or non-accessible?) streams
                if (stream != null)
                    streams.add(stream);
            }
        }

        int size = writeStreams(buffer, streams);

        synchronized (out) {
            out.writeInt(STREAMS_DEFINITION);
            out.writeInt(size);
            buffer.writeTo(out);
            buffer.reset();
            out.flush();
        }
    }

    /*
        Returns number of valid stream written into buffer
     */
    private int     writeStreams(ByteArrayOutputStreamEx buffer, List<DXTickStream> streams) throws IOException {
        int valid = 0;

        for (DXTickStream stream : streams) {
            int pos = buffer.size();
            try {
                // has access to the stream?
                String key = stream.getKey();

                TDBProtocol.writeStream(dout, stream, clientVersion);
                valid++;

                synchronized (states) {
                    StreamState state = getState(key);
                    if (state == null)
                        state = new StreamState();
                    state.set(false);
                    states.put(key, state);
                }
            } catch (Throwable e) {
                buffer.reset(pos);
                TickDBServer.LOGGER.log(Level.WARNING, "Session: sending stream " + stream + " definition failed.", e);
            }
        }

        return valid;
    }

    @Override
    public void             writerCreated(DXTickStream stream, IdentityKey[] ids) {
        String key = stream.getKey();

        if (getState(key) == null)
            return;

        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.STREAM_PROPERTY);
                out.writeUTF(key);
                out.writeByte(TickStreamProperties.WRITER_CREATED);

                if (ids == null)
                    out.writeInt (-1);
                else {
                    out.writeInt (ids.length);
                    for (IdentityKey id : ids)
                        TDBProtocol.writeIdentityKey(id, out);
                }

                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(WRITER_CREATED) failed.", e);
        }
    }

    @Override
    public void             writerClosed(DXTickStream stream, IdentityKey[] ids) {
        String key = stream.getKey();
        if (getState(key) == null)
            return;

        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.STREAM_PROPERTY);
                out.writeUTF(key);
                out.writeByte(TickStreamProperties.WRITER_CLOSED);

                if (ids == null)
                    out.writeInt (-1);
                else {
                    out.writeInt (ids.length);
                    for (IdentityKey id : ids)
                        TDBProtocol.writeIdentityKey(id, out);
                }

                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(WRITER_CLOSED) failed.", e);
        }
    }

    private void writePropertyHeader(String key, int property) throws IOException {
        out.writeInt(TDBProtocol.STREAM_PROPERTY);
        out.writeUTF(key);
        out.writeByte(property);
    }

    private void processGetProperty() throws IOException {
        String key = din.readUTF();
        int property = din.readByte();

        //todo: what if stream not exists or can't be accessed
        DXTickStream stream = db.getStream(key);

        //TickDBServer.LOGGER.info("getProperty(" + key + ", "  + property + ")");

        switch (property) {
            case TickStreamProperties.NAME:
                String name = stream != null ? stream.getName() : null;

                synchronized (out) {
                    writePropertyHeader(key, property);
                    out.writeUTF(name != null ? name : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.DESCRIPTION:
                String description = stream != null ? stream.getDescription() : null;

                synchronized (out) {
                    writePropertyHeader(key, property);
                    out.writeUTF(description != null ? description : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.PERIODICITY:
                Periodicity p = stream != null ? stream.getPeriodicity() : null;

                synchronized (out) {
                    writePropertyHeader(key, property);
                    out.writeUTF(p != null ? p.toString() : "<NULL>");
                    out.flush();
                }
                break;

            case TickStreamProperties.SCHEMA:
                StreamOptions options = stream != null ? stream.getStreamOptions() : new StreamOptions();

                synchronized (out) {
                    writePropertyHeader(key, property);
                    out.writeBoolean(options.isPolymorphic());
                    writeClassSet(out, options.getMetaData());
                    out.flush();
                }
                break;

            case TickStreamProperties.ENTITIES:
                IdentityKey[] ids = stream != null ? stream.listEntities() : null;
                buffer.reset();
                TDBProtocol.writeTimeRange(dout, stream, ids);

                synchronized (out) {
                    writePropertyHeader(key, property);
                    buffer.writeTo(out);
                    out.flush();
                    buffer.reset();
                }
                break;

            case TickStreamProperties.TIME_RANGE:
                long[] range = stream != null ? stream.getTimeRange() : null;

                synchronized (out) {
                    writePropertyHeader(key, property);
                    writeTimeRange(range, out);
                    out.flush();
                }
                break;

            case TickStreamProperties.BG_PROCESS:
                BackgroundProcessInfo process = stream != null ? stream.getBackgroundProcess() : null;

                synchronized (out) {
                    writePropertyHeader(key, property);
                    writeBGProcessInfo(process, out);
                    out.flush();
                }
                break;

            case TickStreamProperties.OWNER:
                String owner = stream != null ? stream.getOwner() : null;
                synchronized (out) {
                    writePropertyHeader(key, property);
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

    private void        closeAll () {
        //TickDBServer.LOGGER.info("Closing server session: " + ds);

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier) db).removeStreamStateListener(this);

        if (db instanceof SecurityReloadNotifier)
            ((SecurityReloadNotifier) db).removeReloadListener(reloadListener);

        if (ds.getState() == VSChannelState.Connected) {
            try {
                synchronized (out) {
                    out.writeInt (SESSION_CLOSED);
                    out.flush ();
                }
            } catch (IOException iox) {
                TickDBServer.LOGGER.log (Level.WARNING, "Error disconnecting from server - ignored.", iox);
            }
        }

        if (controlTask != null)
            controlTask.unschedule ();

        ds.setAvailabilityListener (null);
        ds.close();

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
                    out.writeInt(TDBProtocol.STREAM_PROPERTY_CHANGED);
                    out.writeUTF(key);
                    out.writeByte(property);
                    out.flush();
                }
            } catch (IOException e) {
                if (ds.getState() != VSChannelState.Closed)
                    TickDBServer.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(" + property + ") failed.", e);
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
                out.writeInt(TDBProtocol.STREAM_RENAMED);
                out.writeUTF(oldKey);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification (STREAM_RENAMED) failed.", e);
        }
    }

    @Override
    public void created(DXTickStream stream) {
        String key = stream.getKey();
//        synchronized (states) {
//            states.put(stream.getKey(), new StreamState());
//        }

        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.STREAM_CREATED);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification (STREAM_CREATED) failed.", e);
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
                out.writeInt(TDBProtocol.STREAM_DELETED);
                out.writeUTF(key);
                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification (STREAM_DELETED) failed.", e);
        }
    }

    public void             onStreamsReloaded() {
        try {
            synchronized (states) {
                states.clear();
            }

            synchronized (out) {
                out.writeInt(TDBProtocol.STREAMS_CHANGED);
                out.flush();
            }

        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification (SECURITY_CONTROLLER_RELOADED) failed.", e);
        }
    }
}