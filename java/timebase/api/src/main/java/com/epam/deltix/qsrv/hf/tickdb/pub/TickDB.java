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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.io.AbstractDataStore;

/**
 *  <p>The top-level interface to the read-only methods of the Deltix Tick
 *  Database engine. Instances of this interface are created by static methods 
 *  of {@link TickDBFactory}.</p>
 * 
 *  <p>At the logical level, a database consists of a number of so-called 
 *  streams. Each stream is a time series of messages for a number of 
 *  financial instruments ("entities"). Multiple streams
 *  can be used to represent data of different frequencies, or completely
 *  different factors. For instance, separate streams can represent
 *  1-minute price bars and tics for the same set of entities. Or,
 *  you can have price bars and volatility bars in separate streams.</p>
 */
public interface TickDB extends AbstractDataStore {
    /**
     *  Looks up an existing stream by key.
     * 
     *  @param key      Identifies the stream.
     *  @return         A stream object, or <code>null</code> if the key was not found.
     *  @throws         java.security.AccessControlException when user is not authorized to READ given stream
     */
    TickStream               getStream (
        String                                  key
    );

    /**
     *  Enumerates existing streams.
     * 
     *  @return         An array of existing stream objects.
     */
    TickStream []            listStreams ();

    /**
     *  <p>Opens an initially empty cursor for reading data from multiple streams,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <p>The cursor is returned initially empty and must be reset.
     *  The {@link TickCursor} interface provides
     *  methods for dynamically re-configuring the subscription, or jumping to
     *  a different timestamp.</p>
     *
     *  @param options  Selection options.
     *  @param streams  Streams from which data will be selected.
     *  @return         A cursor used to read messages.
     */
    TickCursor                       createCursor (
        SelectionOptions                        options,
        TickStream ...                          streams
    );   

     /**
     *  <p>Opens a cursor for reading data from multiple streams,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <p>Note that the arguments of this method only determine the initial
     *  configuration of the cursor. The {@link TickCursor} interface provides
     *  methods for dynamically re-configuring the subscription, or jumping to
     *  a different timestamp.</p>
     *
     *  @param time     The start timestamp.
     *  @param options  Selection options.
     *  @param symbols  Specified symbols to be subscribed. If null, then all entities will be subscribed.
     *  @param types    Specified message types to be subscribed. If null, then all types will be subscribed.
     *  @param streams  Streams from which data will be selected.
     *  @return         A cursor used to read messages.
     */

    TickCursor                       select (
        long                                    time,
        SelectionOptions                        options,
        String[]                                types,
        CharSequence[]                          symbols,
        TickStream ...                          streams
    );

    TickCursor                       select (
            long                                    time,
            SelectionOptions                        options,
            String[]                                types,
            IdentityKey[]                           ids,
            TickStream ...                          streams
    );

    TickCursor                       select (
        long                                    time,
        SelectionOptions                        options,
        String[]                                types,
        TickStream ...                          streams
    );

    TickCursor                       select (
        long                                    time,
        SelectionOptions                        options,
        TickStream ...                          streams
    );
}