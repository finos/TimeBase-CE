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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Weigher;
import com.google.common.primitives.Ints;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.Chunk;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.ChunkPool;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class ChunkCache {
    static final Log LOG = LogFactory.getLog(ChunkCache.class);

    private final LoadingCache<String, ChunkCacheFsEntry> cache;

    private final ChunkPool chunkPool;

    // Value in range (0..1]. Portion of chunks that can remain in pool.
    // Value 0.3 means that if more than 30% of cache capacity invalidated for whatever reason this memory will me released to JVM (and will be collected by GC).
    // Value 1 means that cache never releases any allocated memory.
    private static final double POOL_RATIO =  1.0;

    ChunkCache(long cacheSizeInBytes, int expectedMaxFileSizeInBytes, double preallocateRatio) {
        // We set pool size to 1/4 of all cache size or maxFileSizeInBytes (whatever is bigger)
        int poolEntryCount = Ints.saturatedCast(Math.max(Math.round(cacheSizeInBytes * POOL_RATIO), expectedMaxFileSizeInBytes) / ChunkCachingFileSystem.CHUNK_SIZE);
        if (poolEntryCount < 16) {
            throw new IllegalArgumentException("Chunk pool is too small: " + poolEntryCount);
        }
        int chunkCountToPreallocate = Math.min(poolEntryCount, Ints.saturatedCast(Math.round(cacheSizeInBytes * preallocateRatio) / ChunkCachingFileSystem.CHUNK_SIZE));

        long poolSizeInBytes = poolEntryCount * (long) ChunkCachingFileSystem.CHUNK_SIZE;
        long preallocatedSizeInBytes = chunkCountToPreallocate * (long) ChunkCachingFileSystem.CHUNK_SIZE;
        LOG.info("Assigning %sMb for Chunked FileSystem cache. Chunk pool: %s / %s (%sMb / %sMb)")
                .with(cacheSizeInBytes >> 20)
                .with(chunkCountToPreallocate)
                .with(poolEntryCount)
                .with(preallocatedSizeInBytes >> 20)
                .with(poolSizeInBytes >> 20);

        this.chunkPool = new ChunkPool(poolEntryCount);
        this.chunkPool.preallocateChunks(chunkCountToPreallocate);

        this.cache = Caffeine.newBuilder()
                .maximumWeight(cacheSizeInBytes)
                .weigher((Weigher<String, ChunkCacheFsEntry>) (key, value) -> value.getSizeEstimate())
                //.expireAfterAccess(10, TimeUnit.MINUTES) // TODO: Configure
                .removalListener((key, value, cause) -> {
                    if (value != null) {
                        value.invalidate(chunkPool);
                    }
                })
                .build(key -> new ChunkCacheFsEntry(expectedMaxFileSizeInBytes));


    }

    private String getKey(AbstractPath path) {
        return path.getPathString();
    }

    public ChunkCacheFsEntry getOrCreateEntry(AbstractPath path) {
        return cache.get(getKey(path));
    }

    // Note: this call may block
    public void invalidateIfExists(AbstractPath path) {
        ChunkCacheFsEntry currentEntry = cache.getIfPresent(getKey(path));
        if (currentEntry != null) {
            if (currentEntry.invalidate(chunkPool)) {
                updateEntrySize(path, currentEntry);
            }
        }
    }

    /**
     * @return true if entry size was updated
     */
    boolean updateEntrySize(AbstractPath path, ChunkCacheFsEntry updatedEntry) {
        String key = getKey(path);
        return updateEntrySize(key, updatedEntry);
    }

    private boolean updateEntrySize(String key, ChunkCacheFsEntry updatedEntry) {
        ChunkCacheFsEntry currentEntry = cache.getIfPresent(key); // Note: this will refresh the entry. So it should not expire during this method after that call
        if (currentEntry == updatedEntry) {
            // This is same entry. We should just call update on cache to update the value "weight".
            // Important: "Caffeine" cache does not trigger
            cache.put(key, currentEntry);
            return true;
        } else {
            // Entry expired and (probably) was replaced by something else
            // Note: we can't just put old entry back, this involves race condition
            // We have to a) merge data from old entry to new entry or b) just discard old entry
            // TODO: Consider if we should implement better handling

            // Just discard old entry
            return false;
        }
    }

    // Note: Rename process is not atomic. Multiple threads that attempt to rename same file may observe inconsistent results.
    void rename(AbstractPath sourcePath, AbstractPath destinationPath) {
        String sourceKey = getKey(sourcePath);
        ChunkCacheFsEntry sourceEntry = cache.getIfPresent(sourceKey);
        if (sourceEntry != null && sourceEntry.getLoadedChunkCount() > 0) {
            // We have an entry to rename
            String destinationKey = getKey(destinationPath);
            ChunkCacheFsEntry destinationEntry = cache.get(destinationKey);
            assert destinationEntry != null;

            // Copy data from source entry to destination entry
            // Note: we can't just reuse entry object because old entry object will be invalidated so it's data will be cleaned (in the removalListener).
            destinationEntry.copyDataFrom(sourceEntry, chunkPool);

            // Invalidate old entry
            cache.invalidate(sourceKey);
            updateEntrySize(destinationKey, destinationEntry);
        } else {
            // No entry no rename. We just need to invalidate entry at newPath
            invalidateIfExists(destinationPath);
        }
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    Chunk allocateNewChunk() {
        return chunkPool.getChunk();
    }

    ChunkPool getChunkPool() {
        return chunkPool;
    }
}
