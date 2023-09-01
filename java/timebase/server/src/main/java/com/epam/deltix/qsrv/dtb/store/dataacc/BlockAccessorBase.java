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
package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *  Lock ordering: this, then current time slice.
 */
abstract class BlockAccessorBase extends DataAccessorBase { 
    final MemoryDataInput       mdi = new MemoryDataInput ();
    final byte[]                EMPTY = new byte[0];

    /**
     *  Guarded by self. Concurrent threads may use this hashmap to update links.
     */
    private final IntegerToObjectHashMap <AccessorBlockLink> links = 
        new IntegerToObjectHashMap <> ();   

    /*
     *  Lock ordering: DataBlock -> links
     */
    @Override
    public void             asyncDataInserted (
        DataBlock               db, 
        int                     dataOffset, 
        int                     msgLength, 
        long                    timestamp
    )
    {
        AccessorBlockLink   link;

        synchronized (links) {
            link = links.get (db.getEntity (), null);
            
            if (link != null)
                assert link.isBlock (db);
        }

         if (link != null)
             link.asyncDataInserted (dataOffset, msgLength, timestamp);
    }

    @Override
    public void             asyncDataDropped (
            DataBlock               db,
            int                     dataOffset,
            int                     length,
            long                    nextTime
    )
    {
        // lock ordering: block -> links

        AccessorBlockLink link;

        synchronized (links) {
            link = links.get(db.getEntity(), null);

            if (link != null)
                assert link.isBlock(db);
        }

        if (link != null)
            link.asyncDataDropped (dataOffset, length, nextTime);
    }

    //
    //  PACKAGE INTERFACE
    //
    final void                  clearLinks () {
        synchronized (links) {
            links.clear ();
        }
    }
    
    final boolean               hasLinks () {
        synchronized (links) {
            return (!links.isEmpty ());
        }
    }
    
    final void                  unmap (AccessorBlockLink link) {
        synchronized (links) {
            AccessorBlockLink   check = links.remove (link.getEntity (), null);
        
            if (check != link)
                throw new IllegalArgumentException (link + " is not registered in " + this);
        }
    } 
    
    final void                  map (AccessorBlockLink link) {
        synchronized (links) {
            boolean             ok = links.put (link.getEntity (), link);
        
            if (!ok)
                throw new IllegalArgumentException (link + " was duplicate in " + this);
        }
    }
    
    public final AccessorBlockLink     getBlockLink (int entity, long ffToTimestamp) {
        AccessorBlockLink   link;

        DataBlock           block = currentTimeSlice.getBlock (entity, true);

        synchronized (links) {
            link = links.get (entity, null);

            if (link == null) {
                link = new AccessorBlockLink (this, currentTimeSlice, block);
                links.put (entity, link);
            }
        }

        link.forward(ffToTimestamp);

        return (link);
    }

    public final AccessorBlockLink     getBlockLink (int entity, DataBlock block) {

        assert entity == block.getEntity();

        synchronized (links) {

            AccessorBlockLink   link = links.get (entity, null);

            if (link == null) {
                link = new AccessorBlockLink (this, currentTimeSlice, block);
                links.put (entity, link);
            } else {
                assert link.isBlock (block);
            }

            return (link);
        }
    }

    final AccessorBlockLink     find (int entity) {
        synchronized (links) {
            return links.get (entity, null);
        }
    }

    @Override
    public void                 checkedOut(TimeSlice slice) {
        clearLinks();
    }

    @Override
    public void                 close () {
        clearLinks();

        synchronized (this) {
            if (currentTimeSlice != null)
                currentTimeSlice.getStore().checkInTimeSlice(this, currentTimeSlice);
            currentTimeSlice = null;
        }

        clearBuffers();

        super.close();
    }

    void        clearBuffers() {
        mdi.setBytes(EMPTY);
    }
}