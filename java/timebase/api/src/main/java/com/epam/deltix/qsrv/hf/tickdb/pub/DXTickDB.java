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
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.MetaData;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import java.io.*;

import com.epam.deltix.util.lang.GrowthPolicy;

/**
 *  Methods specific to the Deltix implementation of WritableTickDB.
 */
public interface DXTickDB extends WritableTickDB {
    /**
     *  Forces the building of all memory indexes. This is useful in 
     *  performance testing or on server startup. 
     */
    void                             warmUp();
    
    /**
     *  Removes all memory indexes. The indexes will be rebuilt on demand.
     */
    void                             coolDown ();
    
    /**
     *  Trims all data files to minimum size.
     */
    void                             trimToSize ();

    /**
     *  Returns the approximate size of the database's footprint.
     *
     *  @return     Size in bytes.
     */
    long                             getSizeOnDisk ();

    /**
     * Return stream by given key
     * @param key      Identifies the stream key.
     * @return DXTickStream instance, or null if stream is not exists
     */
    public DXTickStream                     getStream (
        String                                  key
    );

    /**
     * Return list of streams
     * @return collection of DXTickStream instances
     */
    public DXTickStream []                  listStreams ();

    public DXChannel[]                      listChannels ();
    
    public File []                          getDbDirs ();

    /**
     *  Creates a new anonymous stream.
     *
     *  @param options
     *              Options for creating the stream. The stream is
     *              automatically created with the {@link StreamScope#RUNTIME}
     *              scope.
     */
    @Deprecated
    public DXTickStream                     createAnonymousStream (
        StreamOptions                           options
    );

    /**
     * Returns system time of the timebase server.     
     */
    public long                             getServerTime();

    /**
     *  Creates a new stream within the database.
     * 
     *  @param key          A required key later used to identify the stream.
     *  @param options      Options for creating the stream.
     * 
     *  @exception IllegalArgumentException When the key is duplicate.
     */
    public DXTickStream                     createStream (
        String                                  key, 
        StreamOptions                           options
    );

    /**
     *  Creates a new stream within the database. The newly created stream must
     *  be configured with the required metadata
     *  via calling either {@link DXTickStream#setFixedType} or
     *  {@link DXTickStream#setPolymorphic}.
     *
     *  @param key          A required key later used to identify the stream.
     *  @param name         An optional user-readable name.
     *  @param description  An optional multi-line description.
     *  @param distributionFactor The number of files into which to distribute the
     *                      data. Supply 0 to keep a separate file for each
     *                      instrument.
     *
     *  @exception IllegalArgumentException When the key is duplicate.
     */
    public DXTickStream                     createStream (
        String                                  key,
        String                                  name,
        String                                  description,
        int                                     distributionFactor
    );

    /**
     *  Sets the file growth policy
     *
     * @param policy Growth Policy value
     */
    public void                             setGrowthPolicy (GrowthPolicy policy);

    /**
     *  Gets current metadata version.
     *
     *  @return current metadata version number.
     */
    public long                             getMetaDataVersion ();

    /**
     *  Gets stream metadata contains all class descriptors.
     *
     *  @return metadata object.
     */
    MetaData<RecordClassDescriptor>         getMetaData ();

    /**
     *  Returns schema for the given query.
     *
     *  @param qql      Query text.
     *  @param options  Selection options.
     *  @param params   Specified message types to be subscribed. If null, then all types will be subscribed.*
     *
     *  @return         Schema contains classes definitions.
     *
     *  @throws CompilationException when query has errors
     */
    ClassSet<ClassDescriptor>        describeQuery(String qql, SelectionOptions options, Parameter ...params)
        throws CompilationException;

    /**
     *  <p>Execute Query and creates a message source for reading data from it,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <code>select * from bars</code>
     *
     *  @param qql      Query text.
     *  @param options  Selection options.
     *  @param ids      Specified entities to be subscribed. If null, then all entities will be subscribed.
     *  @param params   Specified message types to be subscribed. If null, then all types will be subscribed.*
     *  @return         A cursor used to read messages.
     */
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        CharSequence []                         ids,
        Parameter ...                           params
    )
        throws CompilationException;

    /**
     *  <p>Execute Query and creates a message source for reading data from it,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <code>select * from bars</code>
     *
     *  @param qql      Query text.
     *  @param params   Specified message types to be subscribed. If null, then all types will be subscribed.*
     *  @return         A cursor used to read messages.
     */
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        Parameter ...                           params
    )
        throws CompilationException;

    /**
     *  <p>Execute Query and creates a message source for reading data from it,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <code>select * from bars</code>
     *
     *  @param qql      Query text.
     *  @param params   Specified message types to be subscribed. If null, then all types will be subscribed.*
     *  @return         A cursor used to read messages.
     *
     *  @throws CompilationException when query has errors
     */
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        Parameter ...                           params
    )
        throws CompilationException;

    /**
     *  <p>Execute Query and creates a message source for reading data from it,
     *  according to the specified options. The messages
     *  are returned from the cursor strictly ordered by time. Within the same
     *  exact time stamp, the order of messages is undefined and may vary from
     *  call to call, i.e. it is non-deterministic.</p>
     *
     *  <code>select * from bars</code>
     *
     *  @param qql      Query text.
     *  @param params   Specified message types to be subscribed. If null, then all types will be subscribed.*
     *  @return         A cursor used to read messages.
     *
     *  @throws CompilationException when query has errors.
     */
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        long                                    time,
        Parameter ...                           params
    )
        throws CompilationException;

    /**
     * <p>Execute Query and creates a message source for reading data from it,
     * according to the specified options. The messages
     * are returned from the cursor strictly ordered by time. Within the same
     * exact time stamp, the order of messages is undefined and may vary from
     * call to call, i.e. it is non-deterministic.</p>
     *
     * <code>select * from bars</code>
     *
     * @param qql            Query text.
     * @param options        Selection options.
     * @param streams        Streams from which data will be selected.
     * @param ids            Specified entities to be subscribed. If null, then all entities will be subscribed.
     * @param startTimestamp The start timestamp.
     * @param endTimestamp   The end timestamp
     * @param params         The parameter values of the query.
     * @return An iterable message source to read messages.
     * @throws CompilationException when query has errors.
     */
    InstrumentMessageSource          executeQuery (
            String                                  qql,
            SelectionOptions                        options,
            TickStream []                           streams,
            CharSequence []                         ids,
            long                                    startTimestamp,
            long                                    endTimestamp,
            Parameter ...                           params
    )
            throws CompilationException;

//    /**
//     *  <p>Execute Query and creates a message source for reading data from it,
//     *  according to the specified options. The messages
//     *  are returned from the cursor strictly ordered by time. Within the same
//     *  exact time stamp, the order of messages is undefined and may vary from
//     *  call to call, i.e. it is non-deterministic.</p>
//     *
//     *  <code>select * from bars</code>
//     *
//     *  @param qql      Query text element.
//     *  @param options  Selection options.
//     *  @param streams  Streams from which data will be selected.
//     *  @param ids      Specified entities to be subscribed. If null, then all entities will be subscribed.
//     *  @param time     The start timestamp.
//     *  @param params   The parameter values of the query.
//     *
//     *  @return         An iterable message source to read messages.
//     *
//     *  @throws CompilationException when query has errors.
//     */
//    InstrumentMessageSource          executeQuery (
//        Element                                 qql,
//        SelectionOptions                        options,
//        TickStream []                           streams,
//        CharSequence []                         ids,
//        long                                    time,
//        Parameter ...                           params
//    )
//        throws CompilationException;


    /**
     * Topic API. May be not be supported by some implementations. Use {@link #isTopicDBSupported()} to check this.
     */
    TopicDB getTopicDB();

    /**
     * @return true if this DB instance supports topics. If this methods returns false then {@link #getTopicDB()} is unavailable.
     */
    boolean isTopicDBSupported();
}