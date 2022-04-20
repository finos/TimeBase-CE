package com.epam.deltix.computations.finanalysis.util;

public class EmaProcessor {
    public double value;

    private int period;
    private int count = 0;
    private double factor;
    private double sum = 0;

    public EmaProcessor() {
        this(14);
    }

    public EmaProcessor(int period) {
        this.period = period;
        factor = 2.0 / (period + 1);
    }

    public EmaProcessor(double factor) {
        period = (int) Math.floor((2.0 / factor) - 0.5);
        this.factor = factor;
    }

    public void add(double value, long timestamp) {
        count++;

        if (count <= period) {
            sum += value;
            this.value = sum / count;
        } else {
            double last = this.value;

            this.value = last + factor * (value - last);
        }
    }

}
