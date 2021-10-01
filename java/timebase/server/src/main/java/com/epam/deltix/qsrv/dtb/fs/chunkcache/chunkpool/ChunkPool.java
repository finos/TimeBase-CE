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
package com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Simple "synchronized"-based fixed size chunk pool.
 *
 * @author Alexei Osipov
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class ChunkPool {
    private final Chunk[] pool;
    private final int maxSize;
    private int size = 0;

    public ChunkPool(int maxSize) {
        this.pool = new Chunk[maxSize];
        this.maxSize = maxSize;
    }

    @Nonnull
    public synchronized Chunk getChunk() {
        Chunk result;
        if (size > 0) {
            // We have unused chunks
            size --;
            result = pool[size];
            assert result.getUsageCount() == 0;
            pool[size] = null;
        } else {
            // No free chunks. Create new.
            result = new Chunk(); // ALLOCATION (Heap)
        }

        result.allocate();
        return result;
    }

    public synchronized void putChunk(Chunk chunk) {
        if (chunk.getUsageCount() != 0) {
            throw new IllegalStateException("Can't add chunk to pool. Usage count: " + chunk.getUsageCount());
        }

        //noinspection StatementWithEmptyBody
        if (size < maxSize) {
            // Add to pool
            pool[size] = chunk;
            size ++;
        } else {
            // Pool is full
            // Let chunk to be garbage collected
        }
    }

    public synchronized void preallocateChunks(int count) {
        for (int i = 0; i < count; i++) {
            putChunk(new Chunk());
        }
    }
}