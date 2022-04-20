package com.epam.deltix.computations.finanalysis.util;

public class MacdProcessor {
    public double value = Double.NaN;
    public double signal = Double.NaN;
    public double histogram = Double.NaN;

    private double previousHistogram = Double.NaN;
    private EmaProcessor fastEMA = null;
    private EmaProcessor slowEMA = null;
    private EmaProcessor signalEMA = null;

    public MacdProcessor() {
        this(12, 26, 9);
    }

    public MacdProcessor(int fastPeriod, int slowPeriod, int signalPeriod) {
        fastEMA = new EmaProcessor(fastPeriod);
        slowEMA = new EmaProcessor(slowPeriod);
        signalEMA = new EmaProcessor(signalPeriod);
    }

    public void add(double value, long timestamp) {
        fastEMA.add(value, timestamp);
        slowEMA.add(value, timestamp);

        double macd = fastEMA.value - slowEMA.value;

        signalEMA.add(macd, timestamp);

        // Update MACD values.
        this.value = macd;
        signal = signalEMA.value;
        histogram = macd - signal;

        previousHistogram = histogram;
    }
}
