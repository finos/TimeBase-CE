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

package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Write Queue
 */
public class WriteQueue {

    private final ArrayDeque<TSFile>[] dirtyFiles;
    private final int writers;

    private final AtomicLong usedMemory = new AtomicLong(0);

    private final ObjectArrayList<TSFile> badDirtyFiles = new ObjectArrayList<>(); // Files that were not saved properly

    public WriteQueue(int writers) {
        this.writers = writers;
        this.dirtyFiles = new ArrayDeque[writers];

        for (int i = 0; i < writers; i++)
            this.dirtyFiles[i] = new ArrayDeque<>();
    }

    public synchronized void         addFailed(TSFile tsf) {
        badDirtyFiles.add(tsf);
        notifyAll ();
    }

    public synchronized void         add(TSFile tsf) {
        assert !contains(tsf) : tsf + " is already queued";

        tsf.queued = true;

        if (PDSImpl.LOGGER.isDebugEnabled())
            PDSImpl.LOGGER.debug("Adding to the write queue: " + tsf.getPathString());

        int index = getQueueIndex(tsf);
        dirtyFiles[index].addLast(tsf);

        usedMemory.addAndGet(tsf.getAllocatedSize(true));

        notifyAll ();
    }

    private int getQueueIndex(TSFile tsf) {
        return tsf.root.hashCode() % writers;
    }

    public synchronized boolean     remove(TSFile tsf) {
        int index = getQueueIndex(tsf);
        boolean removed = dirtyFiles[index].remove(tsf);

        if (removed) {
            tsf.queued = false;
            usedMemory.addAndGet(-tsf.getAllocatedSize(false));
            notifyAll ();
        }

        return removed;
    }

    public synchronized TSFile       poll(int index) throws InterruptedException {
        ArrayDeque<TSFile> deque = dirtyFiles[index];

        while (deque.isEmpty ()) {
            // We don't have "normal" file to process, so let's try a "bad" file
            if (!badDirtyFiles.isEmpty()) {
                return badDirtyFiles.remove(0);
            }

            wait ();
        }

        TSFile file = deque.poll();
        usedMemory.addAndGet(-file.getAllocatedSize(false));

        return file;
    }

    public long                     getUsedMemory() {
        return usedMemory.get();
    }

    synchronized boolean             contains(TSFile file) {
        int index = getQueueIndex(file);
        return dirtyFiles[index].contains(file);
    }

}
