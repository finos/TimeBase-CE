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
package com.epam.deltix.computations.api.generated;

import com.epam.deltix.computations.api.annotations.Reset;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.util.annotations.TimeOfDay;

public abstract class TimeOfDayStatefulFunctionBase implements TimeOfDayStatefulFunction {

    protected @TimeOfDay int value = TimebaseTypes.TIMEOFDAY_NULL;

    @Result
    @Override
    public @TimeOfDay int get() {
        return value;
    }

    @Reset
    @Override
    public void reset() {
        value = TimebaseTypes.TIMEOFDAY_NULL;
    }
}