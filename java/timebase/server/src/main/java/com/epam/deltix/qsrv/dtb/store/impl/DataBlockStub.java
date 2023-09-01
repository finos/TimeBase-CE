/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.store.dataacc.*;
import java.io.*;

/**
 *  Information about an unread data block.
 */
final class DataBlockStub implements DataBlockInfo {
    static final int                SIZE_ON_DISK = 24;
        
    private final int               entity;
    private final int               offsetInFile;
    private final int               dataLength;
    private final int               lengthOnDisk;
    private final long              startTime;
    private final long              endTime;

    public DataBlockStub (
        int                 entity,
        int                 offsetInFile, 
        int                 lengthOnDisk, 
        int                 dataLength, 
        long                startTime,
        long                endTime
    )
    {
        this.entity = entity;
        this.offsetInFile = offsetInFile;
        this.dataLength = dataLength;
        this.lengthOnDisk = lengthOnDisk;
        this.startTime = startTime;
        this.endTime = endTime;
    }        

    @Override
    public int                  getEntity () {
        return entity;
    }
        
    @Override
    public int                  getDataLength () {
        return dataLength;
    }

    @Override
    public int                  getAllocatedLength () {
        return SIZE_ON_DISK;
    }

    public int                  getLengthOnDisk () {
        return lengthOnDisk;
    }    

    int                         getOffsetInFile () {
        return offsetInFile;
    }    

    @Override
    public long                 getStartTime () {
        return startTime;
    }

    @Override
    public long                 getEndTime () {
        return endTime;
    }

    @Override
    public void clear() {
        // do nothing
    }
}