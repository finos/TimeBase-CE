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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.util.collections.DoubleQueue;

/**
 *  Exponential Moving Average
 */
public class EMA extends DoubleQueue {    
    private double      factor  = 0.5;
    private double      ema     = 0.0;
    
    public EMA (int numPeriods) {
        super (numPeriods);
        factor = 2.0 / (numPeriods + 1);
    }
    
    public EMA (int numPeriods, double aFactor) {
        super (numPeriods);
        factor = (aFactor>=0.0 && aFactor<=1.0) ? aFactor : 2.0/(numPeriods + 1);
    }
    
    public double       getFactor () {
        return (factor);
    }
    
    public void         setFactor (double aFactor) {
        assert aFactor >= 0.0 && aFactor <= 1.0; 
        factor = aFactor;
    }    
    
    public double       getAverage () {
        return isFull() ? ema : ema / size();
    }
    
    @Override
    public void         offer (double value) {
        if (isFull ()) {
            ema = (1.0 - factor)* ema + factor * value;
        }
        else{
            ema += value;
            super.offer (value);
            if (isFull() )
                ema /= size();   //simple MA is used as the first ema value        
        }
    }

    @Override
    public void         clear () {
        super.clear ();
        ema = 0;
    }

    @Override
    public double       poll () {
        throw new UnsupportedOperationException ();
    }    
}