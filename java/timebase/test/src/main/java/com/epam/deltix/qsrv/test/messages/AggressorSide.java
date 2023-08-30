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
package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.OldElementName;
import com.epam.deltix.timebase.messages.SchemaElement;

/**
 * Side of quote or trade. Buy or Sell.
 */
@OldElementName("deltix.qsrv.hf.pub.AggressorSide")
@SchemaElement(
        name = "deltix.timebase.api.messages.AggressorSide",
        title = "Aggressor Side"
)
public enum AggressorSide {
    /**
     * Buy side.
     */
    @SchemaElement(
            name = "BUY"
    )
    BUY(0),

    /**
     * Sell side.
     */
    @SchemaElement(
            name = "SELL"
    )
    SELL(1);

    private final int value;

    AggressorSide(int value) {
        this.value = value;
    }

    public int getNumber() {
        return this.value;
    }

    public static AggressorSide valueOf(int number) {
        switch (number) {
            case 0: return BUY;
            case 1: return SELL;
            default: return null;
        }
    }

    public static AggressorSide strictValueOf(int number) {
        final AggressorSide value = valueOf(number);
        if (value == null) {
            throw new IllegalArgumentException("Enumeration 'AggressorSide' does not have value corresponding to '" + number + "'.");
        }
        return value;
    }
}