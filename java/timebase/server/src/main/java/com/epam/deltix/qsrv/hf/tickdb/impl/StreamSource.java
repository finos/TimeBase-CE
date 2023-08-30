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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.Disposable;

import java.util.Collection;

interface StreamSource extends Disposable {

    /**
     *  Subscribe to all available entities from given time (in nanoseonds).
     */
    public boolean                      subscribeToAllEntities(long timestamp);

    /**
     *  Switch to selective subscription mode (if necessary) and clear the list.
     */
    public boolean                      clearAllEntities();

    /**
     *  Bulk add the specified entities to subscription using specified time (in milliseconds).
     *  The type and symbol are copied from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     */
    public boolean                      addEntities (long timestamp, Collection<IdentityKey> ids);

    /**
     *  Bulk add the specified entities to subscription using specified time (in milliseconds).
     *  The type and symbol are copied from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     */
    public boolean                      addEntities(long timestamp, IdentityKey[] ids);

    /**
     *  Remove the specified entities from subscription. The type and symbol are copied
     *  from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     */
    public boolean                      removeEntities(IdentityKey[] ids);

    /**
     *  Re-open readers for given entities from specified time (in milliseconds).
     */
    public boolean                      reset(long timestamp);

    public boolean                      entityCreated(IdentityKey id);

    public boolean                      spaceCreated(String space);

    public boolean                      handle(MessageSource<?> feed, RuntimeException ex);

    /**
     *  Close the this source and remove all readers
     */
    public void                         close();
}