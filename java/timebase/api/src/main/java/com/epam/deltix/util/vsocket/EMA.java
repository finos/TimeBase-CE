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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.time.TimeKeeper;

/**
 *  Exponential moving average: 
 *  EMA(v) = v*(1-k)+ previousEMA*k, where
 *  k = e^(-timeSincePreviousMeasurement/decayTime)
 */
public class EMA {
    private long            lastTimestamp = Long.MIN_VALUE;
    private double          lastAverage = Double.NaN;
    private double          lastValue = Double.NaN;
    private double          factor;
    
    /**
     * Creates a running EMA instance
     * 
     * @param decayTimeMillis   Time in milliseconds during which a reading's 
     *                          weight falls to 1/e.
     */
    public EMA (double decayTimeMillis) {
        factor = -1 / decayTimeMillis;
    }
    
    public double           update (double value) {
        register (value);        
        return (lastAverage);
    }

    public void             clear () {
        lastTimestamp = Long.MIN_VALUE;
        lastAverage = Double.NaN;
        lastValue = Double.NaN;
    }

    public double           getLastRegisteredValue () {
        return (lastValue);
    }
    
    public double           getAverage () {
        return (lastAverage);
    }

    public void             register (double value) {
        register (value, TimeKeeper.currentTime);
    }
    
    public void             register (double value, long time) {
        lastValue = value;
        
        if (lastTimestamp == Long.MIN_VALUE)
            lastAverage = value;
        else {
            double          alpha = Math.exp ((time - lastTimestamp) * factor);
            
            lastAverage = value * (1 - alpha) + lastAverage * alpha;
        }
        
        lastTimestamp = time;        
    }        
}