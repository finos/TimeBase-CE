package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMIStructureType extends SMIType {
    private final SMIField []           fields;

    public SMIStructureType (SMIField[] fields) {
        this.fields = fields;
    }

    public SMIField []                  getFields () {
        return fields;
    }
    
    
}
