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

public class MmaProcessor {
    public double value;

    private int period;
    private int count = 0;
    private double sum = 0;
    private double current = 0;

    public MmaProcessor() {
        this(14);
    }

    public MmaProcessor(int period) {
        this.period = period;
    }

    public void add(double Value, long DateTime) {
        count += 1;
        if (count > period) {
            current = (current * (period - 1) + Value) / period;
            this.value = current;
        } else if (count == period) {
            current = (current + Value) / period;
            this.value = current;
        } else {
            current += Value;
            this.value = current / count;
        }
    }

}
