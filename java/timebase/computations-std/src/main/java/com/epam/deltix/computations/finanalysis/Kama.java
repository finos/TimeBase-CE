package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.KamaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import javax.naming.OperationNotSupportedException;

@Function("KAMA")
public class Kama extends DoubleStatefulFunctionBase {

    private KamaProcessor kama;

    @Init
    public void init(@Arg(defaultValue = "14") int period) {
        kama = new KamaProcessor(period);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }

        try {
            kama.add(v, timestamp);
            value = kama.value;
        } catch (OperationNotSupportedException ignored) {
        }
    }

}

