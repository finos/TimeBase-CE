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
import com.epam.deltix.util.io.BasicIOUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
@NotThreadSafe
public class ChunkCacheInputStream extends InputStream {
    private static final ChunkLoadStrategy CHUNK_LOAD_STRATEGY = getChunkLoadStrategy();

    private static final int NO_LIMIT = -1;
    private Chunk chunkObject;

    @Nonnull
    private static ChunkLoadStrategy getChunkLoadStrategy() {
        String defaultValue = ChunkLoadStrategy.MODE_1.toString();
        String value = System.getProperty("TimeBase.fileSystem.chunkedCache.loadStrategy", defaultValue).toUpperCase();
        return ChunkLoadStrategy.valueOf(value);
    }

    private final ChunkCachingAbstractPath path;
    private final ChunkCache cache;

    private long offset; // offset of next byte to read

    private byte[] currentChunk = null; // Current pre-loaded chunk of data
    private long chunkOffset = -1; // First position for data for this chunk
    private long chunkLimit = -1; // First position for data AFTER this chunk

    private long wrappedStreamOffset = -1;
    private long wrappedStreamLimit = -1; // if >= 0 then this is the limit for data in the requested stream. Limit count from the file start (not stream start).
    private InputStream wrappedStream = null;

    private boolean reachedEnd = false;
    private boolean hadToJump = false;
    private boolean firstChunk = true;

    /**
     * @param offset initial offset
     */
    ChunkCacheInputStream(ChunkCachingAbstractPath path, ChunkCache cache, long offset) {
        this.path = path;
        this.cache = cache;
        this.offset = offset;
    }

    @Override
    public int read() throws IOException {
        if (offset < chunkOffset || offset >= chunkLimit) {
            changeChunk(1);
            if (reachedEnd) {
                return -1;
            }
        }

        int result = currentChunk[(int) (offset - chunkOffset)] & 0xFF;
        offset ++;
        return result;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        int readCount = 0;
        while (readCount < len) {
            if (offset < chunkOffset || offset >= chunkLimit) {
                changeChunk(len - readCount);
                if (reachedEnd) {
                    break;
                }
            }
            assert offset >= chunkOffset;
            assert offset < chunkLimit;
            int bytesToCopy = Math.min(len - readCount, (int) (chunkLimit - offset));
            System.arraycopy(currentChunk, (int) (offset - chunkOffset), b, off + readCount, bytesToCopy);
            offset += bytesToCopy;
            readCount += bytesToCopy;
        }
        if (readCount == 0) {
            return -1;
        } else {
            return readCount;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        offset += n;
        return n;
    }

    @Override
    public int available() throws IOException {
        if (offset >= chunkOffset && offset < chunkLimit) {
            return (int) (chunkLimit - offset);
        } else {
            return 0;
        }
    }

    private void changeChunk(int bytesRequested) throws IOException {
        ChunkCacheFsEntry entry = cache.getOrCreateEntry(path);
        //chunkOffset = ChunkCacheFsEntry.getChunkStart(offset);
        int chunkIndex = ChunkCacheFsEntry.getChunkIndex(offset);
        long newChunkOffset = ChunkCacheFsEntry.getChunkStartByIndex(chunkIndex);

        // TODO: We can decrease synchronization scope but that will mean than two threads may download same data
        // Current synchronization scope: entire file during the download operations.
        // So only one thread can be work on file download at same time.
        // This can be not optimal solution in some cases.
        // TODO: Provide separate lock object for downloads. So we can at least read data in cache while other thread downloads chunks.
        boolean needToLoad;
        synchronized (entry) {
            Chunk chunkObject = entry.getChunk(chunkIndex);
            if (chunkObject != null) {
                useDataFromCachedChunk(entry, chunkIndex, newChunkOffset, chunkObject);
                needToLoad = false;
            } else {
                // We don't have cached entry
                needToLoad = true;
            }
        }
        if (needToLoad) {
            synchronized (entry.getLoadLock()) {
                // Re-check if cache still don't have desired chunk
                synchronized (entry) {
                    Chunk chunkObject = entry.getChunk(chunkIndex);
                    if (chunkObject != null) {
                        // Chunk appeared in cache (loaded by other thread). Use it and cancel load.
                        useDataFromCachedChunk(entry, chunkIndex, newChunkOffset, chunkObject);
                        needToLoad = false;
                    }
                }

                if (needToLoad) {
                    // We hold the load lock and still need to load data. So load desired chunk.
                    int chunksToFulfillRequest = ChunkCacheFsEntry.getNumberOfChunks(offset, bytesRequested);
                    int chunksToLoad = entry.getNumberOfMissingChunks(chunkIndex, chunksToFulfillRequest);
                    assert chunksToLoad > 0;
                    int chunkDataLength = loadChunk(newChunkOffset, chunksToLoad);

                    // Note: there is a chance that entry was evicted from the cache. We still update old entry.
                    synchronized (entry) {
                        entry.putChunk(chunkIndex, this.chunkObject, chunkDataLength, cache.getChunkPool());
                        if (entry.getLoadedChunkCount() > 1) {
                            cache.updateEntrySize(path, entry); // This let's cache know that entry size was changed
                        }
                        this.chunkOffset = newChunkOffset;
                        this.chunkLimit = newChunkOffset + chunkDataLength;
                    }
                }
            }
        }
        this.reachedEnd = offset >= chunkLimit;
        this.firstChunk = false;
    }

    private void useDataFromCachedChunk(ChunkCacheFsEntry entry, int chunkIndex, long newChunkOffset, Chunk chunkObject) {
        releaseChunk();
        this.chunkObject = chunkObject;
        this.currentChunk = chunkObject.getArray();
        this.chunkOffset = newChunkOffset;
        this.chunkLimit = entry.getChunkLimitByIndex(chunkIndex); // Ensure that this call in same sync block with "getChunk"
    }

    private int loadChunk(long newChunkOffset, int requestedChunksToLoad) throws IOException {
        assert newChunkOffset % ChunkCacheFsEntry.CHUNK_SIZE == 0;

        if (wrappedStream != null && wrappedStreamOffset > newChunkOffset) {
            throw new IllegalArgumentException("Attempt to read backwards");
        }

        if (wrappedStream != null && wrappedStreamLimit != NO_LIMIT && offset >= wrappedStreamLimit) {
            // We depleted stream. Close it.
            wrappedStream.close();
            wrappedStream = null;
        }

        if (wrappedStream != null) {
            long skipLength = newChunkOffset - wrappedStreamOffset;
            long reopenOnSeekThreshold = path.getNestedInstance().getFileSystem().getReopenOnSeekThreshold();
            if (reopenOnSeekThreshold != 0 && skipLength >= Math.min(reopenOnSeekThreshold, ChunkCacheFsEntry.CHUNK_SIZE)) {
                // It's better to reopen stream
                wrappedStream.close();
                wrappedStream = null;
                hadToJump = true;
            } else {
                // Preserve stream and skip

                // We should NOT meet end of file here in normal operation because we skip to the place where data should be present
                try {
                    // TODO: we should add data into cache instead of throwing it (or not?)
                    BasicIOUtil.skipFully(wrappedStream, skipLength);
                } catch (EOFException e) {
                    // TODO: handle EOFException
                    throw new RuntimeException("Not implemented", e);
                }
                wrappedStreamOffset += skipLength;
                if (skipLength >= ChunkCacheFsEntry.CHUNK_SIZE) {
                    hadToJump = true;
                }
            }
        }

        if (wrappedStream == null) {
            wrappedStreamOffset = newChunkOffset;

            // Decision point: request bound or unbound block? TODO: Implement tests
            int numberOfChunksToRequest = determineOptimalNumberOfChunksToLoad(path, hadToJump, requestedChunksToLoad, firstChunk);
            if (numberOfChunksToRequest == NO_LIMIT) {
                // No limit
                wrappedStream = path.getNestedInstance().openInput(newChunkOffset);
                wrappedStreamLimit = NO_LIMIT;
            } else {
                //
                int bytesToGet = numberOfChunksToRequest * ChunkCacheFsEntry.CHUNK_SIZE;
                wrappedStream = path.getNestedInstance().openInput(newChunkOffset, bytesToGet);
                wrappedStreamLimit = newChunkOffset + bytesToGet;
            }
        }

        // Chunk allocation. TODO: Use allocator/pool
        releaseChunk();
        this.chunkObject = cache.allocateNewChunk();
        byte[] chunk = chunkObject.getArray();

        int bytesToRead = ChunkCacheFsEntry.CHUNK_SIZE;
        int gotBytes = readFully(wrappedStream, chunk, 0, bytesToRead);
        wrappedStreamOffset += gotBytes;
        if (gotBytes < bytesToRead) {
            // Reached end of file
            wrappedStream.close();
            wrappedStream = null;
        }

        this.currentChunk = chunk;
        return gotBytes;
    }

    /**
     * @param hadToJump if we ever needed to skip a chunk for this stream?
     * @param requestedChunksToLoad number of chunks needed to fulfill current request
     * @param firstChunk is this the first chunk requested for this stream
     * @return number of chunks to load or -1 if no limit
     */
    private static int determineOptimalNumberOfChunksToLoad(ChunkCachingAbstractPath path, boolean hadToJump, int requestedChunksToLoad, boolean firstChunk) {
        if (!path.getNestedInstance().getFileSystem().isReadsWithLimitPreferable()) {
            return NO_LIMIT;
        }

        switch (CHUNK_LOAD_STRATEGY) {
            case ALWAYS_NO_LIMIT:
                return NO_LIMIT;
            case ALWAYS_WHAT_REQUESTED:
                return requestedChunksToLoad;
            case ALWAYS_ONE_CHUNK:
                return 1;
            case MODE_1:
                return mode1Strategy(hadToJump, requestedChunksToLoad, firstChunk);
            default:
                throw new IllegalArgumentException("Unknown strategy: " + CHUNK_LOAD_STRATEGY);
        }
    }

    private static int mode1Strategy(boolean hadToJump, int requestedChunksToLoad, boolean firstChunk) {
        if (firstChunk || hadToJump) {
            // Give exactly what requested
            return requestedChunksToLoad;
        } else {
            // Request without limit
            return -1;
        }
    }

    /**
     * @return number of bytes read. can be less than length only if end of stream reached
     */
    private static int      readFully (
            InputStream             is,
            byte []                 bytes,
            int                     offset,
            int                     length
    )
            throws IOException
    {
        int bytesRead = 0;
        while (length > 0) {
            int     count = is.read (bytes, offset, length);

            if (count < 0) {
                // No more data
                return bytesRead;
            }

            bytesRead += count;
            offset += count;
            length -= count;
        }
        return bytesRead;
    }

    private void releaseChunk() {
        if (chunkObject != null) {
            chunkObject.release(cache.getChunkPool());
            chunkObject = null;
        }
    }

    @Override
    public void close() throws IOException {
        if (wrappedStream != null) {
            wrappedStream.close();
            wrappedStream = null;
        }
        currentChunk = null;
        releaseChunk();
        super.close();
    }
}