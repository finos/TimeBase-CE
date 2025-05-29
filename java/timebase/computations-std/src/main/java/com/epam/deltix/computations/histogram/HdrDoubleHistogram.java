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

import org.HdrHistogram.DoubleHistogram;

class HdrDoubleHistogram implements QuantilesDouble {

    private final DoubleHistogram histogram;
    private int count;

    HdrDoubleHistogram(long maxToMinRatio, int precision) {
        if (maxToMinRatio == 0) {
            this.histogram = new DoubleHistogram(precision);
        } else {
            this.histogram = new DoubleHistogram(maxToMinRatio, precision);
        }
    }

    @Override
    public void add(double v) {
        histogram.recordValue((long) v);
        count++;
    }

    @Override
    public double calcQuantile(double q) {
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
