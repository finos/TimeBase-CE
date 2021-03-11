package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMIConstrainedType extends SMIType {
    private final SMIType           base;    
    private final SMIConstraint     constraint;
    
    public SMIConstrainedType (SMIType base, SMIConstraint constraint) {
        this.base = base;    
        this.constraint = constraint;
    }

    public SMIType              getBase () {
        return base;
    }

    public SMIConstraint        getConstraint () {
        return constraint;
    }        
}
