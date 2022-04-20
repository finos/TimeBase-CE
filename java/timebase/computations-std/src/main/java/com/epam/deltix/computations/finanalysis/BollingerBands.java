package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.BollingerBandsProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.ObjectStatefulFunctionBase;
import com.epam.deltix.computations.messages.BollingerMessage;

import javax.naming.OperationNotSupportedException;

@Function("BOLLINGER")
public class BollingerBands extends ObjectStatefulFunctionBase<BollingerMessage> {

    private int pointWindow = -1;
    private long timeWindow = -1;
    private double factor;
    private boolean reset = false;
    private BollingerBandsProcessor bollinger;

    public BollingerBands() {
        super(BollingerMessage::new);
    }

    @Init
    public void init(long timeWindow,
                     @Arg(defaultValue = "2.0") double factor,
                     @Arg(defaultValue = "false") boolean reset) {
        this.timeWindow = timeWindow;
        this.factor = factor;
        this.reset = reset;
        bollinger = new BollingerBandsProcessor(timeWindow, factor);
    }

    @Init
    public void init(@Arg(defaultValue = "14") int pointWindow,
                     @Arg(defaultValue = "2.0") double factor,
                     @Arg(defaultValue = "false") boolean reset) {
        this.pointWindow = pointWindow;
        this.factor = factor;
        this.reset = reset;
        bollinger = new BollingerBandsProcessor(pointWindow, factor);
    }

    @Compute
    public void set(@BuiltInTimestampMs long timestamp, double v) {
        try {
            bollinger.add(v, timestamp);
            buffer.setBandWidth(bollinger.bandWidth);
            buffer.setLowerBand(bollinger.lowerBand);
            buffer.setMiddleBand(bollinger.middleBand);
            buffer.setUpperBand(bollinger.upperBand);
            buffer.setPercentB(bollinger.percentB);
            if (value == null) {
                value = buffer;
            }
        } catch (OperationNotSupportedException ignored) {
        }
    }

    @Result
    @Type("OBJECT(com.epam.deltix.computations.messages.BollingerMessage)")
    @Override
    public BollingerMessage get() {
        return value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (timeWindow == -1) {
                bollinger = new BollingerBandsProcessor(pointWindow, factor);
            } else {
                bollinger = new BollingerBandsProcessor(timeWindow, factor);
            }
        }
        super.reset();
    }
}
