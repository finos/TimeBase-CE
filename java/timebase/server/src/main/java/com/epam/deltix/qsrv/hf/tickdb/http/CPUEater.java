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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.util.memory.MemoryDataOutput;

public class CPUEater {
    private final long  avgCostOfNanoTimeCall;
    private final long  cycles;

    private final MemoryDataOutput out = new MemoryDataOutput();
    private final double value = 345.56787899;

    public CPUEater(long nanos) {
        this.avgCostOfNanoTimeCall = nanoTimeCost();

        if (nanos <= avgCostOfNanoTimeCall)
            throw new IllegalArgumentException("Input time is too small: " + nanos);

        // warmup
        for (int j = 0; j < 1000; j++)
            execute(100);

        long time10 = measureExecution(10);
        long time50 = measureExecution(50);
        double c = time50 / time10 / 5.0;

        long low = (nanos / time10 * 10);
        long high = (long) (low / c);
        long count = low + (high - low) / 2;
        long increment = Math.abs((high - low) / 4);

        if (increment == 0)
            increment = 100;

        long time = measureExecution(count);
        while (time < nanos) {
            count += increment;
            time = measureExecution(count);
        }
        cycles = low;
    }

    private static long     nanoTimeCost() {
        final int N = 30000;
        long enterTime = System.nanoTime();
        for (int i = 0; i < N; i++) {
            System.nanoTime();
        }
        long exitTime = System.nanoTime();
        return (exitTime - enterTime) / (N + 2);
    }

    private void            execute(long cycles) {
        for (int i = 0; i < cycles; i++) {
            out.reset();
            out.writeScaledDouble(value);
        }
    }

//        // non-deterministic execution time on high cpu load
//        private void            execute(long cycles) {
//            for (int i = 0; i < cycles; i++) {
//                try {
//                    Thread.sleep(0);
//                } catch (InterruptedException e) {
//                    // ignore
//                }
//            }
//        }

    public void             run() {
        execute(cycles);
    }

    private long            measureExecution(long cycles) {
        long enterTime = System.nanoTime();
        for (int j = 0; j < 20000; j++)
            execute(cycles);
        long exitTime = System.nanoTime();
        long time = avgCostOfNanoTimeCall + (exitTime - enterTime) / 20000;
        //System.out.println("Time of execution(" + cycles  + "): " + time);
        return time;
    }
}