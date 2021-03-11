package com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool;

import com.epam.deltix.qsrv.dtb.fs.chunkcache.ChunkCachingFileSystem;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wrapper object for a fixed-size reusable binary data array.
 *
 * @author Alexei Osipov
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class Chunk {
    private final AtomicInteger usageCounter;
    private final byte[] data;

    public Chunk() {
        this.data = new byte[ChunkCachingFileSystem.CHUNK_SIZE];
        this.usageCounter = new AtomicInteger(0);
    }

    /**
     * Sets usage count to one. This should be first usage increment after extraction from pool.
     */
    public void allocate() {
        int newCount = usageCounter.incrementAndGet();
        if (newCount != 1) {
            throw new IllegalStateException("Attempt to allocate chunk with wrong usage count:" + newCount);
        }
    }

    /**
     * Increments usage by one. The chunk MUST be in use by somebody else. It's not permitted to increment usage count if it's zero.
     * If you need to use chunk for the first time then use {@link #allocate()}.
     */
    public void addUse() {
        int result = usageCounter.incrementAndGet();
        if (result <= 1) {
            throw new IllegalStateException("Attempt to add usage to a chunk that already supposed to be collected");
        }
    }

    public void release(ChunkPool chunkPool) {
        int newCount = usageCounter.decrementAndGet();
        if (newCount < 0) {
            throw new IllegalStateException("Attempt to release chunk more than once");
        }
        if (newCount == 0) {
            chunkPool.putChunk(this);
        }
    }

    public byte[] getArray() {
        return data;
    }

    public int getUsageCount() {
        return usageCounter.get();
    }
}
