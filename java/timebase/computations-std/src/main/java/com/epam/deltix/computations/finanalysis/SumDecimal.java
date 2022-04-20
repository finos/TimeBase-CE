package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.containers.generated.DecimalLongDataQueue;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.util.annotations.TimestampMs;

import javax.naming.OperationNotSupportedException;

@Function("SUM")
public class SumDecimal {

    private BaseSum sum;

    @Init
    public void init(long timePeriod) {
        sum = new MovingTimeSum(timePeriod);
    }

    @Init
    public void init(int period) {
        sum = new MovingCountSum(period);
    }

    @Compute
    public void compute(@BuiltInTimestampMs @TimestampMs long timestamp, @Decimal long value) {
        sum.compute(timestamp, value);
    }

    @Result
    @Decimal
    public long value() {
        return sum.value();
    }

    @Reset
    public void reset() {
        sum.reset();
    }

    private interface BaseSum {

        void compute(@TimestampMs long ts, @Decimal long v);

        @Decimal
        long value();

        void reset();

    }

    private static class MovingTimeSum implements BaseSum {

        private final DecimalLongDataQueue queue;

        public MovingTimeSum(long timeWindow) {
            this.queue = new DecimalLongDataQueue(timeWindow, true);
        }

        @Override
        public void compute(@TimestampMs long ts, @Decimal long v) {
            try {
                queue.put(v, ts);
            } catch (OperationNotSupportedException ignored) {
            }
        }

        @Override
        public long value() {
            return queue.sum();
        }

        @Override
        public void reset() {
            queue.clear();
        }
    }

    private static class MovingCountSum implements BaseSum {

        private final DecimalLongDataQueue queue;

        public MovingCountSum(int countWindow) {
            this.queue = new DecimalLongDataQueue(countWindow, true, false);
        }

        @Override
        public void compute(@TimestampMs long ts, @Decimal long v) {
            try {
                queue.put(v);
            } catch (OperationNotSupportedException ignored) {
            }
        }

        @Override
        public long value() {
            return queue.sum();
        }

        @Override
        public void reset() {
            queue.clear();
        }
    }

}
