package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMISizeConstraint extends SMIConstraint {
    private final SMIConstraint      constraint;

    public SMISizeConstraint (SMIConstraint constraint) {
        this.constraint = constraint;
    }  
    
    public SMISizeConstraint (Number num) {
        this.constraint = new SMISetConstraint (num);
    }

    public SMIConstraint            getConstraint () {
        return constraint;
    }        
}
