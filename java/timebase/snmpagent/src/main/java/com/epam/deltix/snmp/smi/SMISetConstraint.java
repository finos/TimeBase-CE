package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMISetConstraint extends SMIConstraint {
    private final Number []     values;

    public SMISetConstraint (Number ... values) {
        this.values = values;
    }

    public Number []            getValues () {
        return values;
    }
    
    
}
