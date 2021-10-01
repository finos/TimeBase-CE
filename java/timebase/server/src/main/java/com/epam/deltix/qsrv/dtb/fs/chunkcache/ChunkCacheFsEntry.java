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
package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.Chunk;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.ChunkPool;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.lang.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Alexei Osipov
 */
@ThreadSafe
@ParametersAreNonnullByDefault
final class ChunkCacheFsEntry {
    static final int CHUNK_SIZE = ChunkCachingFileSystem.CHUNK_SIZE;

    private long fileSize = -1; // -1 mean file size is unknown

    private int loadedChunks = 0; // Total number of non-null chunks for this file
    private int maxLoadedChunkIndex = -1; // Maximum index of known chunk

    // Lock for download of this file.
    // Contract: if a thread want to download a chunk for this file then the thread should acquire this lock and only then lock on ChunkCacheFsEntry.
    private final Object loadLock = new Object();

    private final int expectedMaxSize;

    private Chunk[] chunks;

    /**
     * @param expectedMaxFileSizeBytes This value defines upper estimate for "common" files.
     *                                 This is not a hard limit but accessing files bugger than this size
     *                                 will involve additional memory allocation and extra copying.
     */
    ChunkCacheFsEntry(int expectedMaxFileSizeBytes) {
        assert BitUtil.isPowerOfTwo(CHUNK_SIZE);
        int expectedMaxChunkCount = expectedMaxFileSizeBytes / CHUNK_SIZE + 1; // 1 to compensate rounding
        this.expectedMaxSize = BitUtil.align(expectedMaxChunkCount, 8);
        this.chunks = new Chunk[8]; // Start with small size
    }

    public int getSizeEstimate() {
        // Even if there no chunks yet usually that means that we going to add at least one.
        // Note: loadedChunks should be volatile but
        int chunks = Math.max(loadedChunks, 1);
        return 64 + CHUNK_SIZE * chunks;
    }

    /**
     * @param chunkPool free chunks (if any) will be released to this pool during the invalidation process
     * @return true if size was changed
     */
    public synchronized boolean invalidate(ChunkPool chunkPool) {
        boolean sizeChanged = loadedChunks > 0;
        for (int i = 0; i <= maxLoadedChunkIndex; i++) {
            // Free chunks
            Chunk chunk = chunks[i];
            chunks[i] = null;
            if (chunk != null) {
                chunk.release(chunkPool);
            }
        }
        loadedChunks = 0;
        maxLoadedChunkIndex = -1;
        fileSize = -1;
        return sizeChanged;
    }

    // Note: there is a possibility of deadlock in case of concurrent renames A->B and B->A. TODO: Decide how to fix.
    synchronized void copyDataFrom(ChunkCacheFsEntry dataSource, ChunkPool chunkPool) {
        // Remove old data
        invalidate(chunkPool);
        synchronized (dataSource) {
            for (int i = 0; i <= dataSource.maxLoadedChunkIndex; i++) {
                // Free chunks
                Chunk chunk = dataSource.chunks[i];
                if (chunk != null) {
                    putChunk(i, chunk, dataSource.getChunkSizeByIndex(i), chunkPool);
                }
            }
        }
    }

    public synchronized int getLoadedChunkCount() {
        return loadedChunks;
    }

    public static int getChunkIndex(long offset) {
        return (int) (offset / CHUNK_SIZE);
    }

    public static long getChunkStartByIndex(int index) {
        return index * CHUNK_SIZE;
    }

    // Note: only valid for loaded chunks
    public synchronized long getChunkLimitByIndex(int index) {
        int limit = (index + 1) * CHUNK_SIZE;
        if (fileSize >= 0) {
            return Math.min(limit, fileSize);
        } else {
            // We not reached end of file so we have only "full" chunks
            return limit;
        }
    }

    // Note: only valid for loaded chunks
    public synchronized int getChunkSizeByIndex(int index) {
        if (fileSize >= 0 && maxLoadedChunkIndex == index) {
            // This is the last chunk
            return (int) (getChunkLimitByIndex(index) - getChunkStartByIndex(index));
        } else {
            // We not reached end of file so we have only "full" chunks
            return CHUNK_SIZE;
        }
    }

    /**
     * Returns chunk and increments it's usage count.
     */
    @Nullable
    public synchronized Chunk getChunk(int chunkIndex) {
        if (chunkIndex <= maxLoadedChunkIndex) {
            Chunk chunk = chunks[chunkIndex];
            if (chunk == null) {
                return null;
            }
            chunk.addUse();
            assert chunk.getUsageCount() >= 2;
            return chunk;
        } else {
            return null;
        }
    }

    public synchronized void putChunk(int chunkIndex, Chunk chunk, int chunkDataLength, ChunkPool chunkPool) {
        assert chunk.getArray().length == CHUNK_SIZE;
        if (chunkIndex >= chunks.length) {
            // Array size is bigger than we expected => Resize chunk array.
            int sizeRequired = chunkIndex + 1;
            int newLength;
            if (this.expectedMaxSize >= sizeRequired) {
                newLength = this.expectedMaxSize; // Skip small sizes and use expected max size
            } else {
                newLength = Util.doubleUntilAtLeast(chunks.length, sizeRequired);
            }

            Chunk[] newChunks = new Chunk[newLength];
            System.arraycopy(chunks, 0, newChunks, 0, maxLoadedChunkIndex + 1);
            this.chunks = newChunks;
        }

        if (chunkIndex > maxLoadedChunkIndex) {
            maxLoadedChunkIndex = chunkIndex;
        }
        if (chunks[chunkIndex] == null) {
            loadedChunks ++;
        }

        chunk.addUse();
        assert chunk.getUsageCount() >= 2; // One use by the stream and one use by this cache entry. In case of rename 3 usages possible.


        Chunk oldChunk = chunks[chunkIndex];
        if (oldChunk != null) {
            oldChunk.release(chunkPool);
        }
        chunks[chunkIndex] = chunk;

        if (chunkDataLength < CHUNK_SIZE) {
            // This is the last chunk of data
            int fileSize = chunkIndex * CHUNK_SIZE + chunkDataLength;

            if (this.fileSize >= 0 && this.fileSize != fileSize) {
                throw new IllegalStateException("Previous and new file size do not match");
            }
            this.fileSize = fileSize;
        }
    }

    public static int getNumberOfChunks(long startOffset, int length) {
        if (length == 1) {
            // Common case
            return 1;
        }
        int startChunkIndex = getChunkIndex(startOffset);
        int endChunkIndex = getChunkIndex(startOffset + length - 1); // Don't need to load last byte
        return endChunkIndex - startChunkIndex + 1;
    }

    /**
     * Returns number of missing (not loaded) sequential chunks starting from {@code startChunkIndex} and up to {@code chunksToFulfillRequest}.
     */
    public int getNumberOfMissingChunks(int startChunkIndex, int chunksToFulfillRequest) {
        if (maxLoadedChunkIndex < startChunkIndex) {
            return chunksToFulfillRequest;
        }

        for (int i = 0; i < chunksToFulfillRequest; i++) {
            if (chunks[startChunkIndex + i] != null) {
                return i;
            }
        }
        return chunksToFulfillRequest;
    }

    Object getLoadLock() {
        return loadLock;
    }
}