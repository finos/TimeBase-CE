/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.computations.histogram;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;

public class Utils {

    static DoubleArrayList decimalToDoubleList(@Decimal LongArrayList percentiles) {
        DoubleArrayList doubles = new DoubleArrayList();
        for (int i = 0; i < percentiles.size(); ++i) {
            if (Decimal64Utils.isLess(percentiles.get(i), Decimal64Utils.ZERO) ||
                Decimal64Utils.isLess(Decimal64Utils.ONE, percentiles.get(i))) {
                throw new RuntimeException("Percentile value must be 0 <= x <= 1: " + Decimal64Utils.toString(percentiles.get(i)));
            }

            doubles.add(Decimal64Utils.toDouble(percentiles.get(i)));
        }

        return doubles;
    }

}
