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
package com.epam.deltix.qsrv.dtb.test;

import com.epam.deltix.qsrv.dtb.store.pub.*;
import com.epam.deltix.util.memory.*;
import static org.junit.Assert.*;

/**
 *
 */
public class MessageGenerator 
    implements TSMessageProducer, TSMessageConsumer
{
    public static final int         NO_MORE = Integer.MAX_VALUE;
    
    private final TestConfig        config;
    private int                     currentSeqNo;
    private int                     entity;
    private long                    timestamp;
    private int                     type;
    
    public MessageGenerator (TestConfig config) {
        this.config = config;
    }

    public int              getSeqNo () {
        return currentSeqNo;
    }

    public int              getEntity () {
        return entity;
    }

    public long             getTimestamp () {
        return timestamp;
    }
        
    public long             seqNoToTimestamp (int seqNo) {
        final int               batchNo = seqNo / config.batchSize;
        
        return (config.baseTime + config.batchSize * batchNo);
    }
    
    /**
     *  Return the seqNo of a message that should be selected by this timestamp. 
     */
    public int              timestampToSeqNo (long ts, EntityFilter filter) {
        if (ts >= config.baseTime + config.numMessages)
            return (NO_MORE);
        
        long                    offset = ts - config.baseTime;
        
        if (offset < 0)
            offset = 0;
        
        return (getNextSeqNo ((int) (offset - offset % config.batchSize), filter));                        
    }
    
    public int              getNextSeqNo (int seqNo, EntityFilter filter) {
        if (filter == null)
            return (seqNo);
         
        while (seqNo < config.numMessages) {
            if (filter.accept (seqNoToEntity (seqNo)))
                return (seqNo);
            
            seqNo++;
        }
        
        return (NO_MORE);
    }
    
    public int              getPrevSeqNo (int seqNo, EntityFilter filter) {
        if (filter == null)
            return (seqNo);
         
        while (seqNo > 0) {
            if (filter.accept (seqNoToEntity (seqNo)))
                return (seqNo);
            
            seqNo--;
        }
        
        return (NO_MORE);
    }
    
    public int              seqNoToEntity (int seqNo) {
        return (seqNo % config.numEntities);
    }
    
    public int              seqNoToType (int seqNo) {
        final int               entSeqNo = seqNo / config.numEntities;
        
        return (entSeqNo % config.numTypes);
    }
    
    public void             setSeqNo (int seqNo) {
        currentSeqNo = seqNo;        
        entity = seqNoToEntity (seqNo);        
        type = seqNoToType (seqNo);                
        timestamp = seqNoToTimestamp (seqNo);
    }
    
    public void             writeTo (DataWriter writer) {
        writer.insertMessage (entity, timestamp, type, this);
    }
    
    @Override
    public void             writeBody (MemoryDataOutput out) {
        out.writeInt (currentSeqNo);
        out.writeInt (entity);
        out.writeLong (timestamp);
        out.writeInt (-1);
    }

    @Override
    public void             process (
        int                     entity,
        long                    timestampNanos,
        int                     type, 
        int                     bodyLength, 
        MemoryDataInput         mdi
    )
    {
        if (currentSeqNo == NO_MORE)
            throw new AssertionError ("Did not expect to read a message");
        
        assertEquals (20, bodyLength);
                        
        int                     readSeqNo = mdi.readInt ();
        
        assertEquals (this.currentSeqNo, readSeqNo);        
        assertEquals (this.entity, entity);
        assertEquals (this.timestamp, timestampNanos);
        assertEquals (this.type, type);
    }

    @Override
    public boolean processRealTime(long timestampNanos) {
        // do nothing
        return false;
    }

    @Override
    public boolean isRealTime() {
        return false;
    }

    @Override
    public boolean realTimeAvailable() {
        return false;
    }
}