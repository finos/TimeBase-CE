package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMITableType extends SMIType {
    private final SMIStructureType          entryType;

    public SMITableType (SMIStructureType entryType) {
        this.entryType = entryType;
    }         
}
