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
package com.epam.deltix.qsrv.config;

import com.epam.deltix.snmp.QuantServerSnmpObjectContainer;

import java.io.Closeable;

public interface ServiceExecutor extends Closeable {

    /**
     * Service entry point
     * @param configs QuantService Configurations
     */
    void    run(QuantServiceConfig ... configs);

    /**
     * Implementors for this method may put own SNMP data objects into provided {@code snmpContextHolder}.
     * @param snmpContextHolder container for SNMP data objects
     */
    default void registerSnmpObjects(QuantServerSnmpObjectContainer snmpContextHolder) {
    }
}
