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

import javax.annotation.Nonnull;

/**
 * Enum version of {@link TickStreamProperties}.
 *
 * @author Alexei Osipov
 */
public enum  TickStreamPropertiesEnum {
    KEY(TickStreamProperties.KEY),
    NAME(TickStreamProperties.NAME),
    DESCRIPTION(TickStreamProperties.DESCRIPTION),
    PERIODICITY(TickStreamProperties.PERIODICITY),
    SCHEMA(TickStreamProperties.SCHEMA),
    TIME_RANGE(TickStreamProperties.TIME_RANGE),
    ENTITIES(TickStreamProperties.ENTITIES),
    HIGH_AVAILABILITY(TickStreamProperties.HIGH_AVAILABILITY),
    UNIQUE(TickStreamProperties.UNIQUE),
    BUFFER_OPTIONS(TickStreamProperties.BUFFER_OPTIONS),

    VERSIONING(TickStreamProperties.VERSIONING),
    DATA_VERSION(TickStreamProperties.DATA_VERSION),
    REPLICA_VERSION(TickStreamProperties.REPLICA_VERSION),

    BG_PROCESS(TickStreamProperties.BG_PROCESS),
    WRITER_CREATED(TickStreamProperties.WRITER_CREATED),
    WRITER_CLOSED(TickStreamProperties.WRITER_CLOSED),

    SCOPE(TickStreamProperties.SCOPE),
    DF(TickStreamProperties.DF),

    OWNER(TickStreamProperties.OWNER),

    LOCATION(TickStreamProperties.LOCATION);

    private final int propertyId;

    TickStreamPropertiesEnum(int propertyId) {
        this.propertyId = propertyId;
    }

    public int getPropertyId() {
        return propertyId;
    }

    @Nonnull
    public static TickStreamPropertiesEnum getValueByPropertyId(int propertyId) {
        // Note: we can use switch case here
        for (TickStreamPropertiesEnum val : TickStreamPropertiesEnum.values()) {
            if (val.getPropertyId() == propertyId) {
                return val;
            }
        }
        throw new IllegalArgumentException("No property with id=" + propertyId);
    }
}