package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class SizeConstraint extends Constraint {
    public final Constraint           constraint;

    public SizeConstraint (long location, Constraint constraint) {
        super (location);
        
        this.constraint = constraint;
    }
    
    
}
