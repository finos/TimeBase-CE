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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import static com.epam.deltix.util.vsocket.VSProtocol.*;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.memory.MemoryDataOutput;

import javax.annotation.CheckReturnValue;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 *
 */
final class VSChannelImpl implements VSChannel {
    private final ContextContainer contextContainer;
    private volatile VSChannelState         state = VSChannelState.NotConnected;
    
    private final VSDispatcher              dispatcher;
    private final int                       localId;
    private volatile int                    remoteId = -1;

    // for debug purposes inCapacity and outCapacity can be changed to smaller sizes
    private final int                       inCapacity; // = 1 << 15;
    private final int                       outCapacity; // = 1 << 14;

    private GapQueueInputStream             in;
    private CountingInputStream             cin;
    private DataInputStream                 din;
    
    private ChannelOutputStream             out;
    private DataOutputStream                dout;
    
    private boolean                         autoFlush = false;
    private boolean                         noDelay = false;
    private final byte []                   buffer8 = new byte[12];

    private final int                       index;
    private volatile int                    remoteIndex = -1;

    private volatile Runnable               listener;

    private final QuickExecutor.QuickTask   lnrNotifier;

    private final boolean                   compressed;

    private final Inflater                  inflater;
    private final MemoryDataOutput          infOut;

    private final Deflater                  deflater;
    private final MemoryDataOutput          defOut;

    private volatile long                   numBytesSend; // synchronized by "this"
    private final Counter                   numBytesRead = new Counter();

//    private final StringBuffer              sendLog = new StringBuffer();
//    private final StringBuffer              recievedLog = new StringBuffer();

    private final PriorityQueue<ChannelCommand>   commands = new PriorityQueue<ChannelCommand>(2,
            new Comparator<ChannelCommand>() {
                @Override
                public int compare(ChannelCommand a, ChannelCommand b) {
                    if (a.offset == b.offset)
                        return Util.compare(a.code, b.code);

                    return a.offset > b.offset ? 1 : -1;
                }
            });

    static class Counter {
        private long count = 0;

        public synchronized long increment(long v) {
            return count += v;
        }

        public synchronized long value() {
            return count;
        }
    }

    static abstract class ChannelCommand {

        public long             offset;
        public int              code;

        public ChannelCommand(int code, long offset) {
            this.offset = offset;
            this.code = code;
        }

        public abstract void run();
    }

    public class RemoteClosing extends ChannelCommand {

        public RemoteClosing (long offset) {
            super(VSProtocol.CLOSING, offset);

            onRemoteClosing();
        }

        @Override
        public void run() {
            in.finish();
            notifyDataAvailable();
        }
    }

    public class RemoteClosed extends ChannelCommand {
        public RemoteClosed(long offset) {
            super(VSProtocol.CLOSED, offset);
        }

        @Override
        public void run() {
            onRemoteClosed();
        }
    }
    
    public VSChannelImpl (VSDispatcher dispatcher, int inCapacity, int outCapacity,
                         boolean compressed, int localId, int index, ContextContainer contextContainer) {
        if (inCapacity <= 0)
            throw new IllegalArgumentException("inCapacity");
        if (outCapacity <= 0)
            throw new IllegalArgumentException("outCapacity");

        this.dispatcher = dispatcher;
        this.localId = localId;
        this.index = index;
        this.compressed = compressed;
        this.inCapacity = inCapacity;
        this.outCapacity = outCapacity;

        this.contextContainer = contextContainer;

        inflater = compressed ? new Inflater() : null;
        infOut = compressed ? new MemoryDataOutput(inCapacity) : null;
        deflater = compressed ? new Deflater() : null;
        defOut = compressed ? new MemoryDataOutput(outCapacity) : null;

        this.out = new ChannelOutputStream(this, outCapacity);
        this.in = new GapQueueInputStream (inCapacity);

        this.cin = new CountingInputStream(this.in, inCapacity / 4) {

            @Override
            protected boolean bytesRead(long change) {
                try {
                    sendBytesRead(change);
                    return true;
                } catch (IOException e) {
                    if (!VSChannelImpl.this.in.isClosed())
                        LOGGER.log (Level.WARNING, "Error sending bytes read.", e);
                    return false;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        };

        this.lnrNotifier =
            new QuickExecutor.QuickTask (contextContainer.getQuickExecutor()) {
                @Override
                public void     run () {
                    Runnable        consistentListener = listener;

                    if (consistentListener != null)
                        consistentListener.run ();
                }
            };
    }   

    public int                  getLocalId () {
        return (localId);
    }

    public int                  getRemoteId () {
        return (remoteId);
    }

    public String               getRemoteAddress() {
        return dispatcher != null ? dispatcher.getRemoteAddress() : null;
    }

    public String               getRemoteApplication() {
        return dispatcher != null ? dispatcher.getApplicationID() : null;
    }

    @Override
    public String               getClientId() {
        return dispatcher != null ? dispatcher.getClientId() : null;
    }

    public InputStream          getInputStream () {
        return (cin);
    }

    public DataInputStream      getDataInputStream () {
        if (din == null)
            din = new DataInputStream(getInputStream());

        return (din);
    }

    public VSOutputStream       getOutputStream () {
        return (out);
    }

    @Override
    public DataOutputStream     getDataOutputStream() {
        if (dout == null)
            dout = new DataOutputStream (out);

        return (dout);
    }

    public synchronized VSChannelState  getState() {
        return state;
    }

    public synchronized boolean         isClosed () {
        return state == VSChannelState.Closed || state == VSChannelState.Removed;
    }
    
    private synchronized boolean isRemoteConnected() {
        return state == VSChannelState.Connected;
    }

    public void                 close () {
        close(false);
    }
    
    public void                 close (boolean terminate) {
        //  Must close "out" before grabbing lock on "this", so that
        //  another thread sending data releases its lock on "this",
        /// held in send ().
        Util.close (in);

        //System.out.println(this + " closing having read " + numBytesRead.value());

        if (isRemoteConnected () && !terminate) {
            Util.close (out); // flush all data
        } else {
//            if (out.size() > 0)
//                LOGGER.info(this + ": closing having available bytes=" + out.size());
            out.closeNoFlush(); // do not flush data
        }

        synchronized (this) {
            if (state == VSChannelState.Removed)
                return;

            if (state == VSChannelState.Closed)
                return;

            try {
                // send 'closing' signal only when connected
                if (state != VSChannelState.NotConnected)
                    sendClosing();
                else
                    LOGGER.log(Level.FINE, this + " not connected yet");

                // if remote in 'disconnected' state - send signal to close
                if (state == VSChannelState.RemoteClosed)
                    sendClosed();

            } catch (ConnectionAbortedException x) {
                LOGGER.log (Level.FINE, "Error sending disconnect.", x);
            } catch (InterruptedException x) {
                LOGGER.log (Level.FINE, "Sending disconnect interrupted.", x);
            } catch (Exception x) {
                LOGGER.log (Level.WARNING, "Error sending disconnect", x);
            }

            state = VSChannelState.Closed;
        }
    }

    public void processCommand(int cmd, long position) {
        assert position >= 0;

        ChannelCommand command = null;
        if (cmd == VSProtocol.CLOSING)
            command = new RemoteClosing(position);
        else if (cmd == VSProtocol.CLOSED)
            command = new RemoteClosed(position);

        synchronized (commands) {
            if (command != null)
                commands.offer(command);
        }

//        if (numBytesRead > position)
//            System.out.println(this + ": command (" + cmd + ", " + position + ") is out " + numBytesRead);
//        else if (numBytesRead < position)
//            System.out.println(this + ": command (" + cmd + ", " + position + ") delayed by " + (position - numBytesRead));

        checkCommands();
    }

    private void                checkCommands() {

        long position = numBytesRead.value();

        if (!commands.isEmpty()) {
            ChannelCommand command;

            synchronized (commands) {
                command = commands.peek();
                if (command != null && command.offset == position)
                    command = commands.poll();
            }

            while (command != null && command.offset == position) {
                command.run();
                position = numBytesRead.increment(2);

                synchronized (commands) {
                    command = commands.poll();
                }
            }
        }
    }

    void                        onRemoteClosed() {
        dispatcher.channelClosed (this);

        synchronized (this) {
            state = VSChannelState.Removed;
        }
    }

    void                        onRemoteClosing() {
        // close output stream - we cannot send any data
        out.closeNoFlush();

        synchronized (this) {

            switch (state) {
                case Connected:
                    state = VSChannelState.RemoteClosed;
                    break;

                case Closed:
                    try {
                        sendClosed();
                    } catch (Throwable x) {
                        LOGGER.log (Level.WARNING, "Error sending disconnect", x);
                    }
                    break;
            }
        }
    }

    void                        onDisconnected(IOException error) {
        // finish input stream - we do not expect any data
        if (error != null)
            in.putError(error);

        in.finish();

        // close output stream - we cannot send any data
        out.closeNoFlush();

        synchronized (this) {
            switch (state) {
                case Connected:
                    state = VSChannelState.RemoteClosed;
                    break;
            }
        }

        // notify availability listener after input close
        notifyDataAvailable();
    }

//    void                        onRemoteClosing() {
//        // finish input stream - we do not expect any data
//        in.finish();
//
//        // close output stream - we cannot send any data
//        out.closeNoFlush();
//
//        boolean shouldSendClosed = false;
//
//        synchronized (this) {
//            switch (state) {
//                case Connected:
//                    state = VSChannelState.RemoteClosed;
//                    break;
//
//                case Closed:
//                    shouldSendClosed = hasTransport;
//                    break;
//
//                default:
//                    break;
////                    if (hasTransport)
////                        throw new IllegalStateException (state.name ());
//            }
//        }
//
//         // notify availability listener after state change
//        notifyDataAvailable();
//
//        if (shouldSendClosed) {
//            try {
//                sendClosed();
//            } catch (Throwable x) {
//                LOGGER.log (Level.WARNING, "Error sending disconnect", x);
//            }
//        }
//    }

//    void                    receive(long position, byte [] data, int offset, int length) throws IOException {
//        // synchronized asserts is NOT allowed - will block transport
//        try {
//            if (compressed) {
//                int size = decompress(data, offset, length);
//                in.putData(infOut.getBuffer(), 0, size);
//            } else {
//                in.putData (data, offset, length);
//                System.out.println("receive(" + offset + ", " + length + ")");
//            }
//
//            notifyDataAvailable();
//        } catch (EOFException e) {
//            // ignore
//        }
//    }

    void                    receive(long position, byte[] data, int offset, int length, int code, int socketNumber) throws IOException {
        // synchronized asserts is NOT allowed - will block transport
        boolean available = false;
        //recievedLog.append(position).append(":").append(length).append("\n\r");

        int unpackedLength = 0;
        try {
            int queuePosition = (int) (position % inCapacity);
            if (compressed) {
                synchronized (inflater) {
                    unpackedLength = decompress(data, offset, length);
                    available = in.putData(infOut.getBuffer(), 0, unpackedLength, queuePosition);
                }
            } else {
                unpackedLength = length;
                available = in.putData (data, offset, unpackedLength, queuePosition);
            }

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Got data block " + position + ":" + (position + unpackedLength) + " (" + length + "/" + unpackedLength + ") from socket: @" + Integer.toHexString(code) + "#" + socketNumber);
            }
        } catch (EOFException e) {
            // ignore
        } finally {
            numBytesRead.increment(unpackedLength);
        }

        if (available) {
            notifyDataAvailable();
            checkCommands();
        }
    }

    private void            notifyDataAvailable() {
        if (listener != null)
            lnrNotifier.submit ();
    }

    void                   sendBytesRead (long bytes)
        throws InterruptedException, IOException
    {
        VSChannelState state = this.state;
        if (state != VSChannelState.Connected && state != VSChannelState.RemoteClosed) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Skipping BYTES_AVAILABLE_REPORT report: " + bytes + " because channel state is " + state);
            }
            return;
        }
        if (state == VSChannelState.RemoteClosed && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending BYTES_AVAILABLE_REPORT report: " + bytes + " at state " + state + " with remoteIndex=" + remoteIndex);
        }

        DataExchangeUtils.writeInt (buffer8, 4, (int)bytes);
        DataExchangeUtils.writeInt (buffer8, 8, remoteIndex);

        final VSTransportChannel    tc = dispatcher.checkOut ();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Sending BYTES_AVAILABLE_REPORT report: " + ((int)bytes) + " from " + tc.getSocketIdStr());
        }
        try {
            tc.write (buffer8, 0, buffer8.length);
        } finally {
            dispatcher.checkIn (tc);
        }
    }

    void                    sendConnect ()
        throws InterruptedException, IOException
    {
        byte []                     data = new byte[17];

        DataExchangeUtils.writeUnsignedShort (data, 0, LISTENER_ID);
        DataExchangeUtils.writeUnsignedShort (data, 2, localId);
        DataExchangeUtils.writeInt (data, 4, inCapacity);
        DataExchangeUtils.writeInt (data, 8, outCapacity);
        DataExchangeUtils.writeInt (data, 12, index);
        DataExchangeUtils.writeByte(data, 16, compressed ? 1 : 0);

        final VSTransportChannel    tc = dispatcher.checkOut ();
        try {
            tc.write (data, 0, data.length);
        } finally {
            dispatcher.checkIn (tc);
        }
    }

    void                    sendClosing()
        throws InterruptedException, IOException
    {
        assert remoteId != -1;

        byte []                     data = new byte [16];

        DataExchangeUtils.writeUnsignedShort (data, 0, remoteId);
        DataExchangeUtils.writeUnsignedShort (data, 2, CLOSING);
        DataExchangeUtils.writeInt(data, 4, remoteIndex);
        DataExchangeUtils.writeLong (data, 8, numBytesSend);

        final VSTransportChannel    tc = dispatcher.checkOut ();
        try {
            tc.write (data, 0, data.length);
            numBytesSend += 2;
        } finally {
            dispatcher.checkIn (tc);
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Sending CLOSING: remoteIndex=" + remoteIndex + " numBytesSend=" + numBytesSend);
        }
    }

    void                    sendClosed ()
        throws InterruptedException, IOException
    {
        assert remoteId != -1;

        byte []                     data = new byte [16];

        DataExchangeUtils.writeUnsignedShort (data, 0, remoteId);
        DataExchangeUtils.writeUnsignedShort (data, 2, CLOSED);
        DataExchangeUtils.writeInt (data, 4, remoteIndex);
        DataExchangeUtils.writeLong(data, 8, numBytesSend);

        final VSTransportChannel    tc = dispatcher.checkOut ();
        try {
            tc.write (data, 0, data.length);
            numBytesSend += 2;
        } finally {
            dispatcher.checkIn (tc);
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Sending CLOSED: remoteIndex=" + remoteIndex + " numBytesSend=" + numBytesSend);
        }
    }

    synchronized void           send(byte [] data, int offset, int length)
        throws InterruptedException, IOException
    {
        switch (state) {
            case Connected: {
                VSTransportChannel tc = null;

                try {
                    if (compressed) {
                        tc = dispatcher.checkOut ();

                        synchronized (deflater) {
                            int compressed = compress(data, offset, length);
                            tc.write(remoteId, remoteIndex, numBytesSend, defOut.getBuffer(), 0, compressed, length);
                            numBytesSend += length;
                        }
                    } else {
                        tc = dispatcher.checkOut ();
                        tc.write(remoteId, remoteIndex, numBytesSend, data, offset, length, length);
                        numBytesSend += length;
                        //sendLog.append("sending bytes: ").append(numBytesSend);
                    }

                } finally {
                    if (tc != null)
                        dispatcher.checkIn (tc);
                }
                break;
            }

            case Closed:
            case RemoteClosed:
                throw new ChannelClosedException ();

            default:
                throw new IllegalStateException (state.name ());
        }
    }

    void                    onCapacityIncreased(int capacity) {
        out.addAvailableCapacity(capacity);
    }

    void                    onConnectionRequest(int remoteId, int remoteCapacity, int rIndex)
        throws InterruptedException, IOException
    {
        assert this.remoteId == -1;

        this.remoteIndex = rIndex;        
        this.remoteId = remoteId;

        DataExchangeUtils.writeUnsignedShort (buffer8, 0, remoteId);
        DataExchangeUtils.writeUnsignedShort (buffer8, 2, BYTES_AVAILABLE_REPORT);        

        //
        //  Send CONNECT_ACK
        //
        byte []                     data = new byte [14];
        DataExchangeUtils.writeUnsignedShort (data, 0, remoteId);
        DataExchangeUtils.writeUnsignedShort (data, 2, CONNECT_ACK);
        DataExchangeUtils.writeUnsignedShort (data, 4, localId);
        DataExchangeUtils.writeInt (data, 6, inCapacity);
        DataExchangeUtils.writeInt (data, 10, index);

        final VSTransportChannel    tc = dispatcher.checkOut ();
        try {
            tc.write (data, 0, data.length);
        } finally {
            dispatcher.checkIn (tc);
        }

        synchronized (this) {
            state = VSChannelState.Connected;
        }
        // set remote capacity after sending CONNECT_ACK to prevent closing channel
        out.setRemoteCapacity(remoteCapacity);
    }

    @CheckReturnValue
    boolean assertIndexValid(int dataSize, int incoming) {
        boolean valid = index == incoming;
        if (!valid)
            LOGGER.log (Level.SEVERE,
                    this + "[Data:" + dataSize + "] - Wrong channel (remote: " + incoming + "; local: " + index + ")");

        //LOGGER.info(this + ":" + index + " - [Data:" + dataSize + "]");
        //System.out.println(index + " [Data:" + dataSize + "]");

        assert valid : "[Data:" + dataSize + "] - Wrong channel (remote: " + incoming + "; local: " + index + ")";
        return valid;
    }

    @CheckReturnValue
    boolean assertIndexValid(String method, int incoming) {
        boolean valid = index == incoming;
        if (!valid)
            LOGGER.log (Level.SEVERE,
                    this + "[" + method + "] - Wrong channel (remote: " + incoming + "; local: " + index + ")");

        //LOGGER.info(this + ":" + index + " - [" + method + "]");
        //LOGGER.log (Level.INFO, index + " - [" + method + "]");
        assert valid : "[" + method + "] - Wrong channel (remote: " + incoming + "; local: " + index + ")";
        return valid;
    }

    void                    onRemoteConnected(int remoteId, int remoteCapacity, int rIndex) {
        assert this.remoteId == -1;

        this.remoteId = remoteId;
        this.remoteIndex = rIndex;

        DataExchangeUtils.writeUnsignedShort (buffer8, 0, remoteId);
        DataExchangeUtils.writeUnsignedShort (buffer8, 2, BYTES_AVAILABLE_REPORT);

        boolean wasClosed;
        
        synchronized (this) {
            //assert state == VSChannelState.NotConnected; //TODO: check this

            wasClosed = state == VSChannelState.Closed;
            state = VSChannelState.Connected;
        }

        out.setRemoteCapacity(remoteCapacity);

        if (wasClosed) {
            //System.out.println(this + " onRemoteConnected for closing channel");
            close();
        }
    }

    @Override
    public boolean          setAutoflush(boolean value) {
        autoFlush = value;
        return true;
    }

    @Override
    public boolean          isAutoflush() {
        return autoFlush;
    }

    @Override
    public boolean          getNoDelay() {
        return noDelay;
    }

    @Override
    public void             setNoDelay(boolean value) {
        this.noDelay = value;
        
        if (value) {
            // TODO: Ideally we should delegate creation of ChannelExecutor instances to ContextContainer but ContextContainer can't access class ChannelExecutor.
            ChannelExecutor.getInstance(contextContainer.getAffinityConfig()).addChannel(this);
        }
    }

    @Override
    public void             setAvailabilityListener(Runnable lnr) {
        listener = lnr;
    }

    @Override
    public Runnable         getAvailabilityListener() {
        return listener;
    }

    @Override
    public String           encode(String value) {
        char[] msg = value.toCharArray();
        char[] id = getClientId().toCharArray();
        char[] output = new char[msg.length];

        for (int i = 0; i < msg.length; i++) {
            int index = i % id.length;
            output[i] = (char) (msg[i] ^ id[index]);
        }

        return new String(output);
    }

    @Override
    public String           decode(String value) {
        char[] msg = value.toCharArray();
        char[] id = getClientId().toCharArray();
        char[] output = new char[msg.length];

        for (int i = 0; i < msg.length; i++)
            output[i] = (char)(msg[i] ^ id[i % id.length]);

        return new String(output);
    }

    private int compress(byte[] data, int offset, int length) {

        deflater.reset();
        deflater.setLevel(length < 128 ? Deflater.NO_COMPRESSION : 3);
        deflater.setInput(data, offset, length);
        deflater.finish();

        defOut.reset(0);
        byte[] buffer = defOut.getBuffer();
        int count = deflater.deflate(buffer);

        while (!deflater.finished()) {
            if (count >= buffer.length) {
                defOut.ensureSize(defOut.getSize() + defOut.getSize() / 2);
                buffer = defOut.getBuffer();
            }
            count += deflater.deflate(buffer, count, buffer.length - count);
        }

        return count;
    }

    private int decompress(byte[] data, int offset, int length) {

        inflater.reset();
        inflater.setInput(data, offset, length);
        infOut.reset(0);

        try {
            int count = inflater.inflate(infOut.getBuffer());
            while (!inflater.finished()) {
                infOut.ensureSize(infOut.getSize() + infOut.getSize() / 2);
                count += inflater.inflate(infOut.getBuffer(), count, infOut.getSize() - count);
            }

            return count;
        } catch (final DataFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String           toString () {
        return ("VSChannel [local: " + localId + "; remote: " + (remoteId == Integer.MIN_VALUE  ? "(none)" : remoteId) + "] (" + index + ")");
    }

    int getIndex() {
        return index;
    }
}
