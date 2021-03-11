/*
 * Copyright 2011 LMAX Ltd.
 * Copyright 2016 Deltix.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor;


import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import com.epam.deltix.util.collections.ByteContainer;

/**
 * Modification of {@link RingBuffer} that works works with bytes instead of objects.
 */
public final class ByteRingBuffer implements Cursored, ByteDataProvider, ByteDataReceiver, ByteContainer {

    private final int indexMask;
    private final byte[] entries;
    private final int bufferSize;
    private final Sequencer sequencer;

    /**
     * Construct a RingBuffer with the full option set.
     *
     * @param sequencer sequencer to handle the ordering of events moving through the RingBuffer.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    ByteRingBuffer(Sequencer sequencer)
    {
        this.sequencer    = sequencer;
        this.bufferSize   = sequencer.getBufferSize();

        if (bufferSize < 1)
        {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1)
        {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }

        this.indexMask = bufferSize - 1;
        this.entries   = new byte[sequencer.getBufferSize()];
    }

    /**
     * Create a new multiple producer RingBuffer with the specified wait strategy.
     *
     * @see MultiProducerSequencer
     * @param bufferSize number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    public static ByteRingBuffer createMultiProducer(int bufferSize,
                                                         WaitStrategy waitStrategy)
    {
        MultiProducerSequencer sequencer = new MultiProducerSequencer(bufferSize, waitStrategy);

        return new ByteRingBuffer(sequencer);
    }

    /**
     * Create a new multiple producer RingBuffer using the default wait strategy  {@link BlockingWaitStrategy}.
     *
     * @see MultiProducerSequencer
     * @param bufferSize number of elements to create within the ring buffer.
     * @throws IllegalArgumentException if <code>bufferSize</code> is less than 1 or not a power of 2
     */
    public static ByteRingBuffer createMultiProducer(int bufferSize)
    {
        return createMultiProducer(bufferSize, new BlockingWaitStrategy());
    }

    /**
     * Create a new single producer RingBuffer with the specified wait strategy.
     *
     * @see SingleProducerSequencer
     * @param bufferSize number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    public static ByteRingBuffer createSingleProducer(int bufferSize,
                                                          WaitStrategy waitStrategy)
    {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);

        return new ByteRingBuffer(sequencer);
    }

    /**
     * Create a new single producer RingBuffer using the default wait strategy  {@link BlockingWaitStrategy}.
     *
     * @see MultiProducerSequencer
     * @param bufferSize number of elements to create within the ring buffer.
     * @throws IllegalArgumentException if <code>bufferSize</code> is less than 1 or not a power of 2
     */
    public static ByteRingBuffer createSingleProducer(int bufferSize)
    {
        return createSingleProducer(bufferSize, new BlockingWaitStrategy());
    }

    /**
     * Create a new Ring Buffer with the specified producer type (SINGLE or MULTI)
     *
     * @param producerType producer type to use {@link ProducerType}.
     * @param bufferSize number of elements to create within the ring buffer.
     * @param waitStrategy used to determine how to wait for new elements to become available.
     * @throws IllegalArgumentException if bufferSize is less than 1 or not a power of 2
     */
    public static ByteRingBuffer create(ProducerType producerType,
                                            int bufferSize,
                                            WaitStrategy waitStrategy)
    {
        switch (producerType)
        {
        case SINGLE:
            return createSingleProducer(bufferSize, waitStrategy);
        case MULTI:
            return createMultiProducer(bufferSize, waitStrategy);
        default:
            throw new IllegalStateException(producerType.toString());
        }
    }

    /**
     * <p>Get the event for a given sequence in the RingBuffer.</p>
     *
     * <p>This call has 2 uses.  Firstly use this call when publishing to a ring buffer.
     * After calling {@link ByteRingBuffer#next()} use this call to get hold of the
     * preallocated event to fill with data before calling {@link ByteRingBuffer#publish(long)}.</p>
     *
     * <p>Secondly use this call when consuming data from the ring buffer.  After calling
     * {@link SequenceBarrier#waitFor(long)} call this method with any value greater than
     * that your current consumer sequence and less than or equal to the value returned from
     * the {@link SequenceBarrier#waitFor(long)} method.</p>
     *
     * @param sequence for the event
     * @return the event for the given sequence
     */
    public byte get(long sequence)
    {
        return entries[(int)sequence & indexMask];
    }

    public byte get(int truncatedSequence)
    {
        return entries[truncatedSequence & indexMask];
    }

    /**
     * Writes data to the Ring Buffer from byte array.
     *
     * @param src source byte array
     * @param srcOffset offset of data in byte array
     * @param length length of data
     */
    public void write(byte[] src, int srcOffset, int length) {
        long hi = sequencer.next(length); // Inclusive
        long lo = hi - length + 1;
        assert hi - lo + 1 == length;
        write(src, srcOffset, lo, length);
        sequencer.publish(lo, hi);
    }

    public <T> long writeInteractive(T event, InteractiveEventWriter<T> interactiveEventWriter) {
        int length = interactiveEventWriter.prepare(event);
        long hi = sequencer.next(length); // Inclusive
        return writeToAllocatedRange(interactiveEventWriter, hi, length);
    }

    public <T> long writeToAllocatedRange(InteractiveEventWriter<T> interactiveEventWriter, long hi, int length) {
        long lo = hi - length + 1;
        assert hi - lo + 1 == length;
        interactiveEventWriter.write(this, lo, length);
        sequencer.publish(lo, hi);
        return lo;
    }

    /**
     * Copies data to RingBuffer from provided array.
     * This method should be used only if you have acquired corresponding slot.
     *
     * @param src array to copy from
     * @param srcOffset offset in src array to copy from
     * @param startSequence sequence number for the first position to write
     * @param length number of byes to copy
     */
    @Override
    public void write(byte[] src, int srcOffset, long startSequence, int length) {
        assert length > 0;
        assert length <= bufferSize;

        long endIndex = startSequence + length; // Non inclusive
        int actualStartIndex = (int) (startSequence & indexMask);
        int actualEndIndex = (int) (endIndex & indexMask);
        if (actualStartIndex < actualEndIndex || actualEndIndex == 0) {
            // No ring wrap
            System.arraycopy(src, srcOffset, entries, actualStartIndex, length);
        } else {
            // Ring wrap: we have to split our block in two parts.
            int headLen = bufferSize - actualStartIndex;
            assert headLen < length;
            int tailLen = length - headLen;
            assert headLen + tailLen == length;
            // First part is written to the end of our buffer
            System.arraycopy(src, srcOffset, entries, actualStartIndex, headLen);
            // Second part is written to the beginning of buffer.
            System.arraycopy(src, srcOffset + headLen, entries, 0, tailLen);
        }
    }

    /**
     * Copies data from RingBuffer to provided array.
     *
     * @param dest array to copy to
     * @param destOffset offset in destination array to copy to
     * @param startSequence sequence number for the first position to copy
     * @param length number of byes to copy
     */
    @Override
    public void read(byte[] dest, int destOffset, long startSequence, int length) {
        long endSequence = startSequence + length; // Non inclusive
        int actualStartIndex = (int) (startSequence & indexMask);
        int actualEndIndex = (int) (endSequence & indexMask);
        // Note: actualEndIndex == 0 means that written data is exactly fills buffer till the end but not overlaps
        if (actualStartIndex < actualEndIndex || actualEndIndex == 0) {
            // No ring wrap
            System.arraycopy(entries, actualStartIndex, dest, destOffset, length);
        } else {
            // Ring wrap: we have to split our block in two parts.
            int headLen = bufferSize - actualStartIndex;
            assert headLen < length;
            int tailLen = length - headLen;
            assert headLen + tailLen == length;
            // First part is written to the end of our buffer
            System.arraycopy(entries, actualStartIndex, dest, destOffset, headLen);
            // Second part is written to the beginning of buffer.
            System.arraycopy(entries, 0, dest, destOffset + headLen, tailLen);
        }
    }

    /**
     * Increment and return the next sequence for the ring buffer.  Calls of this
     * method should ensure that they always publish the sequence afterward.  E.g.
     * <pre>
     * long sequence = ringBuffer.next();
     * try {
     *     Event e = ringBuffer.get(sequence);
     *     // Do some work with the event.
     * } finally {
     *     ringBuffer.publish(sequence);
     * }
     * </pre>
     * @see ByteRingBuffer#publish(long)
     * @see ByteRingBuffer#get(long)
     * @return The next sequence to publish to.
     */
    public long next()
    {
        return sequencer.next();
    }

    /**
     * The same functionality as {@link ByteRingBuffer#next()}, but allows the caller to claim
     * the next n sequences.
     *
     * @see Sequencer#next(int)
     * @param n number of slots to claim
     * @return sequence number of the highest slot claimed
     */
    public long next(int n)
    {
        return sequencer.next(n);
    }

    /**
     * <p>Increment and return the next sequence for the ring buffer.  Calls of this
     * method should ensure that they always publish the sequence afterward.  E.g.
     * <pre>
     * long sequence = ringBuffer.next();
     * try {
     *     Event e = ringBuffer.get(sequence);
     *     // Do some work with the event.
     * } finally {
     *     ringBuffer.publish(sequence);
     * }
     * </pre>
     * <p>This method will not block if there is not space available in the ring
     * buffer, instead it will throw an {@link InsufficientCapacityException}.
     *
     *
     * @see ByteRingBuffer#publish(long)
     * @see ByteRingBuffer#get(long)
     * @return The next sequence to publish to.
     * @throws InsufficientCapacityException if the necessary space in the ring buffer is not available
     */
    public long tryNext() throws InsufficientCapacityException
    {
        return sequencer.tryNext();
    }

    /**
     * The same functionality as {@link ByteRingBuffer#tryNext()}, but allows the caller to attempt
     * to claim the next n sequences.
     *
     * @param n number of slots to claim
     * @return sequence number of the highest slot claimed
     * @throws InsufficientCapacityException if the necessary space in the ring buffer is not available
     */
    public long tryNext(int n) throws InsufficientCapacityException
    {
        return sequencer.tryNext(n);
    }

    /**
     * Resets the cursor to a specific value.  This can be applied at any time, but it is worth not
     * that it is a racy thing to do and should only be used in controlled circumstances.  E.g. during
     * initialisation.
     *
     * @param sequence The sequence to reset too.
     * @throws IllegalStateException If any gating sequences have already been specified.
     */
    public void resetTo(long sequence)
    {
        sequencer.claim(sequence);
        sequencer.publish(sequence);
    }

    /**
     * Sets the cursor to a specific sequence and returns the preallocated entry that is stored there.  This
     * is another deliberately racy call, that should only be done in controlled circumstances, e.g. initialisation.
     *
     * @param sequence The sequence to claim.
     * @return The preallocated event.
     */
    public Object claimAndGetPreallocated(long sequence)
    {
        sequencer.claim(sequence);
        return get(sequence);
    }

    /**
     * Determines if a particular entry has been published.
     *
     * @param sequence The sequence to identify the entry.
     * @return If the value has been published or not.
     */
    public boolean isPublished(long sequence)
    {
        return sequencer.isAvailable(sequence);
    }

    /**
     * Add the specified gating sequences to this instance of the Disruptor.  They will
     * safely and atomically added to the list of gating sequences.
     *
     * @param gatingSequences The sequences to add.
     */
    public void addGatingSequences(Sequence... gatingSequences)
    {
        sequencer.addGatingSequences(gatingSequences);
    }

    /**
     * Get the minimum sequence value from all of the gating sequences
     * added to this ringBuffer.
     *
     * @return The minimum gating sequence or the cursor sequence if
     * no sequences have been added.
     */
    public long getMinimumGatingSequence()
    {
        return sequencer.getMinimumSequence();
    }

    /**
     * Remove the specified sequence from this ringBuffer.
     *
     * @param sequence to be removed.
     * @return <code>true</code> if this sequence was found, <code>false</code> otherwise.
     */
    public boolean removeGatingSequence(Sequence sequence)
    {
        return sequencer.removeGatingSequence(sequence);
    }

    /**
     * Create a new SequenceBarrier to be used by an EventProcessor to track which messages
     * are available to be read from the ring buffer given a list of sequences to track.
     *
     * @see SequenceBarrier
     * @param sequencesToTrack the additional sequences to track
     * @return A sequence barrier that will track the specified sequences.
     */
    public SequenceBarrier newBarrier(Sequence... sequencesToTrack)
    {
        return sequencer.newBarrier(sequencesToTrack);
    }

    /**
     * Get the current cursor value for the ring buffer.  The cursor value is
     * the last value that was published, or the highest available sequence
     * that can be consumed.
     */
    public long getCursor()
    {
        return sequencer.getCursor();
    }

    /**
     * The size of the buffer.
     */
    public int getBufferSize()
    {
        return bufferSize;
    }

    /**
     * Given specified <code>requiredCapacity</code> determines if that amount of space
     * is available.  Note, you can not assume that if this method returns <code>true</code>
     * that a call to {@link ByteRingBuffer#next()} will not block.  Especially true if this
     * ring buffer is set up to handle multiple producers.
     *
     * @param requiredCapacity The capacity to check for.
     * @return <code>true</code> If the specified <code>requiredCapacity</code> is available
     * <code>false</code> if now.
     */
    public boolean hasAvailableCapacity(int requiredCapacity)
    {
        return sequencer.hasAvailableCapacity(requiredCapacity);
    }

    /**
     * Publish the specified sequence.  This action marks this particular
     * message as being available to be read.
     *
     * @param sequence the sequence to publish.
     */
    public void publish(long sequence)
    {
        sequencer.publish(sequence);
    }

    /**
     * Publish the specified sequences.  This action marks these particular
     * messages as being available to be read.
     *
     * @see Sequencer#next(int)
     * @param lo the lowest sequence number to be published
     * @param hi the highest sequence number to be published
     */
    public void publish(long lo, long hi)
    {
        sequencer.publish(lo, hi);
    }

    /**
     * Get the remaining capacity for this ringBuffer.
     * @return The number of slots remaining.
     */
    public long remainingCapacity()
    {
        return sequencer.remainingCapacity();
    }
}
