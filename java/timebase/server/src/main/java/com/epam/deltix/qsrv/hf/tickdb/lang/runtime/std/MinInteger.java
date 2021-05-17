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

import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (INTEGER)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "INTEGER?", args = { "INTEGER?" })
public final class MinInteger {
    private long        minValue = IntegerDataType.INT64_NULL;

    public long         get () {
        return (minValue);
    }

    public void         set1 (long v) {
        if (v != IntegerDataType.INT64_NULL &&
            (minValue == IntegerDataType.INT64_NULL || v < minValue))
            minValue = v;
    }        
}
