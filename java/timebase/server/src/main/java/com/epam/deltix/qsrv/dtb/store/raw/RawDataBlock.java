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
package com.epam.deltix.qsrv.dtb.store.raw;

import java.io.IOException;
import java.io.OutputStream;

import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class RawDataBlock implements Comparable<RawDataBlock> {
    private static final int                MIN_BUFFER_SIZE = 500;

    private final int               idxInFile;
    private final int               entity;
    private final int               offset;
    private final int               lengthOnDisk;
    private       long              firstTimestamp;
    private       long              lastTimestamp;
    private       byte []           bytes;
    private       int               length;
    private       int               compressedLength;

    public RawDataBlock (
        int                         idxInFile,
        int                         entity, 
        int                         offset,
        int                         dataLength, 
        int                         lengthOnDisk,
        long                        firstTimestamp, 
        long                        lastTimestamp
    )
    {
        this.idxInFile = idxInFile;
        this.entity = entity;
        this.offset = offset;
        this.length = dataLength;
        this.lengthOnDisk = lengthOnDisk;
        this.firstTimestamp = firstTimestamp;
        this.lastTimestamp = lastTimestamp;
    }
        
    public int                  getIdxInFile () {
        return idxInFile;
    }

    public int                  getEntity () {
        return entity;
    }

    public int                  getOffset () {
        return offset;
    }

    public int                  getDataLength () {
        return length;
    }

    public int                  getLengthOnDisk () {
        return lengthOnDisk;
    }

    public long                 getFirstTimestamp () {
        return firstTimestamp;
    }

    public long                 getLastTimestamp () {
        return lastTimestamp;
    }

    public byte []                  getData () {
        return (bytes);
    }

    public void                     setData (byte [] src, int offset, int length) {
        this.length = length;
        alloc ();
        System.arraycopy (src, offset, bytes, 0, length);
    }

    private void                    alloc () {
        if (bytes == null || bytes.length < length)
            bytes = new byte [Util.doubleUntilAtLeast(MIN_BUFFER_SIZE, length)];
    }

    public void                     store (OutputStream os) throws IOException {
        os.write (bytes, 0, length);
    }

    @Override
    public int compareTo(RawDataBlock o) {
        if (entity > o.entity) {
            return 1;
        } else if (entity < o.entity) {
            return -1;
        } else {
            return 0;
        }
    }

    public void setFirstTimestamp(long firstTimestamp) {
        this.firstTimestamp = firstTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}