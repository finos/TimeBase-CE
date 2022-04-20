package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.MmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;

@Function("MMA")
public class Mma extends DoubleStatefulFunctionBase {

    private int period;
    private boolean reset;
    private MmaProcessor mma;

    public void init(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.reset = reset;
        mma = new MmaProcessor(period);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        mma.add(v, timestamp);
        value = mma.value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            mma = new MmaProcessor(period);
        }
        super.reset();
    }
}

