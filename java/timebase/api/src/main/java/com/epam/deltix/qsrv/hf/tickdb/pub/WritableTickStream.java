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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;

/**
 *  Writeable stream.
 * 
 *  @see TickStream
 */
public interface WritableTickStream extends TickStream {
    public WritableTickDB                   getDB ();

    /**
     *  Creates a channel for loading data. The loader must be closed
     *  when the loading process is finished.
     *
     *  @return A consumer of messages to be loaded into the stream.
     */
    public TickLoader                       createLoader ();
    
    /**
     *  Creates a channel for loading data. The loader must be closed
     *  when the loading process is finished.
     *
     *  @return A consumer of messages to be loaded into the stream.
     */
    public TickLoader                       createLoader (LoadingOptions options);

    /**
     * Truncates stream data for the given entities from given time
     * @param time  Timestamp. If time less than stream start time, then all stream data will be deleted.
     * @param ids   A list of entities. If unknown, all stream entities will be used.
     */
    public void                             truncate(long time, IdentityKey... ids);

    /**
     * Deletes stream data for the given entities using specified time range
     * @param from  start timestamp (inclusive). Time is measured in milliseconds or nanoseconds that passed since January 1, 1970 UTC.
     * @param to    end timestamp (inclusive). Time is measured in milliseconds or nanoseconds that passed since January 1, 1970 UTC.
     *              If time more than stream end time, then all stream data will be deleted for the given stream.
     * @param ids   A list of entities. If unknown, all stream entities will be used.
     */
    public void                             delete(TimeStamp from, TimeStamp to, IdentityKey... ids);

    /**
     * Clear stream data for the given entities.
     * @param ids  A list of entities. If unknown, all stream entities will be used.
     */
    public void                             clear(IdentityKey... ids);
}