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
 * @author Daniil Yarmalkevich
 * Date: 11/14/2019
 */
public interface SymbolAndTypeSubscriptionController extends
        SymbolSubscriptionController, EntityAndTypeSubscriptionController {

    /**
     * Add symbols of certain types to the subscription.
     * @param symbols symbols notations
     * @param types types (full names)
     */
    void        add(
            CharSequence[] symbols,
            String[] types
    );

    /**
     * Remove symbols of certain types from the subscription.
     * @param symbols symbols notations
     * @param types types (full names)
     */
    void        remove(
            CharSequence[] symbols,
            String[] types
    );

}