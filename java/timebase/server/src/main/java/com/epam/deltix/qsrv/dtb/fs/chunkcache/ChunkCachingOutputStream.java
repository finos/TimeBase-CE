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

import com.google.common.primitives.Ints;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.Chunk;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.ChunkPool;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes data both to cache and the delegated output stream.
 */
@ParametersAreNonnullByDefault
class ChunkCachingOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final ChunkCachingAbstractPath path;
    private final ChunkCache cache;

    private final long expectedSizeInBytes;
    private boolean sizeOverflow = false;

    private final List<Chunk> chunks; // Contains completely filled chunks

    private long offset = 0; // Offset of next byte to write
    private long chunkOffset = -1; // Offset of first chunk byte
    private long chunkLimit = -1; // Offset of first chunk byte
    private Chunk chunkObject = null; // Current chunk
    private byte[] chunkArray = null;
    private boolean closed = false;

    ChunkCachingOutputStream(OutputStream delegate, ChunkCachingAbstractPath path, ChunkCache cache, long expectedSizeInBytes) {
        this.delegate = delegate;
        this.path = path;
        this.cache = cache;
        this.expectedSizeInBytes = expectedSizeInBytes;

        int expectedChunkCount = expectedSizeInBytes > 0 ? Ints.checkedCast(expectedSizeInBytes / ChunkCachingFileSystem.CHUNK_SIZE) : 10;
        this.chunks = new ArrayList<>(expectedChunkCount);
    }

    @Override
    public void write(int b) throws IOException {
        assertOpen();

        if (offset >= chunkLimit) {
            changeChunk();
        }

        chunkArray[(int) (offset - chunkOffset)] = (byte) b;
        offset++;

        delegate.write(b);

        checkSize();
    }

    @Override
    public void write(byte[] block) throws IOException {
        write(block, 0, block.length);
    }

    @Override
    public void write(byte[] block, int blockOffset, int blockLength) throws IOException {
        assertOpen();

        int bytesWritten = 0;
        while (bytesWritten < blockLength) {
            if (offset >= chunkLimit) {
                changeChunk();
            }
            assert offset >= chunkOffset;
            assert offset < chunkLimit;

            int bytesToCopy = Math.min(blockLength - bytesWritten, (int) (chunkLimit - offset));
            System.arraycopy(block, blockOffset + bytesWritten, chunkArray, (int) (offset - chunkOffset), bytesToCopy);
            offset += bytesToCopy;
            bytesWritten += bytesToCopy;
        }

        delegate.write(block, blockOffset, blockLength);

        checkSize();
    }

    private void checkSize() {
        if (!sizeOverflow && offset > expectedSizeInBytes) {
            sizeOverflow = true;
            ChunkCache.LOG.warn("File size is bigger than expected. Expected size: %s. %s bytes already written. Path: %s")
                    .with(expectedSizeInBytes)
                    .with(offset)
                    .with(path.getPathString());
        }
    }

    private void changeChunk() {
        if (chunkObject != null) {
            chunks.add(chunkObject);
        }

        int chunkIndex = ChunkCacheFsEntry.getChunkIndex(offset);
        long newChunkOffset = ChunkCacheFsEntry.getChunkStartByIndex(chunkIndex);

        this.chunkObject = cache.allocateNewChunk();
        this.chunkArray = chunkObject.getArray();
        this.chunkOffset = newChunkOffset;
        this.chunkLimit = newChunkOffset + ChunkCacheFsEntry.CHUNK_SIZE;
    }

    @Override
    public void flush() throws IOException {
        // Note: we don't store data in the cache. We fill store only complete file.
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            delegate.close();

            storeDataToCache();
            closed = true;
        }
    }

    private void assertOpen() {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
    }

    private void storeDataToCache() {
        int chunkIndex = 0;
        ChunkPool chunkPool = cache.getChunkPool();
        ChunkCacheFsEntry entry = cache.getOrCreateEntry(path);
        synchronized (entry) {
            entry.invalidate(chunkPool);
            for (Chunk chunk : chunks) {
                entry.putChunk(chunkIndex, chunk, ChunkCacheFsEntry.CHUNK_SIZE, chunkPool);
                chunk.release(chunkPool);
                chunkIndex ++;
            }
            chunks.clear();
            if (chunkObject != null) {
                int lastChunkData = (int) (offset - chunkOffset);
                entry.putChunk(chunkIndex, chunkObject, lastChunkData, chunkPool);
                chunkObject.release(chunkPool);
                chunkObject = null;
            }
        }
    }

}
