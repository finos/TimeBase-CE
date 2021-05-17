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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.security.AuthorizationController;
import com.epam.deltix.util.time.Periodicity;

/**
 *  Methods specific to the Deltix implementation of WritableTickStream.
 */
public interface DXTickStream extends WritableTickStream, AuthorizationController.OwnedProtectedResource, AuthorizationController.NamedProtectedResource {

    /**
     *  Returns parent database.
     */
    DXTickDB                 getDB ();

    /**
     *  Returns unique identifier of the stream.
     */
    @Override
    String                   getKey();

    /**
     *  Returns the target number of files to be used for storing data.
     */
    int                      getDistributionFactor ();
    
    /**
     *  Sets the target number of files to be used for storing data.
     */
    void                     setTargetNumFiles (int value);
    
    /**
     *  Sets a user-readable short name.
     */
    void                     setName (String name);
    
    /**
     *  Sets a user-readable multi-line description.
     */
    void                     setDescription (String description);

    /**
     *  Sets a stream owner.
     */
    void                     setOwner(String owner);

    /**
     *  Returns stream owner.
     */
    String                   getOwner();

    /**
     *  Returns the durability scope.
     */
    StreamScope              getScope ();

    /**
     *  Marks this stream as multi-type (Polymorphic),
     *  capable of containing messages of the several types.
     *
     *  @param cds           The descriptors of the classes describing messages to be
     *                      contained in the stream.
     */
    
    void                     setPolymorphic (
        RecordClassDescriptor ...               cds
    );
            
    /**
     *  Marks this stream as fixed-type (monomorphic), 
     *  capable of containing messages of a single specified type.
     * 
     *  @param cd           The descriptor of the class describing messages to be 
     *                      contained in the stream. 
     */
    void                     setFixedType (
        RecordClassDescriptor                   cd
    );
    
    /**
     *  Deletes this stream
     */
    void                     delete ();

    /**
     * Sets new key for the stream 
     * @param key   New key of the stream
     */
    void                     rename(String key);

    /**
     * Deletes stream data that is older than a specified time
     * @param time  Purge time in milliseconds
     */
    void                     purge(long time);

    /**
     * Deletes stream data in specific space that is older than a specified time
     * @param time  Purge time in milliseconds
     * @param space Space to be purged
     */
    void                     purge(long time, String space);

    /**
     * Executes stream background transformation task.
     * @param task  Task to execute in background.
     */
     void                    execute(TransformationTask task);

    /**
     * Gets stream background process information.
     * @return  the active BackgroundProcessInfo or null, if no active process running.
     */
    BackgroundProcessInfo    getBackgroundProcess();

    /**
     * Aborts active background process if any exists
     */
    void                     abortBackgroundProcess();

    /**
     * Gets stream periodicity. 
     * @return stream periodicity
     */
    Periodicity              getPeriodicity();

    /**
     * Sets stream periodicity.
     * <p>By default, stream has <code>Periodicity.Type.IRREGULAR</code>.</p>
     *
     * @param periodicity   Periodicity value. If null, stream will have IRREGULAR periodicity.
     */
    void                     setPeriodicity(Periodicity periodicity);

    /**
     * Enables/Disable stream data caching into memory.
     *
     * @param value
     *        If <code>true</code>, stream data will be loaded into memory.
     */

    void                     setHighAvailability(boolean value);
    
    boolean                  getHighAvailability();

     /**
     * Gets stream options.
     * @return stream options.
     */
    StreamOptions            getStreamOptions();

    /**
     * Returns stream DDL description.
     * @return DDL.
     */
    String                   describe();

    /// locking support

    /**
     *  Non-blocking operation that acquires a WRITE lock of this stream (or fails immediately).
     *  
     *  @return     A not-null DBLock object representing the newly-acquired lock
     *
     *  @throws     StreamLockedException
     *              if this stream is already locked by another client
     *
     *  @throws     UnsupportedOperationException
     *              if this stream is not supporting locks
     */
    DBLock                   lock()
            throws StreamLockedException, UnsupportedOperationException;

    /**
     *  Non-blocking operation that lock of this stream (or fails immediately).
     *
     *  @param      type   Type of lock (WRITE or READ)
     *  @return     A not-null DBLock object representing the newly-acquired lock
     *
     *  @throws     StreamLockedException
     *              if this stream is already locked by another client
     *
     *  @throws     UnsupportedOperationException
     *              if this stream is not supporting locks
     */
    DBLock                   lock(LockType type)
            throws StreamLockedException, UnsupportedOperationException;


    /** Blocking operation that attempts to obtain WRITE lock on this stream.
     *  If lock cannot be obtained during specified timeout operation fails with StreamLockedException.
     *
     *  @param      timeout         timeout to wait in milliseconds
     *  @return     A not-null DBLock object representing the newly-acquired lock
     *
     *  @throws     StreamLockedException
     *              if this stream is already locked by another client
     *
     *  @throws     UnsupportedOperationException
     *              if this stream is not supporting locks
     *
     */
    DBLock                   tryLock(long timeout)
            throws StreamLockedException, UnsupportedOperationException;

    /** Blocking operation that attempts to obtain given type of lock on this stream.
     *  If lock cannot be obtained during specified timeout operation fails with StreamLockedException.
     *
     *  @param      type        Type of lock (WRITE or READ)
     *  @param      timeout     timeout to wait in milliseconds
     *  @return     A not-null DBLock object representing the newly-acquired lock
     *
     *  @throws     StreamLockedException
     *              if this stream is already locked by another client
     *
     *  @throws     UnsupportedOperationException
     *              if this stream is not supporting locks
     *
     */
    DBLock                   tryLock(LockType type, long timeout)
            throws StreamLockedException, UnsupportedOperationException;

    /**
     * Blocking operation that attempts to verify that given lock is applied to the stream.
     *
     *  @param      type        Type of lock (WRITE or READ)
     *  @return     A DBLock object representing the applied lock, or null if lock is not exists
     *
     *  @throws     StreamLockedException
     *              if this stream is already locked by another client
     *
     *  @throws     UnsupportedOperationException
     *              if this stream is not supporting locks
     */
    DBLock                   verify(DBLock lock, LockType type)
            throws StreamLockedException, UnsupportedOperationException;

    /*
     * Enable tracking of stream operations (purge, truncate, schema change)
     */
    boolean                  enableVersioning();

    /**
     * Get latest stream operation version.
     *
     * @return     last index of stream operation, or -1 if version tracking is not enabled
     */
    long                     getDataVersion();

    /*
     * Get latest replicated stream operation version.
     */
    long                     getReplicaVersion();

    /*
     * Set version of replica. Usually used to store "data version" of the source stream for replicated stream.
     *
     * @param      version        version number
     */
    void                     setReplicaVersion(long version);

    /*
     * Get entities contains in same "buckets" if stream has Distribution Factor (DF) != MAX
     *
     * @param entities  not-null entities list.
     *
     * @return     list of entities
     */
    IdentityKey[]     getComposition (IdentityKey ... entities);


    /*
     * Remains list of instruments
     *
     * @param from source entities list
     * @param to target entities list
     *
     */
    void                     renameInstruments(IdentityKey[] from, IdentityKey[] to);

    default MessageSource<InstrumentMessage> selectMulticast(boolean raw) {
        throw new UnsupportedOperationException();
    }

    static RecordClassDescriptor[]      getClassDescriptors(DXTickStream stream) {
        if (stream.isFixedType())
            return new RecordClassDescriptor[] {stream.getFixedType()};
        else
            return stream.getPolymorphicDescriptors();
    }
}
