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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
abstract class RawReaderBase <T> 
    implements Disposable, IntermittentlyAvailableResource, QueueMessageReader
{
    private final TickStreamImpl            stream;
    protected final boolean                 live;
    protected final T                       mfile;
    protected final MemoryDataInput         mdi = new MemoryDataInput ();
    //
    //  Reader state, not guarded by anything; relies on the caller
    //  to not re-enter.
    //
    final AtomicLong                        currentFileOffset = new AtomicLong();
    //
    //  Current entry data
    //
    long                                    blockHeadOffset;
    int                                     bodyLength;
    final TimeStamp                         time = new TimeStamp();
    private volatile Runnable               availabilityListener = null;
    private QuickExecutor.QuickTask         notifier;
    //
    //  File cache, change guarded by mfile.
    //
    byte []                        buffer;

    public volatile boolean                 waiting = false;

    RawReaderBase (
        TickStreamImpl                      stream,
        T                                   mfile,
        long                                fileOffset,
        int                                 bufferSize,
        boolean                             live
    )
    {
        this.stream = stream;
        this.mfile = mfile;
        this.currentFileOffset.set(fileOffset);
        this.live = live;
        
        buffer = new byte [bufferSize];

        notifier = new QuickExecutor.QuickTask (stream.getQuickExecutor()) {
            @Override
            public void run () {
                final Runnable          lnr = availabilityListener;

                if (lnr == null)
                    synchronized (RawReaderBase.this) {
                        RawReaderBase.this.notifyAll ();
                    }
                else
                    lnr.run ();
            }
        };
    }

    @Override
    public final TickStreamImpl getStream () {
        return (stream);
    }

    protected boolean           isAsynchronous () {
        return (availabilityListener != null);
    }

    @Override
    public void                 setAvailabilityListener (Runnable lnr) {
        availabilityListener = lnr;
    }

    public final void           submitNotifier () {
        waiting = false;
        notifier.submit ();
    }

    @Override
    public MemoryDataInput      getInput () {
        return mdi;
    }

    @Override
    public long                 getTimestamp () {
        return time.timestamp;
    }

    @Override
    public long                 getNanoTime() {
        return time.getNanoTime();
    }

    long                        getCurrentOffset() {
        return currentFileOffset.get();
    }

    void                        ensureBufferCapacity (int requiredSize) {
        if (buffer.length < requiredSize)
            buffer = new byte [Util.doubleUntilAtLeast (buffer.length, requiredSize)];
    }
        
    public void                 seek (long n) {
        currentFileOffset.set(n);
    }

    @Override
    public abstract long        available();

    @Override
    public abstract boolean     isTransient();

    /**
     * Reads a single message from the data file.
     *     
     * @return  <code>false</code> if end of file reached.
     * @throws IOException
     */
    @Override
    public abstract boolean     read () throws IOException;
    
    @Override
    public String               toString () {
        return (mfile + " at " + currentFileOffset);
    }

    @Override
    public boolean isLive() {
        return live;
    }
}