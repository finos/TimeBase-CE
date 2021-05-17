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

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.lang.Wrapper;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ChunkCachingFileSystem implements AbstractFileSystem, Wrapper<AbstractFileSystem> {
    public static final int CHUNK_SIZE = getChunkSize();

    private final AbstractFileSystem delegate;
    private final ChunkCache cache;
    private final int maxFileSizeInBytesForWrite; // We will not cache file writes if they bigger than this value.

    /**
     * @param delegate backing file system
     * @param cacheSizeInBytes upper limit for cache size (bytes)
     * @param expectedMaxFileSizeInBytes expected maximum file size (bytes). Controls default number of chunks in {@link ChunkCacheFsEntry}.
     * @param preallocateRatio value in range [0..1] defining the portion of cache to be immediately allocated
     */
    public ChunkCachingFileSystem(AbstractFileSystem delegate, long cacheSizeInBytes, int expectedMaxFileSizeInBytes, double preallocateRatio) {
        if (cacheSizeInBytes <= 0 || expectedMaxFileSizeInBytes <= 0) {
            throw new IllegalArgumentException("Cache size and max file size must be positive");
        }
        this.delegate = delegate;
        this.cache = new ChunkCache(cacheSizeInBytes, expectedMaxFileSizeInBytes, preallocateRatio);
        this.maxFileSizeInBytesForWrite = expectedMaxFileSizeInBytes;
    }

    private static int getChunkSize() {
        int result = 1024 * Integer.getInteger("TimeBase.fileSystem.chunkedCache.chunkSizeKb", 128);
        if (!BitUtil.isPowerOfTwo(result)) {
            throw new AssertionError("Chunk size must be power of 2");
        }
        return result;
    }

    ChunkCache getCache() {
        return cache;
    }

    @Override
    public AbstractFileSystem getNestedInstance() {
        return delegate;
    }

    /// AbstractFileSystem interface

    @Override
    public boolean isAbsolutePath(String path) {
        return delegate.isAbsolutePath(path);
    }

    @Override
    public AbstractPath createPath(String path) {
        return wrap(delegate.createPath(path));
    }

    @Override
    public AbstractPath createPath(AbstractPath parent, String child) {
        return wrap(delegate.createPath(parent, child));
    }

    ChunkCachingAbstractPath wrap(AbstractPath path) {
        return new ChunkCachingAbstractPath(path, this);
    }

    @Override
    public long getReopenOnSeekThreshold() {
        // This cache handles reopen/seek internally so there is no need to do that manually.
        return 0;
    }

    @Override
    public int getPrefetchSize() {
        return delegate.getPrefetchSize();
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    int getMaxFileSizeInBytesForWrite() {
        return maxFileSizeInBytesForWrite;
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }
}
