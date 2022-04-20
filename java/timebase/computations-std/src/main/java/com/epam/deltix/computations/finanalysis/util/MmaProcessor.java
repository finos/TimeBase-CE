package com.epam.deltix.computations.finanalysis.util;

public class MmaProcessor {
    public double value;

    private int period;
    private int count = 0;
    private double sum = 0;
    private double current = 0;

    public MmaProcessor() {
        this(14);
    }

    public MmaProcessor(int period) {
        this.period = period;
    }

    public void add(double Value, long DateTime) {
        count += 1;
        if (count > period) {
            current = (current * (period - 1) + Value) / period;
            this.value = current;
        } else if (count == period) {
            current = (current + Value) / period;
            this.value = current;
        } else {
            current += Value;
            this.value = current / count;
        }
    }

}
