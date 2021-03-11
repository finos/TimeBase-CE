package com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor;

/**
 * Controls space allocation and write execution for {@link ByteRingBuffer}.
 *
 * <p>Users of this interface will always call {@link #prepare} and then {@link #write}.
 *
 * <p>Users of this interface must supply to {@link #write} exactly same {@code length} as was returned by preceding {@link #prepare} call.
 *
 * @author Alexei Osipov
 */
public interface InteractiveEventWriter<T> {
    /**
     * Stores event in internal buffer and returns it's size in bytes.
     *
     * @param event event to serialize
     * @return length of serialized event representation in bytes
     */
    int prepare(T event);

    /**
     * Writes data to the {@link ByteDataReceiver}. May not block or wait.
     *
     * @param byteRingBuffer data destination
     * @param startSequence starting sequence to write data to
     * @param length numbers of bytes to write. Must match to value returned by {@link #prepare}.
     */
    void write(ByteDataReceiver byteRingBuffer, long startSequence, int length);
}
