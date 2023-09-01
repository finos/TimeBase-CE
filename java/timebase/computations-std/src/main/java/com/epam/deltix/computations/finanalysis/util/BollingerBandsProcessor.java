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

public class BollingerBandsProcessor {
    public double upperBand = Double.NaN;
    public double lowerBand = Double.NaN;
    public double middleBand = Double.NaN;
    public double bandWidth = Double.NaN;
    public double percentB = Double.NaN;
    private final double multiplier;
    private final DecimalDataQueue queue;
    private boolean valuesReady = false;

    private final boolean needTime;

    public BollingerBandsProcessor() {
        this(14, 2.0);
    }

    public BollingerBandsProcessor(int pointWindow, double factor) {
        needTime = false;
        queue = new DecimalDataQueue(pointWindow, true, needTime);
        multiplier = factor;
    }

    public BollingerBandsProcessor(long timeWindow, double factor) {
        needTime = true;
        queue = new DecimalDataQueue(timeWindow, true);
        multiplier = factor;
    }

    public void add(double value, long timestamp) throws OperationNotSupportedException {
        // Put value to the queue.
        if (needTime) {
            queue.put(Decimal64.fromDouble(value), timestamp);
        } else {
            queue.put(Decimal64.fromDouble(value));
        }

        // Check whether the values are ready.
        if (!valuesReady && queue.size() > 0)
            valuesReady = true;

        // Calculate values of Bollinger technical indicator.
        if (valuesReady) {
            // Calculate base values.
            double middle = queue.expectedValue().doubleValue();
            double deviation = queue.standardDeviationPopulation().doubleValue();

            percentB = (value - (middle - multiplier * deviation)) / (2 * multiplier * deviation);
            bandWidth = 2 * multiplier * deviation / middle;

            // Check for invalid values.
            if (Double.isNaN(percentB) || Double.isInfinite(percentB))
                percentB = 0.0;

            if (Double.isNaN(bandWidth) || Double.isInfinite(bandWidth))
                bandWidth = 0.0;

            // Save result.
            middleBand = middle;
            upperBand = middle + multiplier * deviation;
            lowerBand = middle - multiplier * deviation;
        }
    }

}

