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

import javax.annotation.Nullable;

/**
 *
 */
public interface TimeSliceStore {
    /**
     *  Checks out a TimeSlice that can be used to store data at the specified 
     *  timestamp.
     * 
     *  @param accessor     The accessor to which the TimeSlice is checked out.
     *  @param timestamp    The timestamp at which data will be inserted.
     * 
     *  @return  A checked out but not yet locked TimeSlice.
     * 
     *  @throws InterruptedException    
     *                      If thread is interrupted while obtaining locks.
     */
    public TimeSlice    checkOutTimeSliceForInsert (
        DAPrivate           accessor, 
        long                timestamp
    )
        throws InterruptedException;
    
    /**
     *  Checks out a TimeSlice that can be used to read data at or after 
     *  the specified timestamp for the specified set of entities.
     * 
     *  @param accessor     The accessor to which the TimeSlice is checked out.
     *  @param timestamp
     *  @param filter       The filter.
     * 
     * @return  A checked out TimeSlice.
     * 
     *  @throws InterruptedException    
     *                      If thread is interrupted while obtaining locks.
     */
    public TimeSlice    checkOutTimeSliceForRead (
        DAPrivate           accessor, 
        long                timestamp, 
        EntityFilter        filter
    )
        throws InterruptedException;

    /**
     *  Checks out a TimeSlice that can be used to read data at or after 
     *  the specified timestamp for the specified set of entities.
     * 
     *  @param accessor     The accessor to which the TimeSlice is checked out.
     *  @param tsref        A TimeSlice Reference
     * 
     * @return  A checked out TimeSlice.
     * 
     *  @throws InterruptedException    
     *                      If thread is interrupted while obtaining locks.
     */
    public TimeSlice    checkOutTimeSlice (
        DAPrivate           accessor, 
        TSRef               tsref
    )
        throws InterruptedException;

    /**
     * Checkout a TimeSlice to be used by additional accessor.
     * @return A checked out TimeSlice. Usually this is the same object.
     */
    TimeSlice           checkOutTimeSlice (
            DAPrivate                       accessor,
            TimeSlice                       slice
    );

    /**
     * Checks in a used TimeSlice
     *
     * @param accessor     The accessor to which the TimeSlice is checked out.
     * @param slice        A TimeSlice
     *
     * @throws InterruptedException
     *                      If thread is interrupted while obtaining locks.
     */
    public void         checkInTimeSlice (
            DAPrivate                       accessor,
            TimeSlice                       slice
    );


    /**
     *  Check in the specified time slice (if keepPrevCheckout is false) and return, if any, the next one in the
     *  time series, checked out to the specified accessor.
     * 
     *  @param accessor     The accessor.
     *  @param ts           A time slice checked out to this accessor. By the
     *                      time this method returns, this time slice will be
     *                      checked in and must not be referenced.
     *  @param filter       The filter.
     *  @param forward      Whether to go forward (otherwise, backward).
     *  @param keepPrevCheckout     Whether to leave previous slice checked out.
     *  @return             A new time slice, checked out to the specified
     *                      accessor, or null if it was last.
     * 
     *  @throws InterruptedException    
     *                      If thread is interrupted while obtaining locks.
     */
    @Nullable
    public TimeSlice    getNextTimeSliceToRead (
        DAPrivate           accessor, 
        TimeSlice           ts,
        EntityFilter        filter,
        boolean             forward,
        boolean             keepPrevCheckout
    )
        throws InterruptedException;


    public void         addSliceListener(SliceListener listener);

    public void         removeSliceListener(SliceListener listener);

}
