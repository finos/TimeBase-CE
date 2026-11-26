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
package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.annotations.TimestampNs;
import com.epam.deltix.util.time.GMT;

/**
 *  An inclusive range of timestamps. All times are in nanoseconds.
 *  <p>
 *  Warning: this class does not implement {@link TimeInterval} because the interface
 *  is for milliseconds and this class uses nanoseconds.
 */
public class TimeRangeNs {
    public static final long UNDEFINED = Long.MIN_VALUE;

    @TimestampNs
    public long             from = Long.MAX_VALUE;
    @TimestampNs
    public long             to = Long.MIN_VALUE;
    
    public TimeRangeNs () {
    }
    
    public TimeRangeNs (TimeRangeNs that) {
        this.from = that.from; 
        this.to = that.to;
    }
    
    public TimeRangeNs (long from, long to) {
        this.from = from; 
        this.to = to;
    }
    
//    public boolean          isNull () {
//        return (to == Long.MIN_VALUE);
//    }
    
    public void             setNull () {
        from = Long.MAX_VALUE;
        to = Long.MIN_VALUE;
    }

    public void             set (TimeRangeNs that) {
        this.from = that.from;
        this.to = that.to;
    }

    public boolean          unionInPlace (long from, long to) {
        boolean     changed = false;

        if (from < this.from) {
            this.from = from;
            changed = true;
        }
           
        if (to > this.to) {
            this.to = to;
            changed = true;
        }
        
        return (changed);
    }
    
    public boolean          intersectInPlace (long from, long to) {
        boolean     changed = false;
        
        if (from > this.from) {
            this.from = from;
            changed = true;
        }
           
        if (to < this.to) {
            this.to = to;
            changed = true;
        }
        
        if (from > to)
            setNull ();
        
        return (changed);
    }

    public boolean isUndefined() {
        return (from == UNDEFINED || to == UNDEFINED);
    }

    public long getFromTime() {
        return from;
    }

    public long getToTime() {
        return to;
    }

    /**
     * Converts this range (nanoseconds) to a {@link TimeInterval} in milliseconds.
     */
    public TimeInterval toTimeIntervalMs() {
        return new com.epam.deltix.qsrv.hf.pub.TimeRange(
                TimeStamp.getMilliseconds(this.from),
                TimeStamp.getMilliseconds(this.to)
        );
    }

    /**
     * Converts an array of ranges (nanoseconds) to an array of {@link TimeInterval} in milliseconds.
     * Null elements are preserved.
     */
    public static TimeInterval[] toTimeIntervalsMs(TimeRangeNs[] rangesNs) {
        TimeInterval[] results = new TimeInterval[rangesNs.length];
        for (int i = 0; i < rangesNs.length; i++) {
            results[i] = rangesNs[i] != null ? rangesNs[i].toTimeIntervalMs() : null;
        }
        return results;
    }

    @Override
    public String toString() {
        return "TimeRangeNs{" +
                "from=" + GMT.formatNanos(from) +
                ", to=" + GMT.formatNanos(to) +
                '}';
    }
}