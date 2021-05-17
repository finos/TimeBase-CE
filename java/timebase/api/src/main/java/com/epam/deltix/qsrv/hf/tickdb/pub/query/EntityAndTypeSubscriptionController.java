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
package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 * A combination of EntitySubscriptionController and TypeSubscriptionController frequently used together in UHF.
 *
 */
public interface EntityAndTypeSubscriptionController 
    extends EntitySubscriptionController, TypeSubscriptionController 
{
    /**
     *  Add the specified entities and types to subscription. The type and symbol are copied
     *  from the incoming object, if necessary, so the argument can be re-used
     *  after the call.
     *
     * @param entities not-null array of instruments to subscribe.
     * @param types not-null array of type names to subscribe.
     */

    default void add(IdentityKey[] entities, String[] types) {
        addEntities(entities);
        addTypes(types);
    }

    /**
     *  Remove the specified entities and types from subscription. The type and symbol are copied
     *  from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     *
     *  @param entities not-null array of instruments to subscribe.
     *  @param types not-null array of type names to subscribe.
     */

    default void remove(IdentityKey[] entities, String[] types) {
        removeEntities(entities);
        removeTypes(types);
    }
}
