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

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class OffHeapOutputStream extends OutputStream {
    private OffHeapByteQueue            queue;
    private IdleStrategy                idleStrategy;

    OffHeapOutputStream(OffHeapByteQueue queue) {
        this(queue, new BusySpinIdleStrategy());
    }

    OffHeapOutputStream(OffHeapByteQueue queue, IdleStrategy idleStrategy) {
        this.queue = queue;
        this.idleStrategy = idleStrategy;
    }

    @Override
    public void                         write(int n) throws IOException {
        byte b = (byte) n;
        int workCount = 0;
        while (!queue.offer(b))
            idleStrategy.idle(workCount++);
    }

    @Override
    public void                         write(byte b[], int off, int len)
            throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0) ||
                len > queue.capacity()) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        int workCount = 0;
        while (len > 0) {
            int written = queue.offer(b, off, len);
            len -= written;
            off += written;

            idleStrategy.idle(workCount++);
        }
    }

    @Override
    public void                         close() throws IOException {
    }
}