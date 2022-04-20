package com.epam.deltix.computations.finanalysis.util;

public class AtrProcessor {
    public double atr = Double.NaN;

    private double closePrevious = Double.NaN;
    private double previous = 0.0;
    private double sum = 0.0;

    private int periodForDefault;
    private int count = 0;

    public AtrProcessor() {
        this(14);
    }

    public AtrProcessor(int period) {
        periodForDefault = period;
    }

    public void add(double open, double high, double low, double close, double volume, long timestamp) {
        if (!Double.isNaN(closePrevious)) {
            double tr = Math.max(high, closePrevious) - Math.min(low, closePrevious);

            count++;

            if (count >= periodForDefault) {
                previous = atr = (previous * ((double) periodForDefault - 1) + tr) / (double) periodForDefault;
            } else {
                sum += tr;
                previous = atr = sum / count;
            }
        }

        closePrevious = close;
    }

}
