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
package com.epam.deltix.qsrv.hf.tickdb.tests.reports;

import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

public class Metrics {

    private static final DecimalFormat decimalFormat;

    static {
        decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        decimalFormat.applyPattern("###.#####");
    }

    public static abstract class BaseTimestampedMetric<T> implements TimestampedMetric<T> {

        protected final LongArrayList timestamps = new LongArrayList();

        @Override
        public synchronized LongArrayList timestamps() {
            return timestamps;
        }
    }

    public static class DoubleMetric implements Metric<Double> {

        private final DoubleArrayList values = new DoubleArrayList();

        @Override
        public synchronized DoubleMetric addValue(Double value) {
            values.add(value);
            return this;
        }

        @Override
        public String valueToString(Double value) {
            return decimalFormat.format(value);
        }

        @Override
        public synchronized Collection<Double> values() {
            return values;
        }
    }

    public static class DoubleTimestampedMetric extends BaseTimestampedMetric<Double> {

        private final DoubleArrayList values = new DoubleArrayList();

        @Override
        public synchronized DoubleTimestampedMetric addValue(long timestamp, Double value) {
            timestamps.add(timestamp);
            values.add(value);
            return this;
        }

        @Override
        public String valueToString(Double value) {
            return decimalFormat.format(value);
        }

        @Override
        public synchronized Collection<Double> values() {
            return values;
        }
    }

    public static DoubleTimestampedMetric createDoubleTimestamped() {
        return new DoubleTimestampedMetric();
    }

    public static DoubleMetric createDoubleMetric() {
        return new DoubleMetric();
    }

}