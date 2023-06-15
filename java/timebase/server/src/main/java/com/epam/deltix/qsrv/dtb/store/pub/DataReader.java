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
package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;

/**
 *  A reusable object for reading TimeBase messages.
 */
public interface DataReader extends DataAccessor, IntermittentlyAvailableResource {
    /**
     *  
     *  Resets this accessor to focus on the first message in the specified 
     *  TSF at or after the specified timestamp.
     * 
     *  @param timeSlice            The time slice to focus on.
     *  @param timestamp            Fast forward to specified timestamp.
     *                              Pass Long.MIN_VALUE (or any timestamp prior 
     *                              to TSF) to process all messages.
     *  @param movePastTSFEnd       If true, the reader will move to next 
     *                              time slice, as normal. If false, the reader
     *                              will return false from {@link #readNext(deltix.qsrv.dtb.store.pub.TSMessageConsumer) }
     *                              at the end of the current time slice.
     *  @param filter               The entity filter. Pass null to read all data.
     */
    public void                 open (
        TSRef                       timeSlice,
        long                        timestamp,
        boolean                     movePastTSFEnd,
        EntityFilter                filter
    ); 
    
    /**
     *  Resets this accessor to focus on
     *  the first message at or after the specified timestamp.
     * 
     *  @param timestamp            The timestamp (all times are in nanoseconds) to seek.
     *  @param forward              Whether to read forward in time.
     *  @param filter               The entity filter. Pass null to read all data.
     */
    public void                 open (
        long                        timestamp,
        boolean                     forward,
        EntityFilter                filter
    );

    /**
     * Reset current filter and change subscription.
     * @param filter EntityFilter to apply for the current slice
     */
    public void                 setFilter(EntityFilter filter);
    
    /**
     *  Sets limit timestamp to read.
     *
     *  @param timestamp            The timestamp (all times are in nanoseconds) to limit reader.
     */
    public void                 setLimitTimestamp (long timestamp);

    /**
     *  Returns start time of the current time slice.
     */
    public long                 getStartTimestamp();

    /**
     *  Returns end time of the current time slice.
     */
    public long                 getEndTimestamp();
    
    /**
     *  Read a message at the current location.
     * 
     *  @param processor    The MemoryDataInput instance to configure
     * 
     *  @return             false if the current message was the last one.
     */
    public boolean              readNext (TSMessageConsumer processor);

    /*
        Reopens reader to the given timestamp in nanoseconds.
     */
    public void                 reopen(long timestamp);

    /**
     *  Closed all allocated and opened slices. Data reading can be resumed again calling reopen() method.
     */
    public void                 park();
}