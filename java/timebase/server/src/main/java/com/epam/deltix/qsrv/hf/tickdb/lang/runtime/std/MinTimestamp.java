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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (TIMESTAMP)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "TIMESTAMP?", args = { "TIMESTAMP?" })
public final class MinTimestamp {
    private long        minValue = DateTimeDataType.NULL;

    public long         get () {
        return (minValue);
    }

    public void         set1 (long v) {
        if (v != DateTimeDataType.NULL &&
            (minValue == DateTimeDataType.NULL || v < minValue))
            minValue = v;
    }        
}
