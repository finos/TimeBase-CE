package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ValueListConstraint extends Constraint {
    public final Number []                 values;

    public ValueListConstraint (long location, Number ... values) {
        super (location);
        
        this.values = values;
    }
    
    
}
