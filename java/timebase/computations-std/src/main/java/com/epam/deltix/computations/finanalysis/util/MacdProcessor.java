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

public class MacdProcessor {
    public double value = Double.NaN;
    public double signal = Double.NaN;
    public double histogram = Double.NaN;

    private double previousHistogram = Double.NaN;
    private EmaProcessor fastEMA = null;
    private EmaProcessor slowEMA = null;
    private EmaProcessor signalEMA = null;

    public MacdProcessor() {
        this(12, 26, 9);
    }

    public MacdProcessor(int fastPeriod, int slowPeriod, int signalPeriod) {
        fastEMA = new EmaProcessor(fastPeriod);
        slowEMA = new EmaProcessor(slowPeriod);
        signalEMA = new EmaProcessor(signalPeriod);
    }

    public void add(double value, long timestamp) {
        fastEMA.add(value, timestamp);
        slowEMA.add(value, timestamp);

        double macd = fastEMA.value - slowEMA.value;

        signalEMA.add(macd, timestamp);

        // Update MACD values.
        this.value = macd;
        signal = signalEMA.value;
        histogram = macd - signal;

        previousHistogram = histogram;
    }
}
