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
package com.epam.deltix.computations;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.util.collections.generated.LongArrayList;

@FunctionsRepo
public class Range {

    @Function("RANGE")
    public static boolean range(long startInclusive, long endExclusive, @Result LongArrayList result) {
        for (long i = startInclusive; i < endExclusive; i++) {
            result.add(i);
        }
        return true;
    }

    @Function("RANGE")
    public static boolean range(long startInclusive, long endExclusive, long step, @Result LongArrayList result) {
        for (long i = startInclusive; i < endExclusive; i+=step) {
            result.add(i);
        }
        return true;
    }

    @Function("RANGE")
    public static boolean decimalRange(@Decimal long startInclusive, @Decimal long endExclusive, @Decimal long step, @Result @Decimal LongArrayList result) {
        for (@Decimal long i = startInclusive; Decimal64Utils.isLess(i, endExclusive); i = Decimal64Utils.add(i, step)) {
            result.add(i);
        }
        return true;
    }

}