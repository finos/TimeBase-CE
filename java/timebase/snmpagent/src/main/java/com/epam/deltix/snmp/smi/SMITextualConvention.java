package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMITextualConvention extends SMIType {
    private final SMIType           base;
    private final String            hint;
    private final String            status;
    private final String            description;
    private final String            reference;

    public SMITextualConvention (
        SMIType                     base, 
        String                      hint, 
        String                      status,
        String                      description, 
        String                      reference
    )
    {
        this.base = base;
        this.hint = hint;
        this.status = status;
        this.description = description;
        this.reference = reference;
    }

    public String                   getDescription () {
        return description;
    }

    public String                   getHint () {
        return hint;
    }

    public String                   getReference () {
        return reference;
    }

    public String                   getStatus () {
        return status;
    }

    public SMIType                  getBase () {
        return base;
    }
    
    
}
