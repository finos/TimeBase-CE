package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.qsrv.hf.pub.TimeInterval;

/**
 *  An inclusive range of timestamps. All times are in nanoseconds.
 */
public class TimeRange implements TimeInterval {
    public static final long UNDEFINED = Long.MIN_VALUE;

    public long             from = Long.MAX_VALUE;
    public long             to = Long.MIN_VALUE;
    
    public TimeRange () {
    }
    
    public TimeRange (TimeRange that) {
        this.from = that.from; 
        this.to = that.to;
    }
    
    public TimeRange (long from, long to) {
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

    public void             set (TimeRange that) {
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

    @Override
    public boolean isUndefined() {
        return (from == UNDEFINED || to == UNDEFINED);
    }

    @Override
    public long getFromTime() {
        return from;
    }

    @Override
    public long getToTime() {
        return to;
    }
}
