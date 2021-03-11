package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.unicast;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.TypeSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.CursorCommandProcessor;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.DownloadHandler;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.UserLogger;
import com.epam.deltix.qsrv.hf.tickdb.impl.IdleStrategyProvider;
import com.epam.deltix.qsrv.hf.tickdb.impl.InternalByteArrayInputStream;
import com.epam.deltix.qsrv.hf.tickdb.impl.InternalByteArrayOutputStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.CursorException;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.security.DataFilter;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;
import io.aeron.*;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.security.Principal;
import java.util.logging.Level;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeIdentityKey;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.*;

/**
 * @author Alexei Osipov
 */
class AeronDownloadTask implements Runnable {
    private static final boolean DEBUG_COMM = DownloadHandler.DEBUG_COMM;
    private static final boolean DEBUG_COMM_EVERY_MSG = DownloadHandler.DEBUG_COMM_EVERY_MSG;

    //        private final Aeron aeron;
//        private final int aeronDataStreamId;
//        private final int aeronCommandStreamId;
    private final ExclusivePublication publication;
    private final Subscription subscription;
    private final IdleStrategy idleStrategy;
    private final InstrumentMessageSource cursor;
    private final DataFilter<RawMessage> filter;
    private final boolean binary;
    private final Principal user;
    private final String remoteAddress;
    private final String remoteApplication;
    private final VSChannel channel;
    private final DXTickDB db;

    // Output buffers
    //private final ExpandableArrayBuffer outBuffer;
    //private int outBufferPos = 0;
    private final DataOutputStream dataOutputAdapter;
    private final InternalByteArrayOutputStream dataOutputAdapterBackingStream; // TODO: Apply custom buffer here

    private final MemoryDataOutput mdo = new MemoryDataOutput (256);
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer(mdo.getBuffer(), 0, 0);

    private volatile boolean stopped = false;

    private long lastCommandSerial = 0;

    private int lastLoadedStreamIndex = -1;
    private int lastLoadedEntityIndex = -1;
    private int lastLoadedTypeIndex = -1;
    private long lastAcknowledgedSerial = 0;
    private long sequence = 1; // for debugging
    private long lastTime = Long.MIN_VALUE;

    private boolean publicationFailures = false;

    private final TypeSet typeSet;

    private final FragmentAssembler commandFragmentHandler = new FragmentAssembler(new CommandHandler(this));
    private final CursorCommandProcessor commandProcessor;

    public AeronDownloadTask(Aeron aeron, int aeronDataStreamId, int aeronCommandStreamId, InstrumentMessageSource cursor, DataFilter<RawMessage> filter, boolean binary, Principal user, ChannelPerformance channelPerformance, VSChannel channel, DXTickDB db, @Nullable TickCursor tcursor) {
        this.cursor = cursor;
        this.filter = filter;
        this.binary = binary;
        this.user = user;
        this.remoteAddress = channel.getRemoteAddress();
        this.remoteApplication = channel.getRemoteApplication();
//            this.aeron = aeron;
//            this.aeronDataStreamId = aeronDataStreamId;
//            this.aeronCommandStreamId = aeronCommandStreamId;
        this.publication = aeron.addExclusivePublication(AeronDownloadHandler.CHANNEL, aeronDataStreamId);
        this.subscription = aeron.addSubscription(AeronDownloadHandler.CHANNEL, aeronCommandStreamId);
        this.idleStrategy = IdleStrategyProvider.getIdleStrategy(channelPerformance);
        //this.outBuffer = new ExpandableArrayBuffer(outBufferSize); //new UnsafeBuffer(ByteBuffer.allocateDirect(outBufferSize));
        this.dataOutputAdapterBackingStream = new InternalByteArrayOutputStream();
        this.dataOutputAdapter = new DataOutputStream(dataOutputAdapterBackingStream);

        this.typeSet = new TypeSet(new TypeSender(cursor, dataOutputAdapter));
        this.channel = channel;
        this.channel.setAvailabilityListener(new DisconnectListener());
        this.db = db;
        this.commandProcessor = new CursorCommandProcessor(cursor, tcursor, user, this.remoteAddress, this.remoteApplication);
    }

    @Override
    public void run() {
        try {
            //Publication publication = this.publication;
            //Subscription subscription = this.subscription;

            while (!stopped) {
                boolean processedCommands = processCommands();
                boolean trySend = !processedCommands || !stopped; // Don't re-check "stopped flag" if we have not processed any messages
                boolean sentMessages = trySend && sendMessages();

                if (processedCommands || sentMessages || stopped) {
                    idleStrategy.reset();
                } else {
                    idleStrategy.idle();
                }
            }
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        cursor.setAvailabilityListener(null);
        cursor.close();

        subscription.close();
        publication.close();

        channel.setAvailabilityListener (null);
        channel.close(false);
    }

    private boolean sendMessages() {
        int messagesSent = 0;
        int messagesChecked = 0;
        publicationFailures = false;
        try {
            while (messagesSent < 100 && messagesChecked <= 10_000 && !publicationFailures) {
                boolean         hasNext = false;
                boolean         newSerial = false;

                Throwable       exception;

                try {
                    hasNext = cursor.next();
                    messagesChecked += 1;

                    // filter message
                    if (hasNext && filter != null) {
                        try {
                            if (!filter.accept((RawMessage) cursor.getMessage())) {
                                continue;
                            }
                        } catch (Throwable ex) {
                            TickDBServer.LOGGER.warning("Error while filtering message:" + ex);
                        }
                    }

                    if (lastCommandSerial > lastAcknowledgedSerial) {
                        lastAcknowledgedSerial = lastCommandSerial;
                        newSerial = true;
                    }

                    exception = null;
                } catch (CursorIsClosedException x) {
                    stopped = true;
                    break;
                } catch (UnavailableResourceException x) {
                    // No more messages
                    break;
                } catch (CursorException x) {
                    hasNext = true;
                    exception = x;
                } catch (Throwable x) {
                    exception = x;
                }

                if (exception != null) {
                    sendError(exception);
                } else {
                    if (newSerial) {
                        sendAck();
                    }

                    if (hasNext) {
                        sendMessageWithMetadata();
                        messagesSent += 1;
                    } else {
                        sendEndOfCursor();
                    }
                }

                if (!hasNext) {
                    break;
                }
            }
        } catch (PublicationClosedException x) {
            TickDBServer.LOGGER.log(Level.FINE, this.cursor + ": publication closed unexpectedly", x);
            stopped = true;
        }
        return messagesSent > 0;
    }

    private void sendMessageWithMetadata() {
        RawMessage msg = (RawMessage) cursor.getMessage();

        //System.out.println(msg);

        int streamIndex = cursor.getCurrentStreamIndex();
        int entityIndex = cursor.getCurrentEntityIndex();

        if (streamIndex > 0xFFFF) {
            throw new RuntimeException("streamIndex too big: " + streamIndex);
        }

        //
        //  Send CURRESP_LOAD_STREAM if new stream
        //
        if (streamIndex > lastLoadedStreamIndex) {
            sendNewStreamData(streamIndex);
        }
        //
        //  Send CURRESP_LOAD_ENTITY if new entity
        //
        if (entityIndex > lastLoadedEntityIndex) {
            sendNewEntityData(msg, entityIndex);
        }
        //
        //  Send CURRESP_LOAD_TYPE
        //
        int typeIndex = typeSet.getIndexOfConcreteTypeNoAdd(msg.type);
        if (typeIndex < 0)
            typeIndex = sendNewTypeData(msg.type);

        sendMessageData(msg, streamIndex, entityIndex, typeIndex);
    }

    private void sendMessageData(RawMessage msg, int streamIndex, int entityIndex, int typeIndex) {
        lastTime = msg.getTimeStampMs();

        if (TDBProtocol.USE_TIME_CODEC_FOR_AERON) {
            throw new UnsupportedOperationException();
        }

        int payloadLength = Long.BYTES // Time
                + Short.BYTES // Stream
                + Integer.BYTES // Entity
                + Byte.BYTES // Type
                + msg.length; // Message data

        int lengthWithHeaders = Byte.BYTES // Code
                + payloadLength;

        if (USE_MAGIC) {
            lengthWithHeaders += 2 * Byte.BYTES;
        }
        if (SEND_SEQUENCE) {
            lengthWithHeaders += Long.BYTES;
        }

        boolean partitionedMessage = lengthWithHeaders > publication.maxMessageLength();

        //
        //  On to the message body
        //
        mdo.reset ();

        mdo.writeByte(partitionedMessage ? TDBProtocol.CURRESP_MSG_MULTIPART_HEAD : TDBProtocol.CURRESP_MSG);
        if (partitionedMessage) {
            // Length with magic/sequence headers but without partial message size
            mdo.writeInt(lengthWithHeaders);
        }


        if (USE_MAGIC) {
            mdo.writeByte(35);
            mdo.writeByte(214);
        }

        if (SEND_SEQUENCE) {
            sequence++;
            mdo.writeLong(sequence);
        }
        int headerSize = mdo.getSize();

        if (TDBProtocol.USE_TIME_CODEC_FOR_AERON) {
            TimeCodec.writeTime(msg, mdo);
        } else {
            mdo.writeLong(msg.getNanoTime());
        }

        mdo.writeUnsignedShort (streamIndex);
        mdo.writeInt (entityIndex);
        mdo.writeUnsignedByte (typeIndex);
        mdo.write (msg.data, msg.offset, msg.length);

        int fullSize = mdo.getSize();

        int sizeFieldLength = Integer.BYTES;
        assert fullSize == lengthWithHeaders + (partitionedMessage ? sizeFieldLength : 0);
        int messageSize = fullSize - headerSize;
        assert payloadLength == messageSize; // It's actually same thing just computed different way. Both variables are left for now to perform self checks.

        // Note: we not send size because this is handled by aeron
        //MessageSizeCodec.write (size, bout);


        if (DEBUG_COMM && DEBUG_COMM_EVERY_MSG) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " SEND MESSAGE size=" + messageSize + " #" + lastAcknowledgedSerial + " SEQ #" + sequence + "; ts=" + msg.getTimeStampMs());
        }

        //bout.write (mdo.getBuffer (), 0, size);

        unsafeBuffer.wrap(mdo.getBuffer(), 0, fullSize);
        if (partitionedMessage) {
            executeMultipartPublicationBlocking(unsafeBuffer, (byte) TDBProtocol.CURRESP_MSG_MULTIPART_BODY, mdo.getBuffer(), fullSize);
        } else {
            executePublicationBlocking(unsafeBuffer);
        }
    }

    private void sendNewStreamData(int streamIndex) {
        if (streamIndex != lastLoadedStreamIndex + 1) {
            throw new RuntimeException(
                    "streamIndex jumped " + lastLoadedStreamIndex +
                            " --> " + streamIndex
            );
        }

        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " SEND CURRESP_LOAD_STREAM");
        }

        dataOutputAdapterBackingStream.reset();

        dataOutputAdapterBackingStream.write(CURRESP_LOAD_STREAM);
        //bout.write (CURRESP_LOAD_STREAM);

        String csk = cursor.getCurrentStreamKey();

        if (csk == null) {
            throw new RuntimeException("null csk");
        }

        // TODO: Memory allocation here! Try to avoid.
        try {
            dataOutputAdapter.writeUTF(csk);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //size += outBuffer.putStringUtf8(size, csk);
        //dout.writeUTF (csk);

        lastLoadedStreamIndex = streamIndex;

        executePublicationBlocking(dataOutputAdapterBackingStream);
    }

    private void sendNewEntityData(RawMessage msg, int entityIndex) {
        if (entityIndex != lastLoadedEntityIndex + 1)
            throw new RuntimeException (
                    "SERVER: " + cursor + "entityIndex jumped " + lastLoadedEntityIndex + " --> " + entityIndex
            );

        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SEND CURRESP_LOAD_ENTITY: " + entityIndex);
        }

        dataOutputAdapterBackingStream.reset();

        dataOutputAdapterBackingStream.write(CURRESP_LOAD_ENTITY);
        //outBuffer.putByte(0, (byte) CURRESP_LOAD_ENTITY);
        //int size = 1;
        //bout.write (CURRESP_LOAD_ENTITY);


        try {
            writeIdentityKey(msg, dataOutputAdapter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //int bytesFromDataOutput = dataOutputAdapterBackingStream.size();
        //outBuffer.putBytes(size, dataOutputAdapterBackingStream.getInternalBuffer(), 0, bytesFromDataOutput);
        //size += bytesFromDataOutput;

        lastLoadedEntityIndex = entityIndex;

        executePublicationBlocking(dataOutputAdapterBackingStream);
    }

    private int sendNewTypeData(RecordClassDescriptor classDescriptor) {
        dataOutputAdapterBackingStream.reset();
        final int typeIndex;
        try {
            typeIndex = typeSet.getIndexOfConcreteType(classDescriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (typeIndex > 0xFF) {
            throw new RuntimeException("typeIndex too big: " + typeIndex);
        }

        if (typeIndex != lastLoadedTypeIndex + 1) {
            throw new RuntimeException(
                    "typeIndex jumped " + lastLoadedTypeIndex +
                            " --> " + typeIndex
            );
        }

        lastLoadedTypeIndex = typeIndex;

        executePublicationBlocking(dataOutputAdapterBackingStream);

        return typeIndex;
    }

    private void executePublicationBlocking(InternalByteArrayOutputStream dataOutputAdapterBackingStream) {
        unsafeBuffer.wrap(dataOutputAdapterBackingStream.getInternalBuffer(), 0, dataOutputAdapterBackingStream.size());
        executePublicationBlocking(unsafeBuffer);
    }

    private void executePublicationBlocking(UnsafeBuffer outBuffer) {
        idleStrategy.reset();
        // Send buffer
        while (true) {
            long result = publication.offer(outBuffer);
            if (result < 0) {
                processPublicationError(result);
            } else {
                break;
            }
        }
    }

    private void executeMultipartPublicationBlocking(UnsafeBuffer outBuffer, byte messagePartHeader, byte[] buffer, int length) {
        int partHeaderSize = Byte.BYTES;
        int sendOffset = 0;
        int sendLength = publication.maxMessageLength();
        idleStrategy.reset();
        // Send buffer
        //int fragmentCount = 0;
        while (sendOffset + partHeaderSize < length) {
            long result = publication.offer(outBuffer, sendOffset, sendLength);
            if (result < 0) {
                processPublicationError(result);
            } else {
                sendOffset += sendLength - partHeaderSize; // Step one byte back so we have space for header byte
                if (sendOffset + sendLength > length) {
                    // Last part of message
                    sendLength = length - sendOffset;
                }
                buffer[sendOffset] = messagePartHeader;
                //fragmentCount ++;
            }
        }
        assert sendOffset + partHeaderSize == length;
    }

    private void processPublicationError(long result) {
        // TODO: We should investigate why we get disconnect event from DisconnectListener but do not get
        if (result == Publication.CLOSED || stopped) {
            throw PublicationClosedException.INSTANCE;
        } else if (result == Publication.BACK_PRESSURED || result == Publication.NOT_CONNECTED || result == Publication.ADMIN_ACTION) {
            publicationFailures = true;
            idleStrategy.idle();
        } else {
            throw new RuntimeException("Unknown exception code: " + result);
        }
    }

    private void            sendEndOfCursor () {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SENDING EOC at #" + lastAcknowledgedSerial);
        }
        dataOutputAdapterBackingStream.reset();

        dataOutputAdapterBackingStream.write (CURRESP_END_OF_CURSOR);

        executePublicationBlocking(dataOutputAdapterBackingStream);
    }

    private void            sendAck () {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info("SERVER: " + cursor + " SENDING CURRESP_ACK_SERIAL #" + lastAcknowledgedSerial);
        }

        dataOutputAdapterBackingStream.reset();

        try {
            dataOutputAdapter.write(CURRESP_ACK_SERIAL);
            dataOutputAdapter.writeLong(lastAcknowledgedSerial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executePublicationBlocking(dataOutputAdapterBackingStream);
    }

    private void            sendError (Throwable x) {
        if (DEBUG_COMM) {
            TickDBServer.LOGGER.info ("SERVER: " + cursor + " SENDING CURRESP_ERROR (" + x + ") #" + lastAcknowledgedSerial);
        }

        TickDBServer.LOGGER.log(Level.WARNING, "Error while reading " + cursor, x);

        dataOutputAdapterBackingStream.reset();

        dataOutputAdapterBackingStream.write (CURRESP_ERROR);
        writeException(dataOutputAdapter, x);

        executePublicationBlocking(dataOutputAdapterBackingStream);
    }

    private void        writeException (DataOutputStream dataOutputAdapter, Throwable x) {
        try {
            if (binary) {
                TDBProtocol.writeBinary(dataOutputAdapter, x);
            } else {
                TDBProtocol.writeError(dataOutputAdapter, x);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean processCommands() {
        return subscription.poll(commandFragmentHandler, 1000) > 0;
    }

    //private final InternalByteArrayInputStream inputArray = new InternalByteArrayInputStream();

    private class CommandHandler implements FragmentHandler {
        private final AeronDownloadTask parent;


        private final ExpandableArrayBuffer inputBuffer = new ExpandableArrayBuffer();
        private final InternalByteArrayInputStream byteIs = new InternalByteArrayInputStream(inputBuffer.byteArray());
        private final DataInputStream din = new DataInputStream(byteIs);

        // TODO: Refactor for GFLogger
        //private final StringBuilder sb = new StringBuilder(); // for logging

        private CommandHandler(AeronDownloadTask parent) {
            this.parent = parent;
        }

        @Override
        public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
            if (parent.stopped) {
                return;
            }
            short code = buffer.getShort(offset, ByteOrder.BIG_ENDIAN);
            offset += Short.BYTES;
            length -=  Short.BYTES;
            setupDataInputStream(buffer, offset, length);
            processCommand(code);
            assert byteIs.available() == 0 : "Some data was left in buffer";
        }

        private void setupDataInputStream(DirectBuffer buffer, int offset, int length) {
            buffer.getBytes(offset, inputBuffer, 0, length);
            byteIs.setBuffer(inputBuffer.byteArray(), 0, length);
            //return din;
        }


        private String getClientId() {
            return parent.channel.getClientId();
        }

        private void processCommand(short code) {
            // TODO: Split out to separate class
            try {
                switch (code) {
                    case CURREQ_DISCONNECT:             processDisconnect(); break;
                    case CURREQ_ADD_STREAMS:            processAddStreams (); break;
                    case CURREQ_REMOVE_STREAMS:         processRemoveStreams (); break;
                    case CURREQ_REMOVE_ALL_STREAMS:     processRemoveAllStreams (); break;
                    case CURREQ_ALL_ENTITIES:           processAllEntities(); break;
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
                    case CURREQ_RESET_TIME:             processResetTime(); break;
                    //case CURREQ_RESET_INSTRUMENTS:      processResetInstruments(); break;

                    default:
                        throw new RuntimeException("Got not implemented command code: " + code);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void processDisconnect() {
            if (DEBUG_COMM) {
                TickDBServer.LOGGER.info("SERVER: " + parent.cursor + " RECEIVED CURREQ_DISCONNECT");
            }

            parent.closeAll();
        }

        private void signalCommandReceived(long serial) {
            parent.lastCommandSerial = serial;
        }

        private void processAddStreams() throws IOException {
            final long serial = din.readLong();
            final long time = din.readLong();
            final DXTickStream[] streams = DownloadHandler.readStreamList(din, getClientId(), db);

            commandProcessor.processAddStreams(serial, time, streams);
            signalCommandReceived(serial);
        }

        private void processRemoveStreams() throws IOException {
            final long serial = din.readLong();
            final DXTickStream[] streams = DownloadHandler.readStreamList(din, getClientId(), db);

            commandProcessor.processRemoveStreams(serial, streams);
            signalCommandReceived(serial);
        }

        private void processRemoveAllStreams() throws IOException {
            final long serial = din.readLong();

            commandProcessor.processRemoveAllStreams(serial);
            signalCommandReceived(serial);
        }

        private void processAllEntities() throws IOException {
            final long serial = din.readLong();
            final long time = din.readLong();

            commandProcessor.processAllEntities(serial, time);
            signalCommandReceived(serial);
        }

        private void processAddEntities() throws IOException {
            final long serial = din.readLong();
            final long time = din.readLong();
            final IdentityKey[] ids = readInstrumentIdentities(din);

            commandProcessor.processAddEntities(serial, time, ids);
            signalCommandReceived(serial);
        }

        private void processAddEntitiesTypes() throws IOException {
            final long serial = din.readLong();
            final long time = din.readLong();
            final IdentityKey[] ids = readInstrumentIdentities(din);
            final String[] types = readNonNullableStrings(din);

            commandProcessor.processAddEntitiesTypes(serial, time, ids, types);
            signalCommandReceived(serial);
        }

        private void processRemoveEntitiesTypes() throws IOException {
            final long serial = din.readLong();
            final IdentityKey[] ids = readInstrumentIdentities(din);
            final String[] types = readNonNullableStrings(din);

            commandProcessor.processRemoveEntitiesTypes(serial, ids, types);
            signalCommandReceived(serial);
        }

        private void processRemoveEntities() throws IOException {
            final long serial = din.readLong();
            final IdentityKey[] ids = readInstrumentIdentities(din);

            commandProcessor.processRemoveEntities(serial, ids);
            signalCommandReceived(serial);
        }

        private void processClearEntities() throws IOException {
            final long serial = din.readLong();

            commandProcessor.processClearEntities(serial);
            signalCommandReceived(serial);
        }

        private void processAllTypes() throws IOException {
            final long serial = din.readLong();

            commandProcessor.processAllTypes(serial);
            signalCommandReceived(serial);
        }

        private void processAddTypes() throws IOException {
            final long serial = din.readLong();
            final String[] names = readNonNullableStrings(din);

            commandProcessor.processAddTypes(serial, names);
            signalCommandReceived(serial);
        }

        private void processSetTypes() throws IOException {
            final long serial = din.readLong();
            final String[] names = readNonNullableStrings(din);

            commandProcessor.processSetTypes(serial, names);
            signalCommandReceived(serial);
        }

        private void processRemoveTypes() throws IOException {
            final long serial = din.readLong();
            final String[] names = readNonNullableStrings(din);

            commandProcessor.processRemoveTypes(serial, names);
            signalCommandReceived(serial);
        }

        private void processResetTime() throws IOException {
            final long serial = din.readLong();
            final long time = din.readLong();

            commandProcessor.processResetTime(serial, time);
            signalCommandReceived(serial);
        }
    }

    private static class TypeSender implements TypeSet.TypeSender {
        private final InstrumentMessageSource cursor;
        private final DataOutputStream dataOutput;

        public TypeSender(InstrumentMessageSource cursor, DataOutputStream dataOutputAdapter) {
            this.cursor = cursor;
            this.dataOutput = dataOutputAdapter;
        }

        @Override
        public DataOutputStream begin() throws IOException {
            if (DEBUG_COMM) {
                TickDBServer.LOGGER.info("SERVER: " + cursor + " SEND CURRESP_LOAD_TYPE");
            }

            dataOutput.write(CURRESP_LOAD_TYPE);
            return dataOutput;
        }

        @Override
        public void end() {
            if (DEBUG_COMM) {
                TickDBServer.LOGGER.info("SERVER: " + cursor + " END OF CURRESP_LOAD_TYPE");
            }
        }

        @Override
        public int version() {
            return TDBProtocol.VERSION;
        }
    }

    private static class PublicationClosedException extends RuntimeException {
        static final PublicationClosedException INSTANCE = new PublicationClosedException();
    }

    /// COMMAND PROCESSING
    private void closeAll() {
        stopped = true;

        // cursorLock is used only for subscription events
        cursor.setAvailabilityListener (null);
        cursor.close ();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, remoteAddress, remoteApplication, UserLogger.CLOSE_CURSOR_PATTERN, cursor, lastTime);
        }
    }

    private class DisconnectListener implements Runnable {
        @Override
        public void run() {
            if (channel.getState() != VSChannelState.Connected) {
                stopped = true;
            }
        }
    }
}
