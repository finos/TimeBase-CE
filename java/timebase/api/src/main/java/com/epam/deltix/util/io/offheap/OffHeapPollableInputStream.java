/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.util.io.offheap;

import com.epam.deltix.util.collections.OffHeapByteQueue;
import com.epam.deltix.util.io.PollingThread;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.waitstrat.ParkWaitStrategy;
import com.epam.deltix.util.io.waitstrat.WaitStrategy;
import com.epam.deltix.util.lang.Pollable;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class OffHeapPollableInputStream extends InputStream implements Pollable {

    static final PollingThread          POLLER;

    static {
        synchronized (OffHeapPollableInputStream.class) {
            (POLLER = new PollingThread(new BusySpinIdleStrategy())).start();
        }
    }

    private OffHeapByteQueue            queue;
    private WaitStrategy                waitStrategy;

    OffHeapPollableInputStream(OffHeapByteQueue queue) throws IOException {
        this(queue, new ParkWaitStrategy());
    }

    OffHeapPollableInputStream(OffHeapByteQueue queue, WaitStrategy waitStrategy) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public void                         poll() {
        if (queue.getTailSequence().changed())
            waitStrategy.signal();
    }

    @Override
    public int                          read() throws IOException {
        int res;
        while ((res = queue.poll()) < 0) {
            waitUnchecked();
        }

        return res;
    }

    @Override
    public int                          read(byte b[], int off, int len)
            throws IOException {

        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int count;
        while ((count = queue.poll(b, off, len)) <= 0) {
            waitUnchecked();
        }

        return count;
    }

    private void                        waitUnchecked() throws IOException {
        try {
            waitStrategy.waitSignal();
        } catch (InterruptedException e) {
            //ignore
        }
    }

    @Override
    public void                         close() {
        POLLER.remove(this);
    }
}