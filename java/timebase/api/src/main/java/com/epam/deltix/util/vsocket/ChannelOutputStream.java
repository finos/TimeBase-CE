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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import net.jcip.annotations.GuardedBy;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: Mar 25, 2010
 */
public class ChannelOutputStream extends VSOutputStream {
    @ApiStatus.Experimental // Temporary option for testing performance effect of flushing single packet
    private static final boolean SINGLE_SEND_ON_PARTIAL_FLUSH = Boolean.getBoolean("TimeBase.network.channel.singleSendOnPartialFlush");

    private final int                       maxCapacity;
    private final VSChannelImpl             channel;
    @GuardedBy("this")
    private boolean                         closed = false;
    @GuardedBy("this")
    private byte []                         buffer;
    @GuardedBy("this")
    private boolean                         flushDisabled = false;

    // Total number of accumulated bytes in the buffer.
    @GuardedBy("this")
    private int                             size = 0;

    // Number of bytes available to be sent.
    // Always: available <= size.
    // Can be less than size if flush is disabled.
    @GuardedBy("this")
    private int                             available = 0;

    private final AtomicInteger             waiting = new AtomicInteger(0);
    // TODO: Consider making it volatile instead. All writes are under synchronization
    private final AtomicInteger             remoteCapacityAvailable = new AtomicInteger(-1);
    private final AtomicInteger             capacityIncrement = new AtomicInteger(0);

    ChannelOutputStream(VSChannelImpl channel, int bufferSize) {
        this.channel = channel;
        this.maxCapacity = bufferSize;
        this.buffer = new byte [bufferSize];
    }

    @Override
    public synchronized void    close() throws IOException {
        if (!closed) {
            flush();
            closed = true;
            notifyAll();
        }
    }

    @Override
    public synchronized void    enableFlushing() throws IOException {
        flushDisabled = false;
        available = size;

        // Previously this check looked like this: "size >= maxCapacity"
        // However this is ineffective: the remote capacity is "maxCapacity" at most,
        // So attempt to flush more than that almost certainly results in situation
        // when we will block on that flush and need to wait for BYTES_AVAILABLE_REPORT from the remote side.
        // At the same time we do not want to flush too often (it's costly),
        // so we flush only when we have at least half of the buffer filled.
        int halfCapacity = maxCapacity >> 1;
        if (size >= halfCapacity) {
            // If we below of 75% capacity, we can flush buffer partially.
            // However, if we are above 75% capacity, we should flush all data
            // and block till all accumulated data is sent.
            // Otherwise, if the consumer too slow, the buffer will start to grow indefinitely.
            int buffer75percent = halfCapacity + (halfCapacity >> 1);
            boolean partialOk = size < buffer75percent;
            try {
                flushInternal(partialOk, false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    @Override
    public synchronized void    disableFlushing() {
        flushDisabled = true;
        available = size;
    }

    synchronized void           closeNoFlush() {
        if (!closed) {
            closed = true;
            notifyAll();
        }
    }

    @Override
    public synchronized void    flush() throws IOException {
        try {
            flushInternal (false, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    @Override
    public synchronized int flushAvailable(boolean flushAll) throws IOException {
        try {
            // we do not want to wait() here

            // TODO: Consider adding "capacityIncrement.get()" here
            if (available > 0 && getRemoteCapacity() > VSProtocol.MINSIZE) {
                boolean stopAfterSingleSend = SINGLE_SEND_ON_PARTIAL_FLUSH && !flushAll;
                return flushInternal(true, stopAfterSingleSend);
            } else {
                return 0;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    // Must be called by the thread that performs flush under lock
    @GuardedBy("this")
    private int checkCapacity() {
        return remoteCapacityAvailable.addAndGet(capacityIncrement.getAndSet(0));
    }

    private int                 getRemoteCapacity() {
        return remoteCapacityAvailable.get();
    }

    @GuardedBy("this")
    private int flushInternal (boolean partialOk, boolean stopAfterSingleSend) throws IOException, InterruptedException {
        assert partialOk || !stopAfterSingleSend : "stopAfterSingleSend is only allowed with partialOk==true";

        // Currently we try to send all "available" bytes at once.
        // This may be not the best idea with partialOk==true, because this means that this way we may prevent
        // loader thread from adding new data to the buffer and blocking him for a long time.
        // At the same time it possible that we will get into a blocking send because socket buffer
        // is full.
        // TODO: Consider sending only once if partialOk==true. Additionally the flush may return number of bytes sent
        //  so the ChannelExecutor can decide if it should sleep or not. Because if we sent at least some data
        //  then it's very likely that we spend on this more time, than the ChannelExecutor sleeps.

        int sent = 0;
        for (;;) {
            int remoteCapacity;
            for (;;) {
                remoteCapacity = checkCapacity();

                if (available == 0)
                    return sent;

                if (closed)
                    throw new ChannelClosedException();

                if (remoteCapacity >= VSProtocol.MINSIZE) {
                    // The only way to proceed to code after the loop
                    break;
                }

                if (partialOk)
                    return sent;

                //  "out" could be asynchronously flushed while this thread
                //  is in wait (). Therefore, we have to query the state of
                //  "out" after wait ().

                // TODO: "waiting" counter is not needed anymore. Consider removal.
                waiting.incrementAndGet();
                wait ();
                waiting.decrementAndGet();
            }

            // No need to get new value of getRemoteCapacity() here, we just got it in the loop above
            int packetSize = Math.min(Math.min(available, remoteCapacity), VSProtocol.MAXSIZE);

            channel.send (buffer, 0, packetSize);

            remoteCapacityAvailable.addAndGet(-packetSize);

            size -= packetSize;
            available -= packetSize;
            sent += packetSize;

            assert size >= 0;

            if (size > 0) {
                // Shift remaining data to the buffer start.
                // Slow on big buffer sizes!
                // TODO: Implement cyclic buffer instead
                System.arraycopy(buffer, packetSize, buffer, 0, size);

                if (stopAfterSingleSend) {
                    // We stop after very first send to allow loader thread to add new data.

                    // Move capacityIncrement to remoteCapacityAvailable,
                    // so threads that calls flushAvailable() or addAvailableCapacity() can see updated value.
                    checkCapacity();

                    return sent;
                }
            }
        }
    }

    @GuardedBy("this")
    private void                            ensureCapacity (int c) {
        int     cap = buffer.length;

        if (cap < c) {
            byte []     save = buffer;

            buffer = new byte [Util.doubleUntilAtLeast (cap, c)];

            System.arraycopy (save, 0, buffer, 0, size);
        }
    }

    @Override
    public synchronized void                write (byte @NotNull [] b, int off, int len)
            throws IOException
    {
        if (closed)
            throw new ChannelClosedException();

        int         newSize = size + len;

        if (flushDisabled) {
            ensureCapacity (newSize);
            System.arraycopy (b, off, buffer, size, len);
            size = newSize;
        }
        else if (newSize <= buffer.length) {
            System.arraycopy (b, off, buffer, size, len);
            available = size = newSize;
        }
        else {
            try {
                // TODO: We do not necessarily need full flush here. We need to get "len" bytes of free space
                flushInternal (false, false);

                assert size == 0;

                if (len < maxCapacity) {
                    System.arraycopy (b, off, buffer, 0, len);
                    available = size = len;
                } else {
                    send (b, off, len);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UncheckedInterruptedException (e);
            }
        }
    }

    @Override
    public synchronized void                write(int b) throws IOException {
        if (closed)
            throw new ChannelClosedException();

        try {
            if (flushDisabled)
                ensureCapacity (size + 1);
            else if (size >= maxCapacity)
                flushInternal (false, false);

            if (!flushDisabled)
                available++;

            buffer [size++] = (byte) b;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException (e);
        }
    }

    public synchronized void                 setRemoteCapacity(int capacity) {
        remoteCapacityAvailable.set(capacity);
        notify();
    }

    private void wakeAfterCapacityAdded() {
        synchronized (this) {
            // Update remote capacity, so next "addAvailableCapacity" will not need to block
            checkCapacity();
            // Wakeup waiting thread
            notify();
        }
    }

    public void                             addAvailableCapacity(int capacity) {
        capacityIncrement.addAndGet(capacity);
        if (getRemoteCapacity() < VSProtocol.MINSIZE) {
            wakeAfterCapacityAdded();
        }
    }

    /**
     * Sends data immediately, without putting it into the buffer.
     * Used when the data does not fit into buffer.
     */
    @GuardedBy("this")
    private int                             send (byte @NotNull [] data, int offset, int length)
            throws IOException
    {
        int bytes = 0;

        try {
            while (length > 0) {
                int remoteCapacity;
                for (;;) {
                    remoteCapacity = checkCapacity();

                    if (closed)
                        throw new ChannelClosedException();

                    if (remoteCapacity >= VSProtocol.MINSIZE) {
                        // The only way to proceed to code after the loop
                        break;
                    }

                    //waiting.incrementAndGet();
                    wait ();
                    //waiting.decrementAndGet();
                }

                // No need to get new value of getRemoteCapacity() here, we just got it in the loop above
                int packetSize = Math.min(Math.min(length, remoteCapacity), VSProtocol.MAXSIZE);

                channel.send(data, offset, packetSize);

                remoteCapacityAvailable.addAndGet(-packetSize);

                length -= packetSize;
                bytes += packetSize;
                offset += packetSize;
            }
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException (e);
        }

        return bytes;
    }

    @Override
    public String toString() {
        return "ChannelOutputStream@" + Integer.toHexString(hashCode()) +
                " for channel=" + channel;
    }
}
