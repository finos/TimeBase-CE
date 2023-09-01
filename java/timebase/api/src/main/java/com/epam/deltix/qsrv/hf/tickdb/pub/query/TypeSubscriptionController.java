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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 *  Controls subscription for specific message types. Methods have no return
 *  value, because they can act asynchronously. The implementor of this
 *  interface is not required to remember the subscription details; therefore,
 *  there are no methods provided to query the current state (such as
 *  "hasType", etc.). The user of this interface can independently track
 *  current subscription, if so desired.
 */
public interface TypeSubscriptionController {
    /**
     *  Subscribe to all available types (no filtering).
     */
    public void                     subscribeToAllTypes ();

    /**
     *  Subscribe to specified types.
     */
    public void                     setTypes(String ... names);

    /**
     *  Add the specified type names to subscription.
     */
    public void                     addTypes (String ... names);

    /**
     *  Remove the specified types from subscription.
     */
    public void                     removeTypes (String ... names);
}