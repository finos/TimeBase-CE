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
package com.epam.deltix.qsrv.hf.pub;

/**
 * User-defined value is out of required range.
 */
public class ValueOutOfRangeException extends RuntimeException {
    public final Object     value;
    public final Object     min;
    public final Object     max;

    public ValueOutOfRangeException (Object value, Object min, Object max) {
        super (
            value + " is out of allowed range: [" +
            (min == null ? "" : min) + "src/main " +
            (max == null ? "" : max) + "]"
        );

        this.value = value;
        this.min = min;
        this.max = max;
    }
}