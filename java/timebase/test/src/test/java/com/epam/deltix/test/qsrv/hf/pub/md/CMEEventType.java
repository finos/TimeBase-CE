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
package com.epam.deltix.test.qsrv.hf.pub.md;

import com.epam.deltix.timebase.messages.SchemaElement;

/**
 * Represents the type of event.
 */
@SchemaElement(
        name = "deltix.qsrv.hf.pub.md.CMEEventType",
        title = "CME Event Type"
)
public enum CMEEventType {
    /**
     */
    @SchemaElement(
            name = "UNKNOWN",
            title = "Unknown"
    )
    UNKNOWN(1),

    /**
     */
    @SchemaElement(
            name = "ACTIVATION",
            title = "Activation"
    )
    ACTIVATION(5),

    /**
     */
    @SchemaElement(
            name = "LASTELIGIBLETRADEDATE",
            title = "Last eligible trade date"
    )
    LASTELIGIBLETRADEDATE(7);

    private final int value;

    CMEEventType(int value) {
        this.value = value;
    }

    public int getNumber() {
        return this.value;
    }

    public static CMEEventType valueOf(int number) {
        switch (number) {
            case 0: return UNKNOWN;
            case 5: return ACTIVATION;
            case 7: return LASTELIGIBLETRADEDATE;
            default: return null;
        }
    }

    public static CMEEventType strictValueOf(int number) {
        final CMEEventType value = valueOf(number);
        if (value == null) {
            throw new IllegalArgumentException("Enumeration 'CMEEventType' does not have value corresponding to '" + number + "'.");
        }
        return value;
    }
}

