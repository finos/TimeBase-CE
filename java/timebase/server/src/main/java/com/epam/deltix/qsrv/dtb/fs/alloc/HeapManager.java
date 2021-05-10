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
