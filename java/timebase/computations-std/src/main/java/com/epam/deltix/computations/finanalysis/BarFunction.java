package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;

@FunctionalInterface
public interface BarFunction {

    @Compute
    void set(@BuiltInTimestampMs long timestamp, double open, double high, double low, double close, double volume);

}

