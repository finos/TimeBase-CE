package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.MacdProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.ObjectStatefulFunctionBase;
import com.epam.deltix.computations.messages.MACDMessage;

@Function("MACD")
public class Macd extends ObjectStatefulFunctionBase<MACDMessage> {

    private MacdProcessor macd;

    public Macd() {
        super(MACDMessage::new);
    }

    @Init
    public void init(@Arg(defaultValue = "12") int fastPeriod,
                     @Arg(defaultValue = "26") int slowPeriod,
                     @Arg(defaultValue = "9")  int signalPeriod) {
        macd = new MacdProcessor(fastPeriod, slowPeriod, signalPeriod);
    }

    @Compute
    public void set(@BuiltInTimestampMs long timestamp, double v) {
        if (value == null) {
            value = buffer;
        }
        macd.add(v, timestamp);
        value.setHistogram(macd.histogram);
        value.setSignal(macd.signal);
        value.setValue(macd.value);
    }

    @Result
    @Type("OBJECT(com.epam.deltix.computations.messages.MACDMessage)")
    @Override
    public MACDMessage get() {
        return super.get();
    }
}
