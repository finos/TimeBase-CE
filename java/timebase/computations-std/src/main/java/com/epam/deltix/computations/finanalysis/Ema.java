package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.EmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("EMA")
public class Ema extends DoubleStatefulFunctionBase {

    private EmaProcessor ema;
    private int period = -1;
    private double factor = -1;
    private boolean reset = false;

    @Init
    public void init(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.reset = reset;
        ema = new EmaProcessor(period);
    }

    @Init
    public void init(double factor, @Arg(defaultValue = "false") boolean reset) {
        this.factor = factor;
        this.reset = reset;
        ema = new EmaProcessor(factor);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }
        ema.add(v, timestamp);
        value = ema.value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (period == -1) {
                ema = new EmaProcessor(factor);
            } else {
                ema = new EmaProcessor(period);
            }
        }
        super.reset();
    }

}
