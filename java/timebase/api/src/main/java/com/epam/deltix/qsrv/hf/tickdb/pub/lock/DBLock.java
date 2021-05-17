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
package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

/**
 * A token representing a lock on a timebase stream.
 *
 * <p> A lock object is created each time a lock is acquired on a stream via
 * one of the {@link deltix.qsrv.hf.tickdb.pub.DXTickStream#lock() lock()} or
 * {@link deltix.qsrv.hf.tickdb.pub.DXTickStream#tryLock(long)} tryLock() }
 * methods of the {@link deltix.qsrv.hf.tickdb.pub.DXTickStream} class.</p>
 */

public interface DBLock {

    /**
     * Tells whether this lock is shared.
     *
     * @return <code>true</code> if lock is shared,
     *         <code>false</code> if it is exclusive
     */
    
    public LockType  getType();

    /**
     * Tells whether or not this lock is valid.
     *
     * <p> A lock object remains valid until it is released or the associated
     * file channel is closed, whichever comes first.  </p>
     *
     * @return  <code>true</code> if, and only if, this lock is valid.
     * @throws IllegalStateException if lock is not valid.
     */
    
    public boolean  isValid();

    /**
     * Releases this lock.
     *
     * <p> If this lock object is valid then invoking this method releases the
     * lock and renders the object invalid.  If this lock object is invalid
     * then invoking this method has no effect.  </p>         
     */
    public void     release();
}
