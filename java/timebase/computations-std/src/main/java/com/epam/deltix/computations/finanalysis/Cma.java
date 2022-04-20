package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.CmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("CMA")
public class Cma extends DoubleStatefulFunctionBase {

    private CmaProcessor cma = new CmaProcessor();
    private boolean reset = false;

    public void init(@Arg(defaultValue = "false") boolean reset) {
        this.reset = reset;
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }
        cma.add(v, timestamp);
        value = cma.value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            cma = new CmaProcessor();
        }
        super.reset();
    }

}

