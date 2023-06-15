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
package com.epam.deltix.util.io.offheap;

import com.epam.deltix.util.collections.OffHeapByteQueue;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;

import java.io.*;

/**
 *
 */
public class OffHeapInputStream extends InputStream {
    protected OffHeapByteQueue          queue;
    private IdleStrategy                idleStrategy;

    OffHeapInputStream(OffHeapByteQueue queue) {
        this(queue, new BusySpinIdleStrategy());
    }

    OffHeapInputStream(OffHeapByteQueue queue, IdleStrategy idleStrategy) {
        this.queue = queue;
        this.idleStrategy = idleStrategy;
    }

    @Override
    public int                          read() throws IOException {
        int workCount = 0;
        int res;
        while ((res = queue.poll()) < 0) {
            idleStrategy.idle(workCount++);
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
        int workCount = 0;
        while ((count = queue.poll(b, off, len)) <= 0) {
            idleStrategy.idle(workCount++);
        }

        return count;
    }

    @Override
    public void                         close() {
    }
}