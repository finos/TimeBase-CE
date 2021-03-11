package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMIChoiceType extends SMIType {
    private final SMIField []           fields;

    public SMIChoiceType (SMIField [] fields) {
        this.fields = fields;
    }

    public SMIField []                  getFields () {
        return fields;
    }
    
    
}
