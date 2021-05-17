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

import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: Mar 25, 2010
 */
public class ChannelOutputStream extends VSOutputStream {
    private final int                       maxCapacity;
    private final VSChannelImpl             channel;
    private boolean                         closed = false;
    private byte []                         buffer;
    private boolean                         flushDisabled = false;

    @GuardedBy("this")
    private int                             size = 0;

    @GuardedBy("this")
    private int                             available = 0;

    private final AtomicInteger             waiting = new AtomicInteger(0);
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

    private void  doNotify() {
        if (waiting.get() > 1) {
            notifyAll();
        } else {
            notify();
        }
    }

    @Override
    public synchronized void    enableFlushing() throws IOException {
        flushDisabled = false;
        available = size;

        if (size >= maxCapacity)
            try {
                flushInternal (false);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException (e);
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
            flushInternal (false);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException (e);
        }
    }

    @Override
    public synchronized void flushAvailable() throws IOException {
        try {
            // we do not want to wait() here
            if (available > 0 && getRemoteCapacity() > VSProtocol.MINSIZE)
                flushInternal (true);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException (e);
        }
    }

    private void                checkCapacity() {
        remoteCapacityAvailable.addAndGet(capacityIncrement.getAndSet(0));
    }

    private int                 getRemoteCapacity() {
        return remoteCapacityAvailable.get();
    }

    private void                flushInternal (boolean partialOk) throws IOException, InterruptedException {
        for (;;) {
            for (;;) {
                checkCapacity();

                if (available == 0)
                    return;

                if (closed)
                    throw new ChannelClosedException();

                if (getRemoteCapacity() >= VSProtocol.MINSIZE)
                    break;

                if (partialOk)
                    return;

                //  "out" could be asynchronously flushed while this thread
                //  is in wait (). Therefore, we have to query the state of
                //  "out" after wait ().

                // TODO: "waiting" counter is not needed anymore. Consider removal.
                waiting.incrementAndGet();
                wait ();
                waiting.decrementAndGet();
            }

            int             packetSize = available;

            if (packetSize > getRemoteCapacity())
                packetSize = getRemoteCapacity();

            if (packetSize > VSProtocol.MAXSIZE)
                packetSize = VSProtocol.MAXSIZE;

            channel.send (buffer, 0, packetSize);

            checkCapacity();
            remoteCapacityAvailable.addAndGet(-packetSize);

            size -= packetSize;
            available -= packetSize;

            assert size >= 0;

            System.arraycopy (buffer, packetSize, buffer, 0, size);
        }
    }

    private void                            ensureCapacity (int c) {
        int     cap = buffer.length;

        if (cap < c) {
            byte []     save = buffer;

            buffer = new byte [Util.doubleUntilAtLeast (cap, c)];

            System.arraycopy (save, 0, buffer, 0, size);
        }
    }

    @Override
    public synchronized void                write (byte [] b, int off, int len)
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
                flushInternal (false);

                assert size == 0;

                if (len < maxCapacity) {
                    System.arraycopy (b, off, buffer, 0, len);
                    available = size = len;
                } else {
                    send (b, off, len);
                }
            } catch (InterruptedException e) {
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
                flushInternal (false);

            if (!flushDisabled)
                available++;

            buffer [size++] = (byte) b;
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException (e);
        }
    }

    public synchronized void                 setRemoteCapacity(int capacity) {
        remoteCapacityAvailable.set(capacity);
        notify();
    }

    private synchronized void                addCapacity(int value) {
        remoteCapacityAvailable.addAndGet(value);
        notify();
    }

    public void                             addAvailableCapacity(int capacity) {
        if (getRemoteCapacity() < VSProtocol.MINSIZE)
            addCapacity(capacity);
        else
            capacityIncrement.addAndGet(capacity);
    }

    private int                             send (byte[] data, int offset, int length)
            throws IOException
    {
        int bytes = 0;

        try {
            while (length > 0) {
                for (;;) {
                    checkCapacity();

                    if (closed)
                        throw new ChannelClosedException();

                    if (getRemoteCapacity() >= VSProtocol.MINSIZE)
                        break;

                    //waiting.incrementAndGet();
                    wait ();
                    //waiting.decrementAndGet();
                }

                int             packetSize = Math.min(length, getRemoteCapacity());

                if (packetSize > VSProtocol.MAXSIZE)
                    packetSize = VSProtocol.MAXSIZE;

                channel.send(data, offset, packetSize);

                checkCapacity();
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
