package com.epam.deltix.computations.finanalysis.util;

import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;

import javax.naming.OperationNotSupportedException;

public class KamaProcessor {
    public double value;

    private final DecimalDataQueue difference;
    private double fastest;
    private double slowest;
    private double previousKama = 0;
    private double previousValue = 0;
    private int number = 0;
    private int period;

    public KamaProcessor() {
        this(14);
    }

    public KamaProcessor(int period) {
        this(period, 2, 30);
    }

    public KamaProcessor(int period, int fastPeriod, int slowPeriod) {
        difference = new DecimalDataQueue(period, true, false);
        fastest = 2.0 / (fastPeriod + 1);
        slowest = 2.0 / (slowPeriod + 1);
        this.period = period;
    }

    public void add(double value, long timestamp) throws OperationNotSupportedException {
        if (number > 0) {
            difference.put(Decimal64.fromDouble(value - previousValue));
        }

        if (number >= period) {
            double rate = Math.pow(
                Math.abs(difference.sum().doubleValue() / difference.sumOfAbsoluteValues().doubleValue()) * (fastest - slowest) + slowest, 2
            );

            if (!Double.isInfinite(rate) && !Double.isNaN(rate)) {
                previousKama += rate * (value - previousKama);
            }
        } else {
            previousKama = value;
        }

        this.value = previousKama;
        previousValue = value;
        number++;
    }

}

