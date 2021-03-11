package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.time.TimeKeeper;

/**
 *  Exponential moving average: 
 *  EMA(v) = v*(1-k)+ previousEMA*k, where
 *  k = e^(-timeSincePreviousMeasurement/decayTime)
 */
public class EMA {
    private long            lastTimestamp = Long.MIN_VALUE;
    private double          lastAverage = Double.NaN;
    private double          lastValue = Double.NaN;
    private double          factor;
    
    /**
     * Creates a running EMA instance
     * 
     * @param decayTimeMillis   Time in milliseconds during which a reading's 
     *                          weight falls to 1/e.
     */
    public EMA (double decayTimeMillis) {
        factor = -1 / decayTimeMillis;
    }
    
    public double           update (double value) {
        register (value);        
        return (lastAverage);
    }

    public void             clear () {
        lastTimestamp = Long.MIN_VALUE;
        lastAverage = Double.NaN;
        lastValue = Double.NaN;
    }

    public double           getLastRegisteredValue () {
        return (lastValue);
    }
    
    public double           getAverage () {
        return (lastAverage);
    }

    public void             register (double value) {
        register (value, TimeKeeper.currentTime);
    }
    
    public void             register (double value, long time) {
        lastValue = value;
        
        if (lastTimestamp == Long.MIN_VALUE)
            lastAverage = value;
        else {
            double          alpha = Math.exp ((time - lastTimestamp) * factor);
            
            lastAverage = value * (1 - alpha) + lastAverage * alpha;
        }
        
        lastTimestamp = time;        
    }        
}
