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
package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.gflog.api.Loggable;
import com.epam.deltix.util.collections.ByteArray;

/**
 * Custom Heap Manager
 */
public interface HeapManager extends Loggable {
    /**
     * Allocates given number of bytes into provided ByteBuffer.
     *
     * <p>WARN: Caller is responsible for releasing allocated buffer at some later point to prevent memory leaks!</p>
     * <p>WARN: Caller must not change byte buffer size!</p>
     *
     * @param block output buffer
     * @return false if heap is out of memory (of given size), in which case block size is set to zero
     */
    boolean allocate (int size, ByteArray block);

    /**
     * Releases previously allocated buffer.
     *
     * <p>WARN: Caller must NOT change byte buffer size!.</p>
     * <p>WARN: Caller must NOT de-allocate block twice.</p>
     * @param block
     */
    void deallocate (ByteArray block);
    void defragment (ByteArray block);

    /** @return maximum size of allocation unit and also total amount of memory in heap */
    long getHeapSize();

    /** Return heap utilization percentage */
    long getUtilization();

}
