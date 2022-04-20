package com.epam.deltix.computations.finanalysis.util;

import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;

import javax.naming.OperationNotSupportedException;

public class AdxrProcessor {
    public double adxr = Double.NaN;
    public double adx = Double.NaN;
    public double dx = Double.NaN;
    public double plusDI = Double.NaN;
    public double minusDI = Double.NaN;

    private double previousHigh = 0.0;
    private double previousLow = 0.0;
    private double previousClose = 0.0;

    private double previousPDMSum = 0.0;
    private double previousMDMSum = 0.0;

    private double previousTRSum = 0.0;

    private double previousADX = 0.0;

    private double previousPDI = 0.0;
    private double previousMDI = 0.0;

    private int number = 0;
    private int dxNumber = 1;
    private int n;

    private final DecimalDataQueue queue;

    public AdxrProcessor() {
        this(14);
    }

    public AdxrProcessor(int period) {
        n = period;
        queue = new DecimalDataQueue(period, true, false);
    }

    public void add(double open, double high, double low, double close, double volume, long timestamp) throws OperationNotSupportedException {
        if (number != 0) {
            double deltaH = high - previousHigh;
            double deltaL = previousLow - low;

            double pdm = 0.0, mdm = 0.0, tr;

            double pdi, mdi;

            if (deltaH > deltaL && deltaH > 0.0)
                pdm = deltaH;
            if (deltaL > deltaH && deltaL > 0.0)
                mdm = deltaL;

            tr = Math.max(high, previousClose) - Math.min(low, previousClose);

            if (number < n) {
                previousMDMSum += mdm;
                previousPDMSum += pdm;
                previousTRSum += tr;
            } else if (number == n) {
                previousMDMSum += mdm;
                previousMDMSum /= n;

                previousPDMSum += pdm;
                previousPDMSum /= n;

                previousTRSum += tr;
                previousTRSum /= n;
            } else {
                previousMDMSum += mdm - previousMDMSum / n;
                previousPDMSum += pdm - previousPDMSum / n;
                previousTRSum += tr - previousTRSum / n;
            }

            pdi = 100 * (previousPDMSum / previousTRSum);
            if (Double.isInfinite(pdi) || Double.isNaN(pdi))
                pdi = 0.0;

            mdi = 100 * (previousMDMSum / previousTRSum);
            if (Double.isInfinite(mdi) || Double.isNaN(mdi))
                mdi = 0.0;

            previousMDI = minusDI;
            previousPDI = plusDI;

            dx = 100 * Math.abs(pdi - mdi) / (pdi + mdi);
            if (Double.isInfinite(dx) || Double.isNaN(dx))
                dx = 0.0;

            if (dxNumber < n) {
                previousADX += dx;
            } else if (dxNumber == n) {
                previousADX += dx;
                previousADX /= n;
            } else {
                previousADX = (previousADX * (n - 1) + dx) / n;
            }

            dxNumber += 1;

            queue.put(Decimal64.fromDouble(previousADX));

            adxr = (queue.getFirst().doubleValue() + queue.getLast().doubleValue()) / 2.0;
            adx = previousADX;
            plusDI = pdi;
            minusDI = mdi;
        }

        number += 1;

        previousHigh = high;
        previousLow = low;
        previousClose = close;
    }

}
