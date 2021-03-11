package com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor;

/**
 * Interface for classes that can fill data into byte arrays.
 */
public interface ByteDataProvider {
    /**
     * Writes data into that receiver.
     *
     * @param dest destination byte array
     * @param destOffset offset in the {@code dest} from which data will be written
     * @param startSequence destination (first sequence) of data in this provider
     * @param length amount of bytes to write
     */
    void read(byte[] dest, int destOffset, long startSequence, int length);
}
