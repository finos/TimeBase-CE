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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *  Counts the throughput in messages per second. 
 */
public class Meter <T> extends MessageFilter <T> {
    private long            mCount;
    private long            mBaseTime;
    
    public Meter (MessageChannel<T> out) {
        super (out);
        reset ();
    }
    
    public synchronized void        reset () {
        mCount = 0;
        mBaseTime = System.currentTimeMillis ();
    }

    public synchronized long        getCount () {
        return mCount;
    }
    
    public synchronized long        getTimeSinceReset () {
        return (System.currentTimeMillis () - mBaseTime);
    }

    /**
     *  Return the average number of messages per second since last reset.
     */
    public synchronized double      getRate () {
        long                now = System.currentTimeMillis ();
        return (mCount / ((now - mBaseTime) * 0.001));
    }
    
    /**
     *  Return the average number of messages per second since last reset, and
     *  then reset (in a slightly more efficient way than calling reset separately).
     */
    public synchronized double      getRateAndReset () {
        long                now = System.currentTimeMillis ();
        double              rate = (mCount / ((now - mBaseTime) * 0.001));
        mCount = 0;
        mBaseTime = now;
        return (rate);
    }

    public void                     send (T msg) {
        synchronized (this) {
            mCount++;
        }

        super.send (msg);
    }
}