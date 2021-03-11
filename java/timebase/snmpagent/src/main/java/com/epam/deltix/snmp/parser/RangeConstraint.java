package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class RangeConstraint extends Constraint {
    public final Number             from;
    public final Number             to;

    public RangeConstraint (long location, Number from, Number to) {
        super (location);
        
        this.from = from;
        this.to = to;
    }
    
    
}
