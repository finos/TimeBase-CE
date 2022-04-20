package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.LsmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.ObjectStatefulFunctionBase;
import com.epam.deltix.computations.messages.LSMAMessage;

import javax.naming.OperationNotSupportedException;

@Function("LSMA")
public class Lsma extends ObjectStatefulFunctionBase<LSMAMessage> {

    private int pointWindow = -1;
    private long timeWindow = -1;
    private boolean useDateTime = false;
    private boolean reset = false;
    private LsmaProcessor lsma;

    public Lsma() {
        super(LSMAMessage::new);
    }

    @Init
    public void init(@Arg(defaultValue = "14") int pointWindow,
                     @Arg(defaultValue = "false") boolean useDateTime,
                     @Arg(defaultValue = "false") boolean reset) {
        this.pointWindow = pointWindow;
        this.useDateTime = useDateTime;
        this.reset = reset;
        this.lsma = new LsmaProcessor(pointWindow, useDateTime);
    }

    @Init
    public void init(long timeWindow,
                     @Arg(defaultValue = "false") boolean useDateTime,
                     @Arg(defaultValue = "false") boolean reset) {
        this.timeWindow = timeWindow;
        this.useDateTime = useDateTime;
        this.reset = reset;
        this.lsma = new LsmaProcessor(timeWindow, useDateTime);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        try {
            lsma.add(v, timestamp);
            buffer.setRSquared(lsma.rSquared);
            buffer.setSlope(lsma.slope);
            buffer.setValue(lsma.value);
            if (value == null) {
                value = buffer;
            }
        } catch (OperationNotSupportedException ignored) {
        }
    }

    @Result
    @Type("OBJECT(com.epam.deltix.computations.messages.LSMAMessage)")
    @Override
    public LSMAMessage get() {
        return super.get();
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (timeWindow == -1) {
                lsma = new LsmaProcessor(pointWindow, useDateTime);
            } else {
                lsma = new LsmaProcessor(timeWindow, useDateTime);
            }
        }
        super.reset();
    }
}
