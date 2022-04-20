package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import javax.naming.OperationNotSupportedException;

@Function("SMA")
public class Sma extends DoubleStatefulFunctionBase {

    private DecimalDataQueue queue;
    private long timePeriod = -1;
    private int period = -1;
    private boolean reset = false;

    @Init
    public void initPeriod(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.timePeriod = -1;
        this.reset = reset;
        this.queue = new DecimalDataQueue(period, true, false);
    }

    @Init
    public void initTimePeriod(long timePeriod, @Arg(defaultValue = "false") boolean reset) {
        this.period = -1;
        this.timePeriod = timePeriod;
        this.reset = reset;
        this.queue = new DecimalDataQueue(timePeriod, true);
    }

    @Compute
    public void set(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }

        try {
            if (timePeriod == -1) {
                queue.put(Decimal64.fromDouble(v));
            } else {
                queue.put(Decimal64.fromDouble(v), timestamp);
            }

            value = queue.arithmeticMean().doubleValue();
        } catch (OperationNotSupportedException ignored) {
        }
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (timePeriod == -1) {
                queue = new DecimalDataQueue(period, true, false);
            } else {
                queue = new DecimalDataQueue(timePeriod, true);
            }
        }

        super.reset();
    }

}

