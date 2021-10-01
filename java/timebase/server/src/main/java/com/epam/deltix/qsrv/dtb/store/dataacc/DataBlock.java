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

import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.memory.MemoryDataInput;
import java.io.*;

/**
 *
 */
public final class DataBlock implements DataBlockInfo {
    private static final boolean            DEBUG_VERIFY_BLOCK_ON_WRITE = false;

    private static final int                MIN_BLOCK_SIZE = 256;
    
    private TimeSlice                       ts; 
    private int                             entity;
    private long                            startTime;
    private long                            endTime;
    private int                             length;
    private ByteArray                       data;
    private boolean                         isDirty;

    @Override
    public String                   toString () {
        return "DataBlock{" + "ts=" + ts + ", entity=" + entity + 
            ", startTime=" + startTime + ", endTime=" + endTime + 
            ", length=" + length + ", isDirty=" + isDirty + '}';
    }

    public int                      getStartOffset() {
        return data.getOffset();
    }
    
    @Override
    public int                      getEntity () {
        return (entity);
    }
    
    @Override
    public int                      getDataLength () {
        return (length);
    }

    public void                     configure (MemoryDataInput mdi, int pos) {
        // block already destroyed
        if (data == null)
            throw new IllegalStateException("Block is empty");

        mdi.setBytes(data.getArray(), data.getOffset(pos), length - pos);
    }
    
    private boolean                    setDirty () {
        if (!isDirty) {
            isDirty = true;
            return true;
        }

        return false;
    }

    private void                    alloc () {
        if (data == null)
            data = new ByteArray(Math.max(length, MIN_BLOCK_SIZE)); // @ALLOCATION

        if (data.getLength() < length)
            data.setArray(new byte[length], 0, length); // @ALLOCATION
    }
    
    public void                     initNew (TimeSlice ts, int entity) {
        this.ts = ts;
        this.entity = entity; 
        
        length = 0;
        startTime = Long.MAX_VALUE;
        endTime = Long.MIN_VALUE;
        
        alloc ();
        
        if (setDirty ())
            ts.blockGoesDirty(this);
    }   
            
    public void                     init (
        TimeSlice                       ts,
        int                             entity, 
        int                             length, 
        long                            startTime,
        long                            endTime
    )
    {
        this.ts = ts;
        this.entity = entity; 
        this.length = length;
        this.startTime = startTime;
        this.endTime = endTime;
        
        alloc ();                
    }   
    
    /**
     *  Insert space in the data block.
     *  Returns true, of block goes dirty
     */
    boolean                            insertMessage (
        BlockAccessorBase               accessor,
        long                            timestamp,
        byte []                         src,
        int                             srcOffset,
        int                             msgLength,
        int                             dataOffset
    )
    {
        int         newLength = length + msgLength;
        int         tailSize = length - dataOffset;
        int         oldCapacity = data.getLength();
        
        if (newLength > oldCapacity) {

            ByteArray old = data;

            try {
                //data = PDSFactory.getAllocator().create(Util.doubleUntilAtLeast(oldCapacity, newLength));
                data = new ByteArray(Util.doubleUntilAtLeast(oldCapacity, newLength)); // @ALLOCATION

                if (dataOffset > 0)
                    ByteArray.arraycopy(old, 0, data, 0, dataOffset);

                if (tailSize > 0)
                    ByteArray.arraycopy(old, dataOffset, data, dataOffset + msgLength, tailSize);
            } finally {
                //PDSFactory.getAllocator().free(old);
            }
        } else {
            if (tailSize > 0)
                ByteArray.arraycopy (data, dataOffset, data, dataOffset + msgLength, tailSize);
        }

        System.arraycopy (src, srcOffset, data.getArray(), data.getOffset(dataOffset), msgLength);
        
        length = newLength;        
        
        if (timestamp < startTime)
            startTime = timestamp;
        
        if (timestamp > endTime)
            endTime = timestamp;

        return setDirty();
    }
    
    public boolean                     shorten (int splitOffset, long timestamp) {
        length = splitOffset;

        assert startTime <= timestamp;

        if (splitOffset == 0) {
            startTime = Long.MAX_VALUE;
            endTime = Long.MIN_VALUE;
        } else if (timestamp < endTime) {
            endTime = timestamp;
        }

        return setDirty();
    }

    public boolean                     cut (int startOffset, int endOffset) {
        int free = endOffset - startOffset;

        assert free > 0;

        if (length > endOffset)
            ByteArray.arraycopy(data, endOffset, data, startOffset, length - endOffset);

        length -= free;

        return setDirty ();
    }

    public void                     clear () {
        entity = -1;
        length = -1;

        //PDSFactory.getAllocator().free(data);
        data = null;
        ts = null;
        isDirty = false;    // extremely important for reuse!
    }

    public boolean                     setData (ByteArray src, int offset, int length, long startTime, long endTime) {
        this.length = length;
        
        alloc ();

        ByteArray.arraycopy(src, offset, data, 0, length);

        this.startTime = startTime;
        this.endTime = endTime;
        
        return setDirty ();
    }

    public void                     setClean () {
        isDirty = false;
    }

    @Override
    public long                     getStartTime () {
        return (startTime);
    }

    @Override
    public long                     getEndTime () {
        return (endTime);
    }

    public ByteArray                  getData () {
        return (data);
    }
    
    public void                     store (OutputStream os) throws IOException {
//        if (DEBUG_VERIFY_BLOCK_ON_WRITE) {
//            TSFVerifier tsfv = new TSFVerifier ();
//            MemoryDataInput mdi = new MemoryDataInput ();
//            configure (mdi, 0);
//            tsfv.verifyBlock (mdi, -1, entity, startTime, endTime);
//        }
        
        os.write (data.getArray(), data.getOffset(), length);
        isDirty = false;
    }

    public void                     setStartTime(long startTime) {
        //assert endTime >= startTime;
        assert startTime != Long.MIN_VALUE;
        this.startTime = startTime;
    }

    public void                     setEndTime(long endTime) {
//        assert endTime != Long.MIN_VALUE && endTime != Long.MAX_VALUE;
//        assert endTime >= startTime;

        this.endTime = endTime;
    }
}