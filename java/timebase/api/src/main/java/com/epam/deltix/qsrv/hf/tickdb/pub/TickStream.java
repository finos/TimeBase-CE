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

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  <p>The stream is a time series of messages for a number of
 *  instruments ("entities"). In the simplest case,
 *  a database will have a single stream of data. Multiple streams
 *  can be used to represent data of different frequencies, or completely
 *  different factors. For instance, separate streams can represent
 *  1-minute price bars and ticks for the same set of entities. Or,
 *  you can have price bars and volatility bars in separate streams.</p>
 *
 *  @see TickDB#getStream(String)
 *  @see TickDB#listStreams
 */
public interface TickStream extends DXChannel<InstrumentMessage> {
    /**
     *  Returns the parent database object.
     *
     *  @return Parent database object.
     */
    TickDB                           getDB();

    /**
     *  Returns the key, which uniquely identifies the stream
     *  within its database.
     */
    String                           getKey();

    /**
     *  Returns a user-readable short name.
     */
    String                           getName();

    /**
     *  Returns a user-readable multi-line description.
     */
    String                           getDescription();

    /**
     *  Returns a number which is guaranteed to be incremented every time
     *  stream metadata is changed. This number can be
     *  used as a basis for caching information about streams, as returned
     *  from methods {@link #isPolymorphic}, {@link #isFixedType},
     *  {@link #getFixedType}, {@link #getPolymorphicDescriptors} and
     *  {@link #getAllDescriptors}.
     */
    long                             getTypeVersion();

    /**
     *  Returns stream data format version.
     *  Currently supported formats is 4 and 5.
     */
    int                              getFormatVersion();

    /**
     *  Returns whether the stream is configured as polymorphic.
     */
    boolean                          isPolymorphic();
    
    /**
     *  Returns whether the stream is configured as fixed-type.
     */
    boolean                          isFixedType();
    
    /**
     *  When the stream is configured as fixed-type, returns the content class
     *  descriptor. Otherwise, returns null.
     * 
     *  @return Content class descriptor.
     */
    RecordClassDescriptor            getFixedType();
    
    /**
     *  When the stream is configured as polymorphic, returns the content class
     *  descriptors. Otherwise, returns null.
     * 
     *  @return Content class descriptors.
     */
    RecordClassDescriptor []         getPolymorphicDescriptors();
    
    /**
     *  Return all class descriptors comprising this stream's metadata.
     *
     *  @return All relevant class descriptors.
     */
    ClassDescriptor []               getAllDescriptors();

    /**
     *  <p>Opens a cursor for reading data from this stream, according to the
     *  specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <p>Note that the arguments of this method only determine the initial
     *  configuration of the cursor. The {@link TickCursor} interface provides
     *  methods for dynamically re-configuring the subscription, or jumping to
     *  a different timestamp.</p>
     *
     *
     *  @param time     The start timestamp.
     *  @param options  Selection options.
     *  @param types    Specified message types to be subscribed. If null, then all types will be subscribed.
     *  @param entities Specified entities to be subscribed. If null, then all entities will be subscribed.
     *  @return         A cursor used to read messages.
     */
    TickCursor                       select(
            long                        time,
            SelectionOptions            options,
            String[]                    types,
            IdentityKey[]               entities
    );

    TickCursor                       select(
            long                        time,
            SelectionOptions            options,
            String[]                    types,
            CharSequence[]              symbols
    );

    TickCursor                       select(
            long                        time,
            SelectionOptions            options,
            String[]                    types
    );

    TickCursor                       select(
            long                        time,
            SelectionOptions            options
    );

    /**
     * <p>Same as {@link #select} but will restrict permitted stream type of cursor to type of this stream.
     * Attempt to add new stream of different type to produced cursor <i>may</i> result in error.</p>
     *
     * <p>Do not use this method if you going to add new streams of arbitrary type to the cursor.</p>
     *
     * <p>Cursors created in this way <i>may</i> give better performance is some specific cases.</p>
     */
/*    default TickCursor                       selectUsingStreamCursor(
            long                                time,
            SelectionOptions                    options,
            String[]                            types,
            IdentityKey[]                entities
    ) {
        return select(time, options, types, entities);
    }*/

    /**
     *  <p>Creates a cursor for reading data from this stream, according to the
     *  specified options, but initially with a fully restricted filter.
     *  The user must call {@link TickCursor#reset} at least once, in order to
     *  begin retrieving data. This method is equivalent to (but is
     *  slightly more optimal than) calling
     *  {@code createCursor(options)}
     *  </p>
     *
     *  @return         A cursor used to read messages. Never null.
     */
    TickCursor                       createCursor(
            SelectionOptions options
    );

    /**
     *  Return a non-nullable list of entities, for which this stream has any data.
     */
    IdentityKey []                  listEntities();
    
    /**
     *  Return an inclusive range of times for which the specified entities
     *  have data in the database.
     * 
     *  @param entities     A list of entities. If empty, return for all.
     *  @return             An array consisting of two long timestamps (from and to), 
     *                      or <code>null</code> if no data was found.
     */
    long []                          getTimeRange(IdentityKey... entities);

    /**
     *  Return an range of times for which the specified entities
     *  have data in the database.
     *
     *  @param entities     A list of entities. If empty, return for each entity in the stream.
     *  @return             Array of TimeInterval objects for the specified entities.
     */
    TimeInterval[]                   listTimeRange(IdentityKey... entities);

    /**
     * Returns all created "spaces" for the stream.
     * Default space returns as "" (empty string).
     * <p>
     * If backing stream does not support spaces {@code null} will be returned.
     */
    default String[]                        listSpaces() {
        return null;
    }

    /**
     * Returns all symbols listed in given "space".
     * <p>
     * If backing stream does not support spaces {@code null} will be returned.
     */
    default IdentityKey[]        listEntities(String space) {
        return null;
    }

    /**
     * @return An array consisting of two long timestamps (from and to) or <code>null</code> if no data was found.
     */
    default long[]                      getTimeRange(String space) {
        return null;
    }
}
