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
package com.epam.deltix.computations.finanalysis.util;

public class EmaProcessor {
    public double value;

    private int period;
    private int count = 0;
    private double factor;
    private double sum = 0;

    public EmaProcessor() {
        this(14);
    }

    public EmaProcessor(int period) {
        this.period = period;
        factor = 2.0 / (period + 1);
    }

    public EmaProcessor(double factor) {
        period = (int) Math.floor((2.0 / factor) - 0.5);
        this.factor = factor;
    }

    public void add(double value, long timestamp) {
        count++;

        if (count <= period) {
            sum += value;
            this.value = sum / count;
        } else {
            double last = this.value;

            this.value = last + factor * (value - last);
        }
    }

}
