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

import org.HdrHistogram.Histogram;

class HdrHistogram implements QuantilesLong {

    private final Histogram histogram;

    private int count;

    HdrHistogram(long minValue, long maxValue, int precision) {
        if (minValue == 0) {
            if (maxValue == 0) {
                this.histogram = new Histogram(precision);
            } else {
                this.histogram = new Histogram(maxValue, precision);
            }
        } else {
            this.histogram = new Histogram(minValue, maxValue, precision);
        }
    }

    @Override
    public void add(long v) {
        histogram.recordValue(v);
        count++;
    }

    @Override
    public long calcQuantile(double q) {
        return histogram.getValueAtPercentile(q * 100);
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public void reset() {
        histogram.reset();
        count = 0;
    }
}
