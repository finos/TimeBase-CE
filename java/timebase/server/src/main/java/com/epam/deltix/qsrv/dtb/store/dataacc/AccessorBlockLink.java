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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.qsrv.dtb.store.codecs.SymmetricSizeCodec;
import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public final class AccessorBlockLink {
    public static final long        NO_NEXT_TIMESTAMP = Long.MAX_VALUE;
    
    private final BlockAccessorBase accessor;
    private final int               entity;
    private final DataBlock         block;
    private int                     offset;
    private volatile long           nextTimestamp; // access atEnd() required volatile
    volatile boolean                queued = false;

    private final TimeSlice         ts;

    public AccessorBlockLink (
            BlockAccessorBase       accessor,
            TimeSlice               ts,
            DataBlock               block)
    {
        this.accessor = accessor;
        this.ts = ts;
        this.block = block;
        this.offset = 0;
        this.entity = block.getEntity ();
        this.nextTimestamp = block.getStartTime();
    }

    private int                      getCurrentOffset(MemoryDataInput in) {
        return in.getCurrentOffset() - block.getStartOffset();
    }

    private int                      getStartOffset(MemoryDataInput in) {
        return in.getStart() - block.getStartOffset();
    }

    private void                    seekOffset(MemoryDataInput in, int offset) {
        in.seek(offset);
    }

    /**
     * Positioning to the given time, if exists.
     * @param nstime nanoseconds to forward
     * @return Return true if not EOF, otherwise false
     */
    public boolean                     forward(long nstime) {
        synchronized (block) {
            MemoryDataInput     mdi = getMDI ();

            for (;;) {
                if (!mdi.hasAvail ()) {
                    offset = getCurrentOffset(mdi);
                    nextTimestamp = NO_NEXT_TIMESTAMP;

                    return false;
                }

                offset = getCurrentOffset (mdi);

                nextTimestamp = TimeCodec.readNanoTime (mdi);

                if (nextTimestamp >= nstime)
                    break;

                skipMessageBody(mdi);
                SymmetricSizeCodec.skipForward (mdi);
            }
        }

        return true;
    }

    /**
     * Positioning to the last message with given time, if exists.
     * @param nstime nanoseconds to forward
     * @return Return true if not EOF, otherwise false
     */
    public boolean                     forwardToLast(long nstime) {
        synchronized (block) {
            MemoryDataInput     mdi = getMDI ();

            for (;;) {
                if (!mdi.hasAvail ()) {
                    offset = getCurrentOffset(mdi);
                    nextTimestamp = NO_NEXT_TIMESTAMP;

                    return false;
                }

                offset = getCurrentOffset (mdi);

                nextTimestamp = TimeCodec.readNanoTime (mdi);

                if (nextTimestamp > nstime)
                    break;

                skipMessageBody(mdi);
                SymmetricSizeCodec.skipForward (mdi);
            }
        }

        return true;
    }

    private void        skipMessageBody(MemoryDataInput mdi) {
        mdi.skipBytes(1); // sequence + type
        int bodyLength = SymmetricSizeCodec.readForward (mdi);
        mdi.skipBytes (bodyLength);
    }

    //
    //  PACKAGE INTERFACE
    //    
    boolean                         isBlock (DataBlock db) {
        return (block == db);
    }
    
    public long                     getNextTimestamp () {
        return nextTimestamp;
    }
    
    int                             getEntity () {
        return entity;
    }

    /**
      *  Seeks offset pointing first message with given time or end of block.
      *  If messages with given time is not present, offset will point to the last message with time < nstime
      *  Returns last message timestamp
     */
    private long                    seek (long nstime) {

        assert Thread.holdsLock(this.block);

        long current = nextTimestamp;

        if (nstime > nextTimestamp) {
            MemoryDataInput             mdi = getMDI ();

            for (;;) {
                if (!mdi.hasAvail ()) {
                    nextTimestamp = NO_NEXT_TIMESTAMP;
                    break;
                }

                nextTimestamp = TimeCodec.readNanoTime (mdi);

                if (nextTimestamp >= nstime)
                    break;

                skipMessageBody(mdi);
                SymmetricSizeCodec.skipForward (mdi);

                offset = getCurrentOffset(mdi);

                current = nextTimestamp;
            }
        } else if (nstime <= nextTimestamp) {

            MemoryDataInput     mdi = getMDI ();

            // should navigate one message before to know previous time
            for (;;) {

                final boolean             hasMore = offset != 0;

                if (hasMore) {
//                    final byte []         bytes = mdi.getBytes ();
//                    final int             szfsz =
//                            SymmetricSizeCodec.endByteToFieldSize (bytes [offset - 1]);
//                    final int             sz =
//                            SymmetricSizeCodec.read (bytes, offset - szfsz);
//
//                    assert sz > 0;

                    int pos = offset;
                    offset -= SymmetricSizeCodec.readBackward(mdi, block.getStartOffset() + offset);

                    if (getStartOffset(mdi) > offset)
                        mdi = getMDI();
                    else
                        seekOffset(mdi, offset);

                    long time = TimeCodec.readNanoTime(mdi);

                    if (time < nstime) {
                        current = time;
                        offset = pos;
                        break;
                    } else {
                        nextTimestamp = time;
                    }
                }
                else {
                    break;
                }
            }
        }

        return current;
    }

    /*
        Search & positioning to middle of data.
        Returns timestamp of message found in the middle
     */
    public long                     seekMiddle () {

        long current = nextTimestamp;

        synchronized (block) {
            long middle = block.getDataLength() / 2;

            offset = 0;
            MemoryDataInput mdi = getMDI();

            if (offset < middle) { // search forward

                for (;;) {
                    if (!mdi.hasAvail()) {
                        nextTimestamp = NO_NEXT_TIMESTAMP;
                        break;
                    }

                    nextTimestamp = TimeCodec.readNanoTime(mdi);
                    skipMessageBody(mdi);
                    SymmetricSizeCodec.skipForward(mdi);

                    if (getCurrentOffset(mdi) >= middle && current != nextTimestamp)
                        break;

                    offset = getCurrentOffset(mdi);

                    current = nextTimestamp;
                }
            }
        }

        return current;
    }
    

    public long                     find (long ts) {

        synchronized (block) {
            if (ts < block.getStartTime())
                return 0;

            if (ts >= block.getEndTime())
                return block.getDataLength();

            long time = seek(ts);
            return (time != NO_NEXT_TIMESTAMP) ? offset : block.getDataLength();
        }
    }
    
    void                            insertMessage (
        long                            nstime,
        byte []                         src, 
        int                             srcOffset, 
        int                             length
    )
    {
        int msgOffset;

        boolean dirty = false;

        synchronized (block) {
            seek(nstime + 1); // seek last message with same time
            dirty = block.insertMessage(accessor, nstime, src, srcOffset, length, offset);

            msgOffset = offset;
            offset += length;

            ts.dataInserted (accessor, block, msgOffset, length, nstime);
        }

        if (dirty)
            ts.blockGoesDirty(block);
    }

    int                         readMessageForward (TSMessageConsumer processor) {
        synchronized (block) {
            MemoryDataInput     mdi = getMDI ();

            boolean             hasMore = mdi.hasAvail ();

            if (!hasMore) {
                nextTimestamp = NO_NEXT_TIMESTAMP;
                return NextState.NONE;
            }

            readMessage(mdi, processor);
            SymmetricSizeCodec.skipForward(mdi);

            offset = getCurrentOffset(mdi);

            hasMore = mdi.hasAvail ();
            if (hasMore)
                nextTimestamp = TimeCodec.readNanoTime (mdi);
            else
                nextTimestamp = NO_NEXT_TIMESTAMP;
            
            return (hasMore ? NextState.HAS_ALL : NextState.HAS_CURRENT);
        }
    }

    int                         readMessageReverse (TSMessageConsumer processor) {
        synchronized (block) {
            MemoryDataInput     mdi = getMDI ();

            int result = mdi.hasAvail() ? NextState.HAS_CURRENT : NextState.NONE;

            if (mdi.hasAvail())
                readMessage (mdi, processor);

            final boolean             hasMore = offset != 0;

            if (hasMore) {
//                final byte []         bytes = mdi.getBytes ();
//                final int             szfsz =
//                    SymmetricSizeCodec.endByteToFieldSize (bytes [offset - 1]);
//                final int             sz =
//                    SymmetricSizeCodec.read (bytes, offset - szfsz);
//
//                offset = offset - szfsz - sz;
                offset -= SymmetricSizeCodec.readBackward(mdi, block.getStartOffset() + offset);

                if (getStartOffset(mdi) > offset)
                    mdi = getMDI();
                else
                    seekOffset (mdi, offset);

//                if (mdi.getStart() > offset)
//                    mdi = getMDI();
//                else
//                    mdi.seekOffset (offset);

                nextTimestamp = TimeCodec.readNanoTime (mdi);
            }
            else
                nextTimestamp = NO_NEXT_TIMESTAMP;

            return hasMore ? (result | NextState.HAS_MORE) : result;
        }
    }
    
    MemoryDataInput                 getMDI () {
        assert Thread.holdsLock (block);

        MemoryDataInput     mdi = accessor.mdi;

        //System.out.println(Thread.currentThread() + " reading " + input.hashCode());
        block.configure (mdi, offset);

        return (mdi);
    }

//    boolean                         atStart () {
//        return (offset == 0);
//    }

    boolean                         atEnd () {
        // called under synchronized (block) only
        return (nextTimestamp == NO_NEXT_TIMESTAMP && offset == block.getDataLength());
    }
    
    void                            asyncDataInserted (int insertionOffset, int msgLength, long timestamp) {
        synchronized (block) {
            int dist = insertionOffset - offset;

            if (dist < 0)
                offset += msgLength;
            else if (dist == 0) {
                assert timestamp <= nextTimestamp;  // FIX for reverse

                nextTimestamp = timestamp;
            } else {
                assert nextTimestamp != NO_NEXT_TIMESTAMP;
            }
        }
    }

    void                            asyncDataDropped (int dataOffset, int length, long nextTime) {
        synchronized (block) {
            int dist = offset - dataOffset;

            if (dist > 0) {
                if (length <= dist) {
                    offset -= length;
                } else {
                    offset = dataOffset;
                    nextTimestamp = nextTime;
                }
            } else if (dist == 0) {
                nextTimestamp = nextTime;
            }
        }
    }

    /**
     * Returns true if block contains data having timstamps that more then given time
     * @param nstime time to search
     * @return true, if block contains data with grater timestamps
     */
    public boolean                            hasMoreTime(long nstime) {
        synchronized (block) {
            seek(nstime);
            return block.getDataLength() - offset > 0;
        }
    }

    /*
        Split block to the given time
        Returns number of freed bytes
     */
    public long                     split(long nstime, final DataBlock next) {

        int length;
        boolean dirty;

        synchronized (block) {
            long time = seek(nstime);

            if (offset > 0)
                assert time <= nstime;

            length = block.getDataLength() - offset;

            next.setData(block.getData(), offset, length, nextTimestamp, block.getEndTime());

            dirty = block.shorten(offset, time);

            nextTimestamp = NO_NEXT_TIMESTAMP;
            ts.dataDropped(accessor, block, offset, length, NO_NEXT_TIMESTAMP);
        }

        if (dirty)
            ts.blockGoesDirty(block);

        return length;
    }

    /*
        Cuts time range from the block.
        Returns number of freed bytes
     */
    public long                   cut (long startTime, long endTime) {
        int length = 0;

        boolean dirty = false;

        synchronized (block) {
            long ns0 = seek(startTime);
            int start = offset;

            seek(endTime != NO_NEXT_TIMESTAMP ? endTime + 1 : endTime);
            int end = offset;

            // check bounds
            if (start == 0)
                block.setStartTime(nextTimestamp);

            if (nextTimestamp == NO_NEXT_TIMESTAMP)
                block.setEndTime(start != 0 ? ns0 : Long.MIN_VALUE);

            if (start < end)
               dirty = block.cut(start, end);

            length = end - start;

            if (start < end)
                ts.dataDropped(accessor, block, start, length, nextTimestamp);
        }

        if (dirty)
            ts.blockGoesDirty(block);

        return length;
    }

    public boolean                  isEmpty() {
        synchronized (block) {
            return block.getDataLength() == 0;
        }
    }

    /*
        Truncates to given nanosecond time.
        Returns number of freed bytes;
     */
    public long                     truncate(long nstime) {
        int length;
        boolean dirty;

        synchronized (block) {
            long time = seek(nstime);
            length = block.getDataLength() - offset;
            dirty = block.shorten(offset, time);
            nextTimestamp = NO_NEXT_TIMESTAMP;

            if (length > 0)
                ts.dataDropped(accessor, block, offset, length, NO_NEXT_TIMESTAMP);
        }

        if (dirty)
            ts.blockGoesDirty(block);

        return length;
    }

    private int                     skipMessage (MemoryDataInput mdi)
    {
        synchronized (block) {
            long check = TimeCodec.readNanoTime(mdi);

            if (nextTimestamp != check)
                throw new IllegalStateException(
                        "Timestamp discrepancy (missed update?): expected=" +
                                nextTimestamp + "; read " + check
                );

            skipMessageBody(mdi);
            return getCurrentOffset(mdi); // tail
        }
    }

    private void                     readMessage (
        MemoryDataInput                 mdi, 
        TSMessageConsumer               processor
    )
    {
        long            check = TimeCodec.readNanoTime (mdi);
        
        if (nextTimestamp != check) {
            if (nextTimestamp != NO_NEXT_TIMESTAMP)
                throw new IllegalStateException (
                        "[" + entity + "] Timestamp discrepancy (missed update?): expected=" +
                                nextTimestamp + "; read " + check);
            else
            nextTimestamp = check;
        }
       
        int             type = mdi.readUnsignedByte ();
        int             bodyLength = SymmetricSizeCodec.readForward (mdi);

        //assert bodyLength > 0;

        int             tailOffset = getCurrentOffset (mdi) + bodyLength;
        
        processor.process (entity, nextTimestamp, type, bodyLength, mdi);

        int current = getCurrentOffset(mdi);

        if (current > tailOffset)
            throw new RuntimeException (processor + " illegally looked ahead");

        mdi.skipBytes(tailOffset - current);
    }

    public long                 getEndTime() {
        synchronized (block) {
            return block.getEndTime();
        }
    }

    public long                 getStartTime() {
        synchronized (block) {
            return block.getStartTime();
        }
    }
}