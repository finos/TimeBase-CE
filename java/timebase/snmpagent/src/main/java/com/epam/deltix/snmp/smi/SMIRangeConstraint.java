package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMIRangeConstraint extends SMIConstraint {
    private final Number        min;
    private final Number        max;

    public SMIRangeConstraint (Number min, Number max) {
        this.min = min;
        this.max = max;
    }

    public Number               getMax () {
        return max;
    }

    public Number               getMin () {
        return min;
    }
    
    
}
