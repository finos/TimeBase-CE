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

import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.security.DataFilter;
import com.epam.deltix.util.vsocket.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.*;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

import java.io.*;
import java.security.Principal;
import java.util.logging.Level;


public class DownloadHandler {
    public static final boolean    DEBUG_COMM = Boolean.getBoolean("TimeBase.debugComm");
    public static final boolean    DEBUG_COMM_EVERY_MSG = Boolean.getBoolean("TimeBase.debugCommEveryMsg");

    static class QueryCompilationFailed extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace () {
            return (null);
        }
    }

    private final DXTickDB                  db;
    private final VSChannel                 ds;
    private final InstrumentMessageSource   cursor;
    private final MemoryDataOutput          mdo = new MemoryDataOutput (256);
    private final VSOutputStream            bout;
    private final DataInputStream           din;
    private final TypeSet.TypeSender        typeSender =
        new TypeSet.TypeSender () {
            public DataOutputStream         begin () throws IOException {
                if (DEBUG_COMM) {
                    TickDBServer.LOGGER.info ("SERVER: " + cursor + " SEND CURRESP_LOAD_TYPE");
                }

                bout.write (CURRESP_LOAD_TYPE);
                return (dout);
            }

            public void                     end () {
                if (DEBUG_COMM) {
                    TickDBServer.LOGGER.info ("SERVER: " + cursor + " END OF CURRESP_LOAD_TYPE");
                }
            }

            @Override
            public int version() {
                return (clientVersion <= 99 || clientVersion >= 106) ? 0 : 1;
            }
        };
    private final TypeSet                   typeSet = new TypeSet (typeSender);
    private final DataOutputStream          dout;
    private final SelectionOptions          options;

    /**
     *  Guards lastCommandSerial. Cursor control methods and next are
     *  synchronized by this lock.
     */
    private final Object                    cursorLock = new Object ();
    private long                            lastCommandSerial = 0;
    //
    //  Objects representing the state of the client. Guarded by the download
    //  thread.
    //
    //private long                            lastReportedByteLoss = 0;
    //private long                            lastReportedTimeLoss = 0;
    private int                             lastLoadedStreamIndex = -1;
    private int                             lastLoadedEntityIndex = -1;
    private int                             lastLoadedTypeIndex = -1;
    private long                            lastAcknowledgedSerial = 0;
    private long                            sequence = 1; // for debugging
    private long                            lastTime = Long.MIN_VALUE;

    private final StringBuilder             sb = new StringBuilder(); // for logging

    private final class PumpTask extends QuickExecutor.QuickTask {
        final Runnable avlnr = PumpTask.this::submit;

        public PumpTask (QuickExecutor exe) {
            super (exe);            
        }

        @Override
        public String       toString () {
            return ("Pump Task for " + DownloadHandler.this);
        }

        @Override
        public void         run () {
            try {
                for (;;) {
                    bout.disableFlushing ();

                    boolean         hasNext = false;
                    boolean         newSerial = false;

                    try {
                        Throwable       exception;

                        synchronized (cursorLock) {

                            try {
                                hasNext = cursor.next ();

                                // filter message
                                if (hasNext && filter != null) {
                                    try {
                                        if (!filter.accept((RawMessage)cursor.getMessage()))
                                            continue;
                                    } catch (Throwable ex) {
                                        TickDBServer.LOGGER.warning ("Error while filtering message:" + ex);
                                    }
                                }

                                if (lastCommandSerial > lastAcknowledgedSerial) {
                                    lastAcknowledgedSerial = lastCommandSerial;
                                    newSerial = true;
                                }

                                exception = null;
                            } catch (CursorIsClosedException x) {
                                // cursor closes async without using 'cursorLock' to allow returns
                                return;
                            } catch (UnavailableResourceException x) {
                                break;
                            } catch (CursorException x) {
                                hasNext = true;
                                exception = x;
                            } catch (Throwable x) {
                                exception = x;
                            }
                        }

                        if (exception != null)
                            sendError (exception);
                        else {
                            if (newSerial)
                                sendAck ();

                            if (hasNext)
                                sendMessage ();
                            else
                                sendEndOfCursor ();
                        }
                    } finally {
                        bout.enableFlushing ();
                    }

                    if (!hasNext) {
                        bout.flush ();
                        break;
                    }
                }
            } catch (ChannelClosedException x) {
                TickDBServer.LOGGER.log (Level.FINE, DownloadHandler.this.cursor + ": client disconnect", x);
                unschedule();
            } catch (ConnectionAbortedException e) {

                UserLogger.warn(user, ds.getRemoteAddress(), ds.getRemoteApplication(), cursor + " connection dropped unexpectedly", e);

                closeAll ();

            } catch (IOException iox) {

                UserLogger.warn(user, ds.getRemoteAddress(), ds.getRemoteApplication(), cursor + " has an error: ", iox);

                closeAll ();
            }
        }
    }

    private final class ControlTask extends QuickExecutor.QuickTask {

        final Runnable avlnr = ControlTask.this::submit;
        private volatile boolean    stopped = false;

        public ControlTask (QuickExecutor exe) {
            super (exe);
            
        }

        @Override
        public String       toString () {
            return ("Control Task for " + DownloadHandler.this);
        }

        public void         stop() {
            unschedule();
            stopped = true;
        }

        @Override
        public void         run () {
            try {
                for (;;) {
                    if (stopped)
                        break;

                    if (din.available () < 2)
                        return;

                    processCommand ();
                }
            } catch (EOFException iox) {
                // valid close
                closeAll();
            } catch (IOException ex) {
                TickDBServer.LOGGER.log (Level.INFO, "Exception on download control channel", ex);
                closeAll();
            } catch (RuntimeException ex) {
                TickDBServer.LOGGER.log (Level.INFO, "Error processing command.", ex);
                closeAll();
            }
        }
    }

    private final ControlTask           controlTask;
    private final PumpTask              pumpTask;
    private final Principal             user;
    private final DataFilter<RawMessage> filter;
    private final boolean               binary;
    private final int                   clientVersion;

    private final CursorCommandProcessor commandProcessor;

    static void createAndStart(SelectionOptions options,
                               Principal user,
                               VSChannel ds,
                               DXTickDB db,
                               QuickExecutor executor,
                               int clientVersion,
                               DataFilter<RawMessage> filter,
                               DataOutputStream dout,
                               boolean binary,
                               InstrumentMessageSource cursor,
                               TickCursor tcursor) throws IOException {
        new DownloadHandler(options, user, ds, db, cursor, tcursor, filter, binary, dout, clientVersion, executor);
    }

    private DownloadHandler(SelectionOptions options,
                            Principal user,
                            VSChannel ds,
                            DXTickDB db,
                            InstrumentMessageSource cursor,
                            TickCursor tcursor, DataFilter<RawMessage> filter,
                            boolean binary,
                            DataOutputStream dout,
                            int clientVersion,
                            QuickExecutor exe) throws IOException {
        this.options = options;
        this.db = db;
        this.ds = ds;
        this.user = user;
        this.filter = filter;
        this.clientVersion = clientVersion;

        this.bout = ds.getOutputStream();
        this.dout = ds.getDataOutputStream();
        this.din = ds.getDataInputStream();
        this.binary = binary;

        this.cursor = cursor;

        this.commandProcessor = new CursorCommandProcessor(cursor, tcursor, user, ds.getRemoteAddress(), ds.getRemoteApplication());

        dout.writeBoolean (true);  // OK
        writeSelectedTransport(clientVersion, dout, TDBProtocol.TRANSPORT_TYPE_SOCKET);

        dout.flush ();

        ds.setAutoflush(true);
        ds.setNoDelay(options.channelPerformance.isLowLatency());

        controlTask = new ControlTask (exe);
        pumpTask = new PumpTask (exe);

        ds.setAvailabilityListener (controlTask.avlnr);
        cursor.setAvailabilityListener (pumpTask.avlnr);

        controlTask.submit ();
        pumpTask.submit ();
    }

    public static void writeSelectedTransport(int version, DataOutputStream dout, int transportType) throws IOException {
        boolean aeronSupported = version >= TDBProtocol.AERON_SUPPORT_VERSION;
        if (aeronSupported) {
            dout.write(transportType);
        }
    }

    static StringBuilder                  toString(StringBuilder sb, IdentityKey[] ids) {
        sb.append("INSTRUMENTS {");
        if (ids == null) {
            sb.append("<ALL>");
        } else {
            for (int i = 0; i < ids.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(ids[i]);
            }
        }
        sb.append("}");

        return sb;
    }

    static StringBuilder                  toString(StringBuilder sb, CharSequence[] ids) {
        sb.append("INSTRUMENTS {");
        if (ids == null) {
            sb.append("<ALL>");
        } else {
            for (int i = 0; i < ids.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(ids[i]);
            }
        }
        sb.append("}");

        return sb;
    }

    static StringBuilder                  toString(StringBuilder sb, long time, IdentityKey[] ids) {
        sb.append("TIME {").append(time).append("};");
        return toString(sb, ids);
    }

    static StringBuilder                  toString(StringBuilder sb, DXTickStream[] streams) {
        if (streams == null)
            return sb;

        sb.append("STREAMS {");

        for (int i = 0; i < streams.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(streams[i].getKey());
        }
        sb.append("}");

        return sb;
    }

    public static DXTickStream []         readStreamList (
        VSChannel                             ds,
        DXTickDB                              db
    )
        throws IOException, UnknownStreamException
    {
        return readStreamList(ds.getDataInputStream(), ds.getClientId(), db);
    }

    public static DXTickStream [] readStreamList(
            DataInputStream din,
            String clientId,
            DXTickDB db
    )
        throws IOException, UnknownStreamException
    {

        if (din.readBoolean())
            return null;

        short           n = din.readShort ();
        if (n < 0)
            throw new IOException ("Protocol violation");

        DXTickStream []       streams = new DXTickStream [n];

        for (int ii = 0; ii < n; ii++) {
            String              key = din.readUTF ();
            DXTickStream        stream = db.getStream (key);

            if (stream == null)
                throw new UnknownStreamException ("Unknown stream: " + key);

            DBLock lock = RequestHandler.readLock(din, clientId);
            stream.verify(lock, LockType.READ);

            streams [ii] = stream;
        }

        return (streams);
    }

    private void        writeException (Throwable x) throws IOException {
        if (binary)
            TDBProtocol.writeBinary(dout, x);
        else
            TDBProtocol.writeError(dout, x);
    }

    private void        processCommand () throws IOException {
        int                 code = din.readShort ();

        switch (code) {
            case CURREQ_DISCONNECT:             processDisconnect (); break;
            case CURREQ_ADD_STREAMS:            processAddStreams (); break;
            case CURREQ_REMOVE_STREAMS:         processRemoveStreams (); break;
            case CURREQ_REMOVE_ALL_STREAMS:     processRemoveAllStreams (); break;
            case CURREQ_ALL_ENTITIES:           processAllEntities (); break;
            case CURREQ_ADD_ENTITIES:           processAddEntities (); break;
            case CURREQ_REMOVE_ENTITIES:        processRemoveEntities (); break;
            case CURREQ_CLEAR_ENTITIES:         processClearEntities (); break;
            case CURREQ_ALL_TYPES:              processAllTypes (); break;
            case CURREQ_ADD_TYPES:              processAddTypes (); break;
            case CURREQ_ADD_ENTITIES_TYPES:     processAddEntitiesTypes(); break;
            case CURREQ_REMOVE_ENTITIES_TYPES:  processRemoveEntitiesTypes(); break;
            case CURREQ_SET_TYPES:              processSetTypes(); break;
            case CURREQ_REMOVE_TYPES:           processRemoveTypes (); break;
            //case CURREQ_CLEAR_TYPES:            processClearTypes (); break;
            case CURREQ_RESET_TIME:             processResetTime (); break;
            //case CURREQ_RESET_INSTRUMENTS:      processResetInstruments(); break;
        }
    }

    private void        signalCommandReceived (long serial) {
        assert Thread.holdsLock (cursorLock);

        lastCommandSerial = serial;
        pumpTask.submit ();
    }

    private void        closeAll () {

        if (DEBUG_COMM)
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " CLOSING_ALL");

        if (controlTask != null)
            controlTask.stop();

        if (pumpTask != null)
            pumpTask.unschedule ();

        // cursorLock is used only for subscription events
        cursor.setAvailabilityListener (null);
        cursor.close ();

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CLOSE_CURSOR_PATTERN, cursor, lastTime);
       
        ds.setAvailabilityListener (null);
        ds.close (true);
    }

    private void        processDisconnect () {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " RECEIVED CURREQ_DISCONNECT");
        }

        closeAll ();
    }

    private void        processAddStreams () throws IOException {
        final long          serial = din.readLong ();
        final long          time = din.readLong ();
        final DXTickStream [] streams = readStreamList (ds, db);

        synchronized (cursorLock) {
            commandProcessor.processAddStreams(serial, time, streams);
            signalCommandReceived (serial);
        }
    }

    private void        processRemoveStreams () throws IOException {
        final long          serial = din.readLong ();
        final DXTickStream [] streams = readStreamList (ds, db);

        synchronized (cursorLock) {
            commandProcessor.processRemoveStreams(serial, streams);
            signalCommandReceived (serial);
        }
    }

    private void        processRemoveAllStreams () throws IOException {
        final long          serial = din.readLong ();

        synchronized (cursorLock) {
            commandProcessor.processRemoveAllStreams(serial);
            signalCommandReceived (serial);
        }
    }

    private void        processAllEntities () throws IOException {
        final long          serial = din.readLong ();
        final long          time = din.readLong ();

        synchronized (cursorLock) {
            commandProcessor.processAllEntities(serial, time);
            signalCommandReceived (serial);
        }
    }

    private void        processAddEntities () throws IOException {
        final long          serial = din.readLong ();
        final long          time = din.readLong ();
        final IdentityKey [] ids = readInstrumentIdentities (din);

        synchronized (cursorLock) {
            commandProcessor.processAddEntities(serial, time, ids);
            signalCommandReceived (serial);
        }
    }

    private void        processAddEntitiesTypes () throws IOException {
        final long          serial = din.readLong ();
        final long          time = din.readLong ();
        final IdentityKey [] ids = readInstrumentIdentities (din);
        final String []             types = readNonNullableStrings(din);

        synchronized (cursorLock) {
            commandProcessor.processAddEntitiesTypes(serial, time, ids, types);
            signalCommandReceived (serial);
        }
    }

    private void        processRemoveEntitiesTypes () throws IOException {
        final long          serial = din.readLong ();
        final IdentityKey [] ids = readInstrumentIdentities (din);
        final String []             types = readNonNullableStrings (din);

        synchronized (cursorLock) {
            commandProcessor.processRemoveEntitiesTypes(serial, ids, types);
            signalCommandReceived (serial);
        }
    }

    private void        processRemoveEntities () throws IOException {
        final long          serial = din.readLong ();
        final IdentityKey [] ids = readInstrumentIdentities (din);

        synchronized (cursorLock) {
            commandProcessor.processRemoveEntities(serial, ids);
            signalCommandReceived (serial);
        }
    }

    private void        processClearEntities () throws IOException {
        final long          serial = din.readLong ();

        synchronized (cursorLock) {
            commandProcessor.processClearEntities(serial);
            signalCommandReceived (serial);
        }
    }

    private void        processAllTypes () throws IOException {
        final long          serial = din.readLong ();

        synchronized (cursorLock) {
            commandProcessor.processAllTypes(serial);
            signalCommandReceived (serial);
        }
    }

    private void        processAddTypes () throws IOException {
        final long          serial = din.readLong ();
        final String []     names = readNonNullableStrings (din);

        synchronized (cursorLock) {
            commandProcessor.processAddTypes(serial, names);
            signalCommandReceived (serial);
        }
    }

    private void        processSetTypes () throws IOException {
        final long          serial = din.readLong ();
        final String []     names = readNonNullableStrings (din);

        synchronized (cursorLock) {
            commandProcessor.processSetTypes(serial, names);
            signalCommandReceived (serial);
        }
    }

    private void        processRemoveTypes () throws IOException {
        final long          serial = din.readLong ();
        final String []     names = readNonNullableStrings (din);

        synchronized (cursorLock) {
            commandProcessor.processRemoveTypes(serial, names);
            signalCommandReceived (serial);
        }
    }

//    private void        processClearTypes () throws IOException {
//        final long          serial = din.readLong ();
//
//        if (DEBUG_COMM) {
//            TickDBServer.LOGGER.info ("SERVER: " + cursor + " clearTypes #" + serial);
//        }
//
//        synchronized (cursorLock) {
//            cursor.clearAllTypes ();
//            signalCommandReceived (serial);
//        }
//    }

    private void        processResetTime () throws IOException {
        final long          serial = din.readLong ();
        final long          time = din.readLong ();

        synchronized (cursorLock) {
            commandProcessor.processResetTime(serial, time);
            signalCommandReceived (serial);
        }
    }

//    private void        processResetInstruments() throws IOException {
//
//        final long          serial = din.readLong ();
//        final long          time = din.readLong ();
//        IdentityKey[] ids = TDBProtocol.readInstrumentIdentities(din);
//
//        if (DEBUG_COMM) {
//            TickDBServer.LOGGER.info ("SERVER: " + cursor + " reset #" + serial + " at " + time) ;
//        }
//
//        if (UserLogger.canTrace(user)) {
//            sb.setLength(0);
//            toString(sb, time, ids);
//            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.SUBSCRIBE_PATTERN, cursor, "RESET " + sb.toString());
//        }
//
//        synchronized (cursorLock) {
//            tcursor.reset (time, ids);
//            signalCommandReceived (serial);
//        }
//    }

//    private void        processResetTimeAndFilter () throws IOException {
//        final long          serial = din.readLong ();
//        final long          time = din.readLong ();
//        final FeedFilter    filter = FeedFilter.createFromStream (din);
//
//        if (DEBUG_COMM) {
//            TickDBServer.LOGGER.info ("SERVER: " + cursor + " resetTimeAndFilter #" + serial + " at " + time);
//        }
//
//        synchronized (cursorLock) {
//            tcursor.reset (time, filter);
//            signalCommandReceived (serial);
//        }
//    }

    /**
     *  This method is called to pump a ready message from cursor.
     *  cursor.next () should have already been called and returned true.
     */
    private void        sendMessage () throws IOException {
        final RawMessage        msg = (RawMessage) cursor.getMessage ();

        //System.out.println(msg);

        final int               streamIndex = cursor.getCurrentStreamIndex ();
        final int               entityIndex = cursor.getCurrentEntityIndex ();

        if (streamIndex > 0xFFFF)
            throw new RuntimeException ("streamIndex too big: " + streamIndex);
        
        //
        //  Send CURRESP_LOAD_STREAM if new stream
        //
        if (streamIndex > lastLoadedStreamIndex) {
            if (streamIndex != lastLoadedStreamIndex + 1)
                throw new RuntimeException (
                    "streamIndex jumped " + lastLoadedStreamIndex +
                    " --> " + streamIndex
                );

            if (DEBUG_COMM) {
                TickDBServer.LOGGER.info ("SERVER: " + cursor + " SEND CURRESP_LOAD_STREAM");
            }

            bout.write (CURRESP_LOAD_STREAM);

            String  csk = cursor.getCurrentStreamKey ();

            if (csk == null)
                throw new RuntimeException ("null csk");

            dout.writeUTF (csk);

            lastLoadedStreamIndex = streamIndex;
        }
        //
        //  Send CURRESP_LOAD_ENTITY if new entity
        //
        if (entityIndex > lastLoadedEntityIndex) {
            if (entityIndex != lastLoadedEntityIndex + 1)
                throw new RuntimeException (
                        "SERVER: " + cursor + "entityIndex jumped " + lastLoadedEntityIndex + " --> " + entityIndex
                );

            if (DEBUG_COMM) {
                TickDBServer.LOGGER.info ("SERVER: " + cursor + " SEND CURRESP_LOAD_ENTITY: " + entityIndex);
            }

            bout.write (CURRESP_LOAD_ENTITY);
            writeIdentityKey (msg, dout);

            lastLoadedEntityIndex = entityIndex;
        }
        //
        //  Send CURRESP_LOAD_TYPE
        //
        final int   typeIndex = typeSet.getIndexOfConcreteType (msg.type);

        if (typeIndex > 0xFF)
            throw new RuntimeException ("typeIndex too big: " + typeIndex);

        if (typeIndex > lastLoadedTypeIndex) {
            if (typeIndex != lastLoadedTypeIndex + 1)
                throw new RuntimeException (
                    "typeIndex jumped " + lastLoadedTypeIndex +
                    " --> " + typeIndex
                );

            lastLoadedTypeIndex = typeIndex;
        }

        lastTime = msg.getTimeStampMs();

        //
        //  On to the message body
        //
        mdo.reset ();

        TimeCodec.writeTime (msg, mdo);
        mdo.writeUnsignedShort (streamIndex);
        mdo.writeInt (entityIndex);
        mdo.writeUnsignedByte (typeIndex);
        mdo.write (msg.data, msg.offset, msg.length);

        final int         size = mdo.getSize ();

        MessageSizeCodec.write (size, bout);

        if (USE_MAGIC) {
            bout.write (35);
            bout.write (214);
        }

        if (SEND_SEQUENCE) {
            dout.writeLong(++sequence);
        }

        if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " SEND MESSAGE size=" + size + " #" + lastAcknowledgedSerial + " SEQ #" + sequence + "; ts=" + msg.getTimeStampMs());
        }

        bout.write (mdo.getBuffer (), 0, size);
    }

    private void            sendEndOfCursor () throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SENDING EOC at #" + lastAcknowledgedSerial);
        }

        bout.write (CURRESP_END_OF_CURSOR);
    }

    private void            sendAck () throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SENDING CURRESP_ACK_SERIAL #" + lastAcknowledgedSerial);
        }

        bout.write (CURRESP_ACK_SERIAL);
        dout.writeLong (lastAcknowledgedSerial);
    }

    private void            sendError (Throwable x) throws IOException {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SENDING CURRESP_ERROR (" + x + ") #" + lastAcknowledgedSerial);
        }

        TickDBServer.LOGGER.log(Level.WARNING, "Error while reading " + cursor, x);

        bout.write (CURRESP_ERROR);
        writeException(x);
    }

}