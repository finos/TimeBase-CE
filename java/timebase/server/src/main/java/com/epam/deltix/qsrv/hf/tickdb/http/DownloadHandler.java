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
package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeIdentityKey;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.concurrent.Signal;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.http.download.ChangeAction;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.io.FlushableOutputStream;
import org.apache.catalina.connector.ClientAbortException;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorArray;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.io.LittleEndianDataOutputStream;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeConstants;
import com.epam.deltix.util.time.TimeKeeper;
import org.owasp.encoder.Encode;

import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.*;

/**
 *
 */
public abstract class DownloadHandler <T extends SelectRequest> extends AbstractHandler implements Runnable {
    private static final boolean    DEBUG_COMM = Boolean.getBoolean("timebase.http.download.debug");

    private static final LongToObjectHashMap<DownloadHandler> instances = new LongToObjectHashMap<DownloadHandler>();

    private static final long PING_PERIODICITY = 5 * TimeConstants.SECOND; // 5 seconds
    private static long seed = TimeKeeper.currentTime % 100000;

    protected final DXTickDB db;
    protected final T request;
    private final HttpServletResponse response;

    private int lastLoadedEntityIndex = -1;
    private int lastLoadedTypeIndex = -1;
    private int lastLoadedStreamIndex = -1;

    private final TypeTransmission typeTransmission;

    // contains types, which were sent already
    private final RecordClassSet recordClassSet = new RecordClassSet();
    private final ObjectArrayList<RecordClassDescriptor> concreteTypes = new ObjectArrayList<>();

    private FlushableOutputStream fout;
    private DataOutput            dout;
    //private OutputStream          os;
    private GZIPOutputStream        gzip_os;


    private volatile boolean wasClientAbort = false;
    private InstrumentMessageSource cursor;

    private final long id;

    private long        commandSerial = 0;
    private long        sentSerial = 0; // last command send to the client

    // buffer for message, guarded by writeLock
    //private MemoryDataOutput  buffer = new MemoryDataOutput();

    private boolean isWithinMessageBlock = false;

    private final Object cursorLock = new Object();
    private final Object writeLock = new Object();
    protected final Signal signal = new Signal();

    public static synchronized DownloadHandler   getInstance(long id) {
        return instances.get(id, null);
    }

    public static synchronized boolean           removeInstance(long id) {
        return instances.remove(id);
    }

    public static synchronized DownloadHandler getInstance(DXTickDB db, SelectRequest request, HttpServletResponse response) {
        DownloadHandler handler = new DownloadHandlerSelect(db, request, response);
        instances.put(handler.id, handler);
        return handler;
    }

    public static synchronized DownloadHandler getInstance(DXTickDB db, QQLRequest request, HttpServletResponse response) {
        DownloadHandler handler = new DownloadHandlerQQLSelect(db, request, response);
        instances.put(handler.id, handler);
        return handler;
    }

    public DownloadHandler(DXTickDB db, T request, TypeTransmission typeTransmission, HttpServletResponse response) {
        this.db = db;
        this.request = request;
        this.response = response;
        this.typeTransmission = typeTransmission;

        this.id = nextSequence();
    }

    private long            nextSequence() {
        synchronized (this.getClass()) {
            return seed++;
        }
    }

    public long             getId() {
        return id;
    }

    protected abstract InstrumentMessageSource createSource();

    @Override
    public void run() {
        try {
            final OutputStream os;

            fout = new FlushableOutputStream(response.getOutputStream());

            if (request.useCompression) {
                response.setHeader(HTTPProtocol.CONTENT_ENCODING, HTTPProtocol.GZIP);
                gzip_os = new GZIPOutputStream(fout, 0x1000, true);
                os = gzip_os;
            } else {
                os = fout;
            }

            dout = request.isBigEndian ? new DataOutputStream(os) : new LittleEndianDataOutputStream(os);

            cursor = createSource();

            synchronized (writeLock) {
                dout.write(HTTPProtocol.CURSOR_BLOCK_ID);
                dout.writeLong(id);
            }

            flushInternal();

            for (;;) {
                try {
                    process();
                } catch (UnavailableResourceException e) {
                    // ignore
                } finally {
                    writeKeepAlive(); // make sure that data will be flushed immediately
                    while (!signal.await(5000)) {
                        writeKeepAlive();
                    }
                }

                if (cursor.isClosed())
                    break;
            }
        } catch (CursorIsClosedException e) {
            // valid case
        }
        catch (Throwable t) {
            try {
                if (t instanceof AccessControlException) {
                    if (!response.isCommitted())
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, Encode.forHtml(t.getMessage()));

                    throw (AccessControlException) t;
                } else if (t instanceof RuntimeException) {
                    if (response.isCommitted()) {
                        writeError(HTTPProtocol.ERR_INVALID_ARGUMENTS, t.getMessage());
                    } else {
                        sendError(response, t);
                        throw (RuntimeException) t;
                    }
                } else if (t instanceof ClientAbortException) {
                    wasClientAbort = true;
                    LOGGER.warning(String.format("[%s] Client disconnected: %s",
                            Thread.currentThread().getName(), t.getCause().getMessage()));
                } else if (!wasClientAbort || !(t instanceof CursorIsClosedException)) {
                    LOGGER.log(Level.SEVERE, "TB-HTTP query processing failed", t);
                    writeError(HTTPProtocol.ERR_PROCESSING, Util.printStackTrace(t));
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failed to send an error block", t);
            }
        } finally {
            Util.close(cursor);

            if (!wasClientAbort) {



                try {
                    // must empty GZIP buffer before flush to keep chunk valid for a GZIP client
                    if (gzip_os != null)
                        gzip_os.finish();

                    flushInternal();

                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "failed to flush output stream", e);
                }
            }
        }
    }

    private void        flushInternal() throws IOException {

        if (gzip_os != null)
            gzip_os.flush();
        else
            fout.flush();
    }

    long        reset(long time) throws IOException, InterruptedException {
        long serial;

        synchronized (cursorLock) {
            serial = ++commandSerial;

            if (DEBUG_COMM)
                LOGGER.info ("SERVER: " + cursor + " reset #" + serial + " at " + time);

            cursor.reset(time);
        }

        signal.set();
        return serial;
    }

    long        changeEntities(long time, ChangeAction action, IdentityKey[] ids) throws InterruptedException, IOException {

        long serial;

        synchronized (cursorLock) {
            serial = ++commandSerial;

            if (action == ChangeAction.ADD) {
                if (time != TimeConstants.TIMESTAMP_UNKNOWN)
                    cursor.setTimeForNewSubscriptions(time);

                if (ids == null) {
                    cursor.subscribeToAllEntities();
                    if (DEBUG_COMM)
                        LOGGER.info ("SERVER: " + cursor + " subscribeToAllEntities() #" + serial + " at " + time);
                }
                else if (ids.length > 0) {
                    cursor.addEntities(ids, 0, ids.length);
                }
            } else if (action == ChangeAction.SET) {
                cursor.clearAllEntities();

                if (ids != null && ids.length > 0)
                    cursor.addEntities(ids, 0, ids.length);

            } else if (action == ChangeAction.REMOVE) {
                if (ids == null)
                    cursor.clearAllEntities();
                else if (ids.length > 0)
                    cursor.removeEntities(ids, 0, ids.length);
            }
        }

        signal.set();
        return serial;
    }

    long        changeTypes(ChangeAction action, String[] types) throws InterruptedException, IOException {
        long serial;

        synchronized (cursorLock) {

            serial = ++commandSerial;

            if (action == ChangeAction.ADD) {
                if (types == null)
                    cursor.subscribeToAllTypes();
                else if (types.length > 0)
                    cursor.addTypes(types);
            } else if (action == ChangeAction.SET) {
                if (types == null)
                    cursor.setTypes();
                else
                    cursor.setTypes(types);
            } else if (action == ChangeAction.REMOVE) {
                if (types == null)
                    cursor.setTypes();
                else if (types.length > 0)
                    cursor.removeTypes(types);
            }
        }

        signal.set();
        return serial;
    }

    long        changeStreams(ChangeAction action, DXTickStream[] streams) throws InterruptedException, IOException {

        long serial;

        synchronized (cursorLock) {

            serial = ++commandSerial;

            if (action == ChangeAction.ADD) {
                if (streams != null && streams.length > 0)
                    cursor.addStream(streams);
            } else if (action == ChangeAction.REMOVE) {
                if (streams == null)
                    cursor.removeAllStreams();
                else if (streams.length > 0)
                    cursor.removeStream(streams);
            }
        }

        signal.set();
        return serial;
    }

    private boolean process() throws IOException, InterruptedException {
        for (;;) {
            try {
                fout.disableFlushing();

                synchronized (cursorLock) {
                    boolean hasNext = cursor.next();

                    synchronized (writeLock) {

                        if (commandSerial != sentSerial) {
                            while (sentSerial < commandSerial)
                                writeCommandBlock(++sentSerial);
                        }

                        if (hasNext) {
                            writeMessage((RawMessage) cursor.getMessage());
                        } else {
                            endMessageBlock();
                            writeTerminatorBlock();
                            return false;
                        }
                    }
                }
            } finally {
                fout.enableFlushing();
            }
        }
    }

    private boolean writeMessage(RawMessage raw) throws IOException, InterruptedException {
        if (raw.getTimeStampMs() > request.to || raw.getTimeStampMs() < request.from)
            return true;

        // send new type
        final int typeIndex = writeTypeBlock(raw.type);
        if (typeIndex > 0xFF)
            throw new RuntimeException("typeIndex too big: " + typeIndex);

        if (typeIndex > lastLoadedTypeIndex) {
            if (typeIndex != lastLoadedTypeIndex + 1)
                throw new RuntimeException(
                        "typeIndex jumped " + lastLoadedTypeIndex +
                                " --> " + typeIndex
                );

            lastLoadedTypeIndex = typeIndex;
        }

        // send new instrument
        final int entityIndex = cursor.getCurrentEntityIndex();
        if (entityIndex > lastLoadedEntityIndex) {
            if (entityIndex != lastLoadedEntityIndex + 1)
                throw new RuntimeException(
                        "entityIndex jumped " + lastLoadedEntityIndex +
                                " --> " + entityIndex
                );

            writeInstrumentBlock(raw, entityIndex);
            lastLoadedEntityIndex = entityIndex;
        }

        final int streamIndex = cursor.getCurrentStreamIndex();

        if (streamIndex > lastLoadedStreamIndex) {
            if (streamIndex != lastLoadedStreamIndex + 1)
                throw new RuntimeException(
                        "streamIndex jumped " + lastLoadedEntityIndex +
                                " --> " + streamIndex
                );

            writeStreamBlock(cursor.getCurrentStream(), streamIndex);
            lastLoadedStreamIndex = streamIndex;
        }

        beginMessageBlock();
        writeMessageRecord(raw, typeIndex, entityIndex, streamIndex);
        return false;
    }

    private void beginMessageBlock() throws IOException {
        assert Thread.holdsLock(writeLock);

        if (!isWithinMessageBlock) {
            dout.write(HTTPProtocol.MESSAGE_BLOCK_ID);
            isWithinMessageBlock = true;
        }
    }

    private void endMessageBlock() throws IOException {
        assert Thread.holdsLock(writeLock);

        if (isWithinMessageBlock) {
            dout.writeInt(HTTPProtocol.TERMINATOR_RECORD);
            isWithinMessageBlock = false;
        }
    }

    private void writeCommandBlock(long serial) throws IOException {
        assert Thread.holdsLock(writeLock);

        endMessageBlock();

        dout.write(HTTPProtocol.COMMAND_BLOCK_ID);
        dout.writeLong(serial);
    }

    private void writeInstrumentBlock(RawMessage msg, int entityIndex) throws IOException {
        assert Thread.holdsLock(writeLock);

        endMessageBlock();
        dout.write(HTTPProtocol.INSTRUMENT_BLOCK_ID);
        dout.writeShort(entityIndex);
        writeIdentityKey(msg, dout);
    }

    private void writeMessageRecord(RawMessage raw, int typeIndex, int entityIndex, int streamIndex) throws IOException {
        int msg_size = raw.length;

        msg_size += HTTPProtocol.CURSOR_MESSAGE_HEADER_SIZE;

        if (msg_size >= HTTPProtocol.MAX_MESSAGE_SIZE)
            throw new IllegalStateException("invalid message size " + msg_size);

        dout.writeInt(msg_size);
        dout.writeLong(raw.getNanoTime());
        dout.writeShort(entityIndex);
        dout.writeByte(typeIndex);
        dout.writeByte(streamIndex);
        dout.write(raw.getData(), raw.offset, raw.length);

        if (DEBUG_COMM)
            LOGGER.log(Level.INFO, "Send message: " + raw.getSymbol() + ";" + raw.getTimeString());
    }

    private void writeStreamBlock(TickStream stream, int index) throws IOException {
        dout.write(HTTPProtocol.STREAM_BLOCK_ID);
        dout.writeByte(stream != null ? index : -1);
        if (stream != null)
            dout.writeUTF(stream.getKey());
    }

    private int writeTypeBlock(RecordClassDescriptor type) throws IOException {
        int typeIndex = concreteTypes.indexOf(type);

        if (typeIndex == -1) {
            endMessageBlock();
            dout.write(HTTPProtocol.TYPE_BLOCK_ID);
            typeIndex = concreteTypes.size();
            dout.writeShort(typeIndex);
            concreteTypes.add(type);

            switch (typeTransmission) {
                case GUID:
                    dout.writeUTF(type.getGuid());
                    break;
                case NAME:
                    dout.writeUTF(type.getName());
                    break;
                case DEFINITION:
                    final ClassDescriptorArray cda = new ClassDescriptorArray(type, recordClassSet);
                    recordClassSet.addContentClasses(type);

                    final String xml = marshallUHF(cda);
                    assert xml.length() > 0;
                    dout.writeUTF(xml);
                    break;
                default:
                    throw new IllegalStateException("invalid typeTransmission=" + typeTransmission);
            }
        }

        return typeIndex;
    }

    private void writeTerminatorBlock() throws IOException {
        dout.write(HTTPProtocol.TERMINATOR_BLOCK_ID);
    }

    private void writeError(byte code, String msg) throws IOException {
        synchronized (writeLock) {
            endMessageBlock();

            dout.write(HTTPProtocol.ERROR_BLOCK_ID);
            dout.write(code);
            dout.writeUTF(msg);
        }
    }

    void        close() {
        Util.close(cursor);
        signal.set();
    }

    private long lastPingTimestamp = 0;

    private void writeKeepAlive() throws IOException {

        synchronized (writeLock) {
            long ts = TimeKeeper.currentTime;
            if (ts - lastPingTimestamp > PING_PERIODICITY) {
                endMessageBlock();
                dout.write(HTTPProtocol.PING_BLOCK_ID);
                lastPingTimestamp = ts;
            }
        }

        // flush data anyway
        flushInternal();
    }

    private static class DownloadHandlerSelect extends DownloadHandler<SelectRequest> {

        private DownloadHandlerSelect(DXTickDB db, SelectRequest request, HttpServletResponse response) {
            super(db, request, request.typeTransmission,  response);
        }

        @Override
        protected InstrumentMessageSource createSource() {
            final TickStream[] streams = new TickStream[request.streams.length];
            for (int i = 0; i < streams.length; i++) {
                streams[i] = db.getStream(request.streams[i]);
                if (streams[i] == null)
                    throw new UnknownStreamException(String.format("stream '%s' doesn't exist", request.streams[i]));
            }

            final SelectionOptions so = new SelectionOptions(true, request.live, request.reverse);
            so.allowLateOutOfOrder = request.allowLateOutOfOrder;
            so.realTimeNotification = request.realTimeNotification;

            InstrumentMessageSource cursor = db.select(request.reverse ? request.to : request.from, so, request.types, request.symbols, streams);


            cursor.setAvailabilityListener(new Runnable() {
                @Override
                public void run() {
                    signal.set();
                }
            });

            //HTTPProtocol.LOGGER.log(Level.INFO, "Stream cursor " + cursor + ": " + System.currentTimeMillis() + ": created.");

            return cursor;
        }
    }

    private static class DownloadHandlerQQLSelect extends DownloadHandler<QQLRequest> {

        private Parameter[] params;

        private DownloadHandlerQQLSelect(DXTickDB db, QQLRequest request, HttpServletResponse response) {
            super(db, request, TypeTransmission.DEFINITION,  response);

            if (request.parameters != null) {
                params = new Parameter[request.parameters.length];
                for (int i = 0; i < params.length; i++)
                    params[i] = request.parameters[i].toParameter();
            }
        }

        @Override
        protected InstrumentMessageSource createSource() {
            InstrumentMessageSource source = null;

            if (params != null)
                source = db.executeQuery(request.qql, new SelectionOptions(), null, request.symbols, request.from, params);
            else
                source = db.executeQuery(request.qql, new SelectionOptions(), null, request.symbols, request.from);

            source.setAvailabilityListener(new Runnable() {
                @Override
                public void run() {
                    signal.set();
                }
            });

            return source;
        }
    }
}
