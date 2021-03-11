package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.util.collections.DoubleQueue;

/**
 *  Exponential Moving Average
 */
public class EMA extends DoubleQueue {    
    private double      factor  = 0.5;
    private double      ema     = 0.0;
    
    public EMA (int numPeriods) {
        super (numPeriods);
        factor = 2.0 / (numPeriods + 1);
    }
    
    public EMA (int numPeriods, double aFactor) {
        super (numPeriods);
        factor = (aFactor>=0.0 && aFactor<=1.0) ? aFactor : 2.0/(numPeriods + 1);
    }
    
    public double       getFactor () {
        return (factor);
    }
    
    public void         setFactor (double aFactor) {
        assert aFactor >= 0.0 && aFactor <= 1.0; 
        factor = aFactor;
    }    
    
    public double       getAverage () {
        return isFull() ? ema : ema / size();
    }
    
    @Override
    public void         offer (double value) {
        if (isFull ()) {
            ema = (1.0 - factor)* ema + factor * value;
        }
        else{
            ema += value;
            super.offer (value);
            if (isFull() )
                ema /= size();   //simple MA is used as the first ema value        
        }
    }

    @Override
    public void         clear () {
        super.clear ();
        ema = 0;
    }

    @Override
    public double       poll () {
        throw new UnsupportedOperationException ();
    }    
}

