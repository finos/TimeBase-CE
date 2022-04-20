package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.MinMaxQueue;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("MAX")
public class MaxDouble {

    private MinMaxQueue queue;
    private boolean needTimestamp;

    private long timePeriod = -1;
    private int period = -1;

    @Init
    public void init(long timePeriod) {
        this.timePeriod = timePeriod;
        queue = new MinMaxQueue(timePeriod, MinMaxQueue.MODE.MAXIMUM);
        needTimestamp = true;
    }

    @Init
    public void init(int period) {
        this.period = period;
        queue = new MinMaxQueue(period, MinMaxQueue.MODE.MAXIMUM);
        needTimestamp = false;
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double value) {
        if (TimebaseTypes.isNull(value)) {
            return;
        }

        if (needTimestamp) {
            queue.put(value, timestamp);
        } else {
            queue.put(value);
        }
    }

    @Result
    public double result() {
        return queue.extremum();
    }

    @Reset
    public void reset() {
        if (period == -1) {
            queue = new MinMaxQueue(timePeriod, MinMaxQueue.MODE.MAXIMUM);
        } else {
            queue = new MinMaxQueue(period, MinMaxQueue.MODE.MAXIMUM);
        }
    }

}
