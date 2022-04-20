package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.AtrProcessor;
import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;

@Function("ATR")
public class Atr extends DoubleStatefulFunctionBase implements BarFunction {

    private AtrProcessor atr;

    public void init(int period) {
        atr = new AtrProcessor(period);
    }

    @Compute
    @Override
    public void set(@BuiltInTimestampMs long timestamp, double open, double high, double low, double close, double volume) {
        atr.add(open, high, low, close, volume, timestamp);
        value = atr.atr;
    }
}

