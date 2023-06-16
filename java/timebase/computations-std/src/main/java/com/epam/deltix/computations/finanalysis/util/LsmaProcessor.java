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

import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;

import javax.naming.OperationNotSupportedException;

public class LsmaProcessor {
    public double slope = Double.NaN;
    public double rSquared = Double.NaN;
    public double value;

    private double timeDivider = 86400000000000L;
    private int number = 0;
    private double sumOfT = 0;
    private double sumOfX = 0;
    private double sumOfTX = 0;
    private double sumOfTT = 0;
    private double sumOfXX = 0;
    private long minTicks;
    private boolean useDateTime;
    private DecimalDataQueue queue;

    public LsmaProcessor() {
        this(14, false);
    }

    public LsmaProcessor(int pointWindow, boolean useDateTime) {
        queue = new DecimalDataQueue(pointWindow, true, false);
        queue.addOnPopListener(this::onPop);
        queue.addOnPushListener(this::onPush);
        this.useDateTime = useDateTime;
    }

    public LsmaProcessor(long timeWindow, boolean useDateTime) {
        queue = new DecimalDataQueue(timeWindow, true);
        queue.addOnPopListener(this::onPop);
        queue.addOnPushListener(this::onPush);
        this.useDateTime = useDateTime;
    }

    public void onPush(Decimal64 value, long timestamp) {
        double xAxis = value.doubleValue();
        double tAxis;

        if (useDateTime) {
            if (number == 0) {
                minTicks = timestamp;
                tAxis = 0.0;
            } else {
                tAxis = (timestamp - minTicks) / timeDivider;
            }
        } else {
            tAxis = number;
        }

        // Update values.
        sumOfT += tAxis;
        sumOfX += xAxis;
        sumOfTX += tAxis * xAxis;
        sumOfTT += tAxis * tAxis;
        sumOfXX += xAxis * xAxis;

        int n = queue.size();

        if (n == 1) {
            this.value = xAxis;
            slope = 0;
            rSquared = 1;
        } else {
            // Calculate regression and line slope
            {
                double d = n * sumOfTT - sumOfT * sumOfT;
                double b = n * sumOfTX / d - sumOfT * (sumOfX / d);
                double a = (sumOfX / n - b * (sumOfT / n));

                this.value = a + b * tAxis;

                slope = n * sumOfTX / d - sumOfT * (sumOfX / d);
            }

            // Calculate R-Squared
            {
                double a = n * sumOfTX - sumOfT * sumOfT;
                double b = n * sumOfXX - sumOfX * sumOfX;
                double c = n * sumOfTT - sumOfT * sumOfT;

                rSquared = (a / b) * (a / c);
            }
        }
        // Increment number.
        number += 1;
    }

    public void onPop(Decimal64 value, long timestamp) {
        double xAxis = value.doubleValue();
        double tAxis;

        if (useDateTime) {
            tAxis = (timestamp - minTicks) / timeDivider;
        } else {
            tAxis = (number - queue.size() - 1);
        }

        // Update values.
        sumOfT -= tAxis;
        sumOfX -= xAxis;
        sumOfTX -= tAxis * xAxis;
        sumOfTT -= tAxis * tAxis;
        sumOfXX -= xAxis * xAxis;
    }

    public void add(double value, long timestamp) throws OperationNotSupportedException {
        if (useDateTime) {
            queue.put(Decimal64.fromDouble(value), timestamp);
        } else {
            queue.put(Decimal64.fromDouble(value));
        }
    }

}
