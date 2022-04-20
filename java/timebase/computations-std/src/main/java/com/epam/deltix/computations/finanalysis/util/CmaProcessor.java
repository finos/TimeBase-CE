package com.epam.deltix.computations.finanalysis.util;

public class CmaProcessor {
    public double value;

    private int count;
    private double sum;

    public CmaProcessor() {
    }

    public void add(double value, long timestamp) {
        count++;
        sum += value;
        this.value = sum / count;
    }

}
