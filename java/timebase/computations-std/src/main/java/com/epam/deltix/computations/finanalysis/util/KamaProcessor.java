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

package com.epam.deltix.computations.finanalysis.util;

import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;

import javax.naming.OperationNotSupportedException;

public class KamaProcessor {
    public double value;

    private final DecimalDataQueue difference;
    private double fastest;
    private double slowest;
    private double previousKama = 0;
    private double previousValue = 0;
    private int number = 0;
    private int period;

    public KamaProcessor() {
        this(14);
    }

    public KamaProcessor(int period) {
        this(period, 2, 30);
    }

    public KamaProcessor(int period, int fastPeriod, int slowPeriod) {
        difference = new DecimalDataQueue(period, true, false);
        fastest = 2.0 / (fastPeriod + 1);
        slowest = 2.0 / (slowPeriod + 1);
        this.period = period;
    }

    public void add(double value, long timestamp) throws OperationNotSupportedException {
        if (number > 0) {
            difference.put(Decimal64.fromDouble(value - previousValue));
        }

        if (number >= period) {
            double rate = Math.pow(
                Math.abs(difference.sum().doubleValue() / difference.sumOfAbsoluteValues().doubleValue()) * (fastest - slowest) + slowest, 2
            );

            if (!Double.isInfinite(rate) && !Double.isNaN(rate)) {
                previousKama += rate * (value - previousKama);
            }
        } else {
            previousKama = value;
        }

        this.value = previousKama;
        previousValue = value;
        number++;
    }

}

