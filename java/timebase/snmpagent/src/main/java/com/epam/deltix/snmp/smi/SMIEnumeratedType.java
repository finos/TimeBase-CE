package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMIEnumeratedType extends SMIType {
    private final SMINameNumberPair []          values;

    public SMIEnumeratedType (SMINameNumberPair [] values) {
        this.values = values;
    }

    public SMINameNumberPair []                 getValues () {
        return values;
    }        
}
