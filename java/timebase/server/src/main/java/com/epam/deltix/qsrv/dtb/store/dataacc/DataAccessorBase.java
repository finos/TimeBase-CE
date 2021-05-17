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

import com.epam.deltix.qsrv.dtb.store.pub.*;
import net.jcip.annotations.GuardedBy;

/**
 *  Lock ordering: this, then current time slice.
 */
public abstract class DataAccessorBase implements DataAccessor, DAPrivate {  
    protected TimeSliceStore                      store;

    @GuardedBy("this")
    protected TimeSlice                           currentTimeSlice;    
    
    public DataAccessorBase () {
    }

    //
    //  DataAccessor IMPLEMENTATION
    //
    @Override
    public synchronized void                 associate (TSRoot store) {
        this.store = (TimeSliceStore) store;
    }

    public synchronized  void   associate (TimeSlice slice) {
        this.currentTimeSlice = slice;
    }
    //
    //  INTERNALS
    //  
    protected void              assertOpen () {
        if (currentTimeSlice == null)
            throw new IllegalStateException ("not open");
    }

    @Override
    public synchronized void    close () {
        currentTimeSlice = null;
    }

    public abstract AccessorBlockLink       getBlockLink (int entity, long ffToTimestamp);

    public abstract AccessorBlockLink       getBlockLink (int entity, DataBlock block);
    
}
