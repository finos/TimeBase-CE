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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;

/**
 *  A stream that does not use the multiple tick file paradigm.
 */
public interface SingleChannelStream {

    /*
    *  <p>Creates a message source for reading data from this stream, according to the
    *  specified options and message filter. The messages
    *  are returned from the source strictly ordered by time. Within the same
    *  exact time stamp, the order of messages is undefined and may vary from
    *  call to call, i.e. it is non-deterministic.</p>
    *
    *  @param time     The start timestamp.
    *  @param options  Selection options.
    *  @param filter   Specified message filter, filter changes cannot be accepted immediately.
    *  @return         A message source used to read messages.
    */

    public MessageSource<InstrumentMessage> createSource (
        long                                        time,
        SelectionOptions                            options,
        QuickMessageFilter                          filter
    );

    /*
     *  <p>Creates a message source for reading data from this stream, according to the
     *  specified options. The messages
     *  are returned from the source strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <p>Note that the arguments of this method only determine the initial
     *  configuration of the cursor. The {@link InstrumentMessageSource} interface provides
     *  methods for dynamically re-configuring the subscription, or jumping to
     *  a different timestamp.</p>
     *
     *  @param time     The start timestamp.
     *  @param options  Selection options.
     *  @param types    Specified message types to be subscribed. If null, then all types will be subscribed.
     *  @param entities Specified entities to be subscribed. If null, then all entities will be subscribed.
     *  @return         A message source used to read messages.
     */

    public InstrumentMessageSource              createSource (
            long                                        time,
            SelectionOptions                            options,
            IdentityKey[]                        identities,
            String[]                                    types
    );

    /*
    *  <p>Creates a message source for reading data from this stream, using "ALL" subscription.
    *  The messages are returned from the source strictly ordered by time. Within the same
    *  exact time stamp, the order of messages is undefined and may vary from
    *  call to call, i.e. it is non-deterministic.</p>
    *
    *  <p>Note that the arguments of this method only determine the initial
    *  configuration of the cursor. The {@link InstrumentMessageSource} interface provides
    *  methods for dynamically re-configuring the subscription, or jumping to
    *  a different timestamp.</p>
    *
    *  @param time     The start timestamp.
    *  @param options  Selection options.
    */
    public InstrumentMessageSource              createSource (
            long                                        time,
            SelectionOptions                            options
    );

    /**
     *  Return an range of times for which the specified entities
     *  have data in the database.
     *
     *  @param entities     A list of entities. If empty, return for each entity in the stream.
     *  @return             Array of TimeRange objects for the specified entities.
     */
    public TimeInterval[]                      listTimeRange(IdentityKey ... entities);

}
