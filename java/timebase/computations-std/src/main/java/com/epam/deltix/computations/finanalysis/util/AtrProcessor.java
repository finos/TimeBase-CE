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

public class AtrProcessor {
    public double atr = Double.NaN;

    private double closePrevious = Double.NaN;
    private double previous = 0.0;
    private double sum = 0.0;

    private int periodForDefault;
    private int count = 0;

    public AtrProcessor() {
        this(14);
    }

    public AtrProcessor(int period) {
        periodForDefault = period;
    }

    public void add(double open, double high, double low, double close, double volume, long timestamp) {
        if (!Double.isNaN(closePrevious)) {
            double tr = Math.max(high, closePrevious) - Math.min(low, closePrevious);

            count++;

            if (count >= periodForDefault) {
                previous = atr = (previous * ((double) periodForDefault - 1) + tr) / (double) periodForDefault;
            } else {
                sum += tr;
                previous = atr = sum / count;
            }
        }

        closePrevious = close;
    }

}
