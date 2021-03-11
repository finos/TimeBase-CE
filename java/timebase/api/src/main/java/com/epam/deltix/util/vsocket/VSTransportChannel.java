package com.epam.deltix.util.vsocket;

import static com.epam.deltix.util.vsocket.VSProtocol.*;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.memory.DataExchangeUtils;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.io.*;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

/**
 *  Similar to DataSocket, but stripped of much special logic.
 */
class VSTransportChannel implements Runnable, Disposable {
    // Set to true whenever the channel is checked out
    @GuardedBy("dispatcher.freeChannels")
    boolean checkedOut = false;

    private byte[]                      buffer = new byte[4096];
    private final byte[]                header = new byte[16];
    
    private final VSDispatcher          dispatcher;
    final VSocket                       socket;

    private final VSocketInputStream    vin;
    private final DataInputStream       din;
    private final VSocketOutputStream   out;

    private final byte[]                keepAlive = new byte[2];
    private final byte[]                bytesReport = new byte[10];
    private final byte[]                ping = new byte[2];

    private volatile boolean            closed = false;
    volatile long                       latency = Long.MAX_VALUE;

    private volatile long               reported;

    private final Thread                thread;

    private final QuickExecutor.QuickTask completeTask;

    @Nonnull
    private QuickExecutor.QuickTask createCompleteTask(QuickExecutor quickExecutor) {
        return new QuickExecutor.QuickTask(quickExecutor) {
            @Override
            public void run() throws InterruptedException {
                long bytesRead;
                synchronized (out) {
                    bytesRead = vin.getBytesRead();
                    if (bytesRead == reported) {
                        // We already reported this value
                        return;
                    }
                    DataExchangeUtils.writeLong(bytesReport, 2, (reported = bytesRead));
                    out.write(bytesReport, 0, bytesReport.length);
                }
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "Sent BYTES_RECIEVED report: " + bytesRead + " from " + socket.getSocketIdStr());
                }
            }
        };
    }

    VSTransportChannel(VSDispatcher dispatcher, final VSocket socket, ThreadFactory threadFactory) throws IOException {
        this.dispatcher = dispatcher;
        this.socket = socket;
        this.completeTask = createCompleteTask(dispatcher.getQuickExecutor());

        DataExchangeUtils.writeUnsignedShort(keepAlive, 0, KEEP_ALIVE);
        DataExchangeUtils.writeUnsignedShort(ping, 0, PING);
        DataExchangeUtils.writeUnsignedShort(bytesReport, 0, BYTES_RECIEVED);

        this.vin = socket.getInputStream();
        this.din = new DataInputStream (vin);
        this.out = socket.getOutputStream();

        this.thread = threadFactory.newThread(this);
        this.thread.setName("VSTransportChannel for " + socket);
    }

    private synchronized void   onException (Throwable x) {
        dispatcher.transportStopped (this, x);
        close();
    }

    public void                 write(int id, int index, long position, byte[] data, int offset, int length, int unpackedLength) {
        assert id >= 0 && length > 0;
        
        if (length == 0)
            LOGGER.log (Level.WARNING, "Writing zero length packet");

        if (out.isBroken()) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Write to a broken transport " + socket.getSocketIdStr());
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Sending data block " + position + ":" + (position + unpackedLength) + " (" + length + "/" + unpackedLength + ") to socket: " + socket.getSocketIdStr());
        }

        synchronized (out) {
            DataExchangeUtils.writeUnsignedShort(header, 0, id);
            DataExchangeUtils.writeUnsignedShort(header, 2, length);
            DataExchangeUtils.writeInt(header, 4, index);
            DataExchangeUtils.writeLong(header, 8, position);

            out.writeTwoArrays (header, 0, header.length, data, offset, length);
        }
    }

    public void                 write (byte [] data) {
        write (data, 0, data.length);
    }

    public void                 write (byte [] data, int offset, int length) {
        if (length == 0)
            LOGGER.log (Level.WARNING, "Zero length packet");

        synchronized (out) {
            out.write (data, offset, length);
        }
    }

    public void                 keepAlive () {
        write(keepAlive);
    }

    private long                ping () {
        long l = latency = System.nanoTime();
        write(ping);
        return l;
    }

    @Override
    public void                 run () {
        int     index;
        long    offset;
        Thread currentThread = Thread.currentThread();
        assert currentThread == this.thread;

        try {
            for (;;) {
                vin.complete();

                if (currentThread.isInterrupted())
                    throw new InterruptedException();

                if (vin.getBytesRead() - reported > VSocketOutputStream.CAPACITY / 4)
                    completeTask.submit();
                
                int destId = din.readUnsignedShort ();
                //System.out.println(this.socket + ": signal = " + destId);

                if (destId == LISTENER_ID) {    // Virtual connection request
                    int             code = din.readUnsignedShort ();

                    int inCapacity = din.readInt();
                    int outCapacity = din.readInt();
                    int rIndex = din.readInt();
                    boolean compressed = din.readByte() == 1;

                    VSChannelImpl local = dispatcher.newChannel (outCapacity, inCapacity, compressed);
                    try {
                        local.onConnectionRequest(code, inCapacity, rIndex);
                        dispatcher.connectionListener.connectionAccepted (dispatcher.getQuickExecutor(), local);
                    } catch (Throwable x) {
                        // No reason to shutdown this transport channel
                        LOGGER.log (Level.SEVERE, "Exception sending ACK", x);
                        local.close ();
                    }
                }
                else if (destId == BYTES_RECIEVED) {
                    long size = din.readLong ();
                    out.confirm(size);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Got BYTES_RECIEVED report: " + size + " in " + socket.getSocketIdStr());
                    }
                }
                else if (destId == KEEP_ALIVE) {
                    // keep alive signal - do nothing
                }
                else if (destId == PING) {
                    if (latency != Long.MAX_VALUE)
                        latency = System.nanoTime() - latency;
                    else
                        write(ping);
                }
                else if (destId == DISPATCHER_CLOSE) {
                    dispatcher.onRemoteClosed();
                }
                else {
                    int             code = din.readUnsignedShort ();
                    VSChannelImpl   c = dispatcher.getChannel (destId);

                    if (c == null && code != CLOSING) {
                        if (code == BYTES_AVAILABLE_REPORT) {
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "Got BYTES_AVAILABLE_REPORT for missing (recently closed?) channel " + destId);
                            }
                        } else {
                            LOGGER.log(Level.SEVERE, code + ": No local channel for " + destId);
                        }
                    }

                    switch (code) {
                        case CONNECT_ACK:
                            int     remoteId = din.readUnsignedShort ();
                            int     remoteCapacity = din.readInt ();
                            int     remoteIndex = din.readInt ();

                            assert c != null;

                            c.onRemoteConnected(remoteId, remoteCapacity, remoteIndex);
                            break;

                        case CLOSING:
                            index = din.readInt();
                            offset = din.readLong();

                            if (c != null) {
                                boolean valid = c.assertIndexValid("CLOSING", index);
                                if (valid) {
                                    c.processCommand(code, offset);
                                }
                            }

                            break;
                        
                        case CLOSED:
                            index = din.readInt();
                            offset = din.readLong();

                            if (c != null) {
                                boolean valid = c.assertIndexValid("CLOSED", index);
                                if (valid) {
                                    c.processCommand(code, offset);
                                }
                            }

                            break;

                        case BYTES_AVAILABLE_REPORT :
                            int available = din.readInt();
                            index = din.readInt();

                            assert (available >= 0);

                            if (c != null) {
                                boolean valid = index == c.getIndex();
                                // make sure that we mark that bytes reports as read,
                                vin.complete();
                                // because code below may not return
                                if (valid) {
                                    c.onCapacityIncreased(available);
                                    if (LOGGER.isLoggable(Level.FINEST)) {
                                        LOGGER.log(Level.FINEST, "Got BYTES_AVAILABLE_REPORT report: " + available + " in " + socket.getSocketIdStr());
                                    }
                                } else {
                                    LOGGER.log(Level.FINE, "Got BYTES_AVAILABLE_REPORT for wrong channel " + destId);
                                }
                            }
                            break;

                        default:
                            index = din.readInt();
                            offset = din.readLong();

                            if (c == null) {
                                LOGGER.log (Level.INFO, "Skipping bytes (no channel): " + code);
                                din.skipBytes (code);
                            } else if (!c.assertIndexValid(code, index)) {
                                LOGGER.log (Level.WARNING, "Skipping bytes (wrong channel): " + code);
                                din.skipBytes (code);
                            } else  {
                                int     capacity = buffer.length;

                                if (capacity < code)
                                    buffer = new byte [Util.doubleUntilAtLeast (capacity, code)];

                                if (code == 0)
                                    LOGGER.log (Level.WARNING, "Unknown zero-length data for channel: " + destId);

                                din.readFully (buffer, 0, code);
                                c.receive(offset, buffer, 0, code, socket.getCode(), socket.getSocketNumber());
                            }
                    }
                }
            }
        } catch (InterruptedException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log (Level.FINE, "Interrupted" , e);
        } catch (Throwable x) {
            if (currentThread.isInterrupted())
                LOGGER.log (Level.FINE, this + ": Interrupted.");
            else
                onException (x);
        } finally {
            closed = true;
        }
    }

    public long                 getLatency() {
        long l = ping();
        while (latency == l && !closed)
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }

        if (closed)
            return Long.MAX_VALUE;

        return latency;
    }

    @Override
    public void                 close () {
        //flusher.interrupt();
        this.thread.interrupt();
        Util.close (socket);
    }

    public void                 start() {
        thread.start();
    }

    @Override
    public String toString() {
        return getClass().getName() +"@" + Integer.toHexString(hashCode()) + " of socket " + socket.getSocketIdStr();
    }

    public String getSocketIdStr() {
        return socket.getSocketIdStr();
    }
}
