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
