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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import com.epam.deltix.qsrv.hf.tickdb.comm.StreamState;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.security.SecurityReloadListener;
import com.epam.deltix.util.security.SecurityReloadNotifier;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.*;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;


/**
 *
 */
public class SessionHandler implements StreamStateListener, SecurityReloadListener {

    private static final int DEFAULT_SIZE = 256 * 1024; // 256K

    public static class ByteArrayStream extends ByteArrayOutputStream {

        private final byte[] defaultBuffer;
        /**
         * Creates a new byte array output stream, with a buffer capacity of
         * the specified size, in bytes.
         *
         * @param   size   the initial size. will be used to shrink() method to trim buffer to initial size
         * @exception  IllegalArgumentException if size is negative.
         */
        public ByteArrayStream (int size) {
            super (size);

            defaultBuffer = new byte[size];
        }

        public byte []          getInternalBuffer () {
            return (buf);
        }

        /**
         *  Resets the current position. Seeking forward will lead to arbitrary
         *  data being placed in the buffer.
         */
        public void             reset (int position) {
            ensureCapacity (position);
            count = position;
        }

        /// Clear all data and shrink to the initial size
        public void shrink() {

            if (buf.length != defaultBuffer.length)
                buf = defaultBuffer;

            count = 0;
        }

        /**
         *  Make sure buffer capacity is at least the specified size.
         */
        public void             ensureCapacity (int n) {
            if (n > buf.length) {
                byte[] newbuf = new byte [Math.max (buf.length << 1, n)];
                System.arraycopy (buf, 0, newbuf, 0, count);
                buf = newbuf;
            }
        }
    }

    private static final Log LOG = LogFactory.getLog(SessionHandler.class);

    public static class ChangeAction {
        public final String       stream;
        public final ChangeType   type;

        public ChangeAction(String stream, ChangeType type) {
            this.stream = stream;
            this.type = type;
        }
    }

    public static class PropertyChanged extends ChangeAction {

        public PropertyChanged(String stream, int property) {
            super(stream, ChangeType.PROPERTY_CHANGED);
            this.property = property;
        }

        public final int property;
    }

    public static class WriterAction extends ChangeAction {

        public WriterAction(String stream, boolean created, IdentityKey[] ids) {
            super(stream, created ? ChangeType.WRITER_CREATED : ChangeType.WRITER_CLOSED);
            this.ids = ids;
        }
        public final IdentityKey[]     ids;
    }

    public static class RenamedAction extends ChangeAction {
        public final String oldKey;

        public RenamedAction(String stream, String previous) {
            super(stream, ChangeType.RENAMED);
            this.oldKey = previous;
        }
    }

    public enum ChangeType {
        RENAMED, CREATED, DELETED, WRITER_CLOSED, WRITER_CREATED, PROPERTY_CHANGED, RELOAD
    }

    private final Principal         user;
    private final VSChannel         ds;
    private final DXTickDB          db;
    private final DataInputStream   din;
    private final DataOutputStream  out;

    private final SecurityContext   context;

    private final ByteArrayStream buffer = new ByteArrayStream(DEFAULT_SIZE);
    private final DataOutputStream      dout = new DataOutputStream(buffer);

    private final LinkedList<ChangeAction>  actions = new LinkedList<>();

    private final ObjectToObjectHashMap<String, StreamState> states = new ObjectToObjectHashMap<>();

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

                    processActions();

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
    private final ControlTask controlTask;
    private final int   clientVersion;

    public SessionHandler(Principal user, VSChannel ds, DXTickDB db, SecurityContext context, QuickExecutor exe, int clientVersion) {
        this.user = user;
        this.ds = ds;
        this.db = db;
        this.context = context;

        this.din = ds.getDataInputStream();
        this.out = ds.getDataOutputStream();

        this.controlTask = new ControlTask(exe);
        this.clientVersion = clientVersion;
        ds.setAvailabilityListener(controlTask.avlnr);
        controlTask.submit();

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier) db).addStreamStateListener(this);

        if (db instanceof SecurityReloadNotifier)
            ((SecurityReloadNotifier) db).addReloadListener(this);

        //TickDBServer.LOGGER.info("Starting db session");
    }

    private void        processActions() {

        while (true) {
            ChangeAction next;
            synchronized (actions) {
                next = actions.poll();
            }

            if (next == null)
                break;

            // process actions
            switch (next.type) {
                case RENAMED:
                    sendRenamed(next.stream, ((RenamedAction)next).oldKey);
                    break;
                case CREATED:
                    sendCreated(next.stream);
                    break;
                case DELETED:
                    sendDeleted(next.stream);
                    break;
                case WRITER_CLOSED:
                    sendWriterClosed(next.stream, ((WriterAction)next).ids);
                    break;
                case WRITER_CREATED:
                    sendWriterCreated(next.stream, ((WriterAction)next).ids);
                    break;
                case PROPERTY_CHANGED:
                    sendChanged(next.stream, ((PropertyChanged)next).property);
                    break;
                case RELOAD:
                    sendReloaded();
                    break;
            }

        }
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
                    LOG.warn("User %s cannot access stream '%s'").with(user.getName()).with(name);
                }

                // protocol may ask for non-existent (or non-accessible?) streams
                if (stream != null)
                    streams.add(stream);
            }
        }

        if (clientVersion < 116) {
            int size = writeStreams(buffer, streams, 0, streams.size());

            synchronized (out) {
                out.writeInt(STREAMS_DEFINITION);
                out.writeInt(size);
                buffer.writeTo(out);
                buffer.shrink();
                out.flush();
            }
        } else {
            // support chunks, 20 streams per chunk
            int step = 20;

            int offset = 0;
            while (offset < streams.size()) {
                int size = writeStreams(buffer, streams, offset, step);

                synchronized (out) {
                    out.writeInt(STREAMS_DEFINITION);
                    out.writeInt(size);
                    buffer.writeTo(out);
                    buffer.shrink();
                    out.flush();
                }

                offset += step;
            }

            synchronized (out) {
                out.writeInt(END_STREAMS_DEFINITION);
                out.flush();
            }
        }
    }

    /*
        Returns number of valid stream written into buffer
     */
    private int     writeStreams(ByteArrayStream buffer, List<DXTickStream> streams, int offset, int length) throws IOException {
        int valid = 0;
        int size = Math.min(streams.size(), offset + length);

        for (int i = offset; i < size; i++) {
            DXTickStream stream = streams.get(i);
            int pos = buffer.size();
            String key = stream.getKey();

            try {
                // has access to the stream?

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
                LOG.warn("Session: sending stream %s definition failed. Error: %s").with(key).with(e);
            }
        }

        return valid;
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
                    writeClassSet(out, options.getMetaData(), clientVersion);
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
                    buffer.shrink();
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

            case TickStreamProperties.HIGH_AVAILABILITY:
                boolean availability = stream != null && stream.getHighAvailability();
                synchronized (out) {
                    writePropertyHeader(key, property);
                    out.writeBoolean(availability);
                    out.flush();
                }
                break;

            default:
                LOG.warn("Stream " + key + "' processing for property = (" + property + ") is not supported");
                return;

        }

        getState(key).reset(property);
    }

    private StreamState     getState(String key) {
        synchronized (states) {
            return states.get(key, null);
        }
    }

    private void        closeAll () {
        //TickDBServer.LOGGER.info("Closing server session: " + ds);

        if (db instanceof StreamStateNotifier)
            ((StreamStateNotifier) db).removeStreamStateListener(this);

        if (db instanceof SecurityReloadNotifier)
            ((SecurityReloadNotifier) db).removeReloadListener(this);

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

//        if (context!= null && context.ac != null)
//            context.ac.disconnected(user, ds.getRemoteApplication(), ds.getClientAddress());

//        if (GlobalQuantServer.MAC != null)
//            GlobalQuantServer.MAC.connected(user, ds.getRemoteAddress());

        //TickDBServer.LOGGER.info("Closing db session");
    }

    @Override
    public void             writerCreated(DXTickStream stream, IdentityKey[] ids) {
        String key = stream.getKey();

        if (getState(key) != null)
            submitAction(new WriterAction(key, true, ids));
    }

    private void            sendWriterCreated(String key, IdentityKey[] ids) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.STREAM_PROPERTY);
                out.writeUTF(key);
                out.writeByte(TickStreamProperties.WRITER_CREATED);
                TDBProtocol.writeInstrumentIdentities(ids, out);

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
        if (getState(key) != null)
            submitAction(new WriterAction(key, false, ids));
    }

    private void            sendWriterClosed(String key, IdentityKey[] ids) {
        try {
            synchronized (out) {
                out.writeInt(TDBProtocol.STREAM_PROPERTY);
                out.writeUTF(key);
                out.writeByte(TickStreamProperties.WRITER_CLOSED);
                TDBProtocol.writeInstrumentIdentities(ids, out);
                out.flush();
            }
        } catch (IOException e) {
            if (ds.getState() != VSChannelState.Closed)
                TickDBServer.LOGGER.log(Level.FINE, "Session notification STREAM_PROPERTY_CHANGED(WRITER_CLOSED) failed.", e);
        }
    }

    @Override
    public void             changed(DXTickStream stream, int property) {
        String key = stream.getKey();
        StreamState state = getState(key);

        if (state != null && state.set(property))
            submitAction(new PropertyChanged(key, property));
    }

    private void            sendChanged(String key, int property) {
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

    @Override
    public void     renamed(DXTickStream stream, String oldKey) {
        String key = stream.getKey();

        synchronized (states) {
            StreamState state = states.get(oldKey, null);
            if (states.remove(oldKey))
                states.put(key, state);
        }

        if (getState(key) != null)
            submitAction(new RenamedAction(key, oldKey));
    }

    private void        sendRenamed(String key, String oldKey) {
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

    private void submitAction(ChangeAction action) {
        synchronized (actions) {
            actions.add(action);
        }

        controlTask.submit();
    }

    @Override
    public void         created(DXTickStream stream) {
        submitAction(new ChangeAction(stream.getKey(), ChangeType.CREATED));
    }

    private void        sendCreated(String key) {
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
    public void         deleted(DXTickStream stream) {
        String key = stream.getKey();

        submitAction(new ChangeAction(key, ChangeType.DELETED));
    }

    private void        sendDeleted(String key) {
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

    @Override
    public void             reloaded() {
        synchronized (states) {
            states.clear();
        }

        submitAction(new ChangeAction(null, ChangeType.RELOAD));
    }

    private void            sendReloaded() {
        try {
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