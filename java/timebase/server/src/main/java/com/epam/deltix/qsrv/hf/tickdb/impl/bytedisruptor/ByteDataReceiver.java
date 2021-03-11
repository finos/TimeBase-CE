package com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor;

/**
 * Interface for classes that can accept in data in form of byte arrays.
 */
public interface ByteDataReceiver {
    /**
     * Writes data into that receiver.
     *
     * @param src source byte array
     * @param srcOffset offset of data in the {@code src}
     * @param startSequence destination (first sequence) of data in this reciever
     * @param length amount of bytes to write
     */
    void write(byte[] src, int srcOffset, long startSequence, int length);
}
