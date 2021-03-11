package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMIBitsType extends SMIType {
    private final SMINameNumberPair []          bits;

    public SMIBitsType (SMINameNumberPair [] bits) {
        this.bits = bits;
    }

    public SMINameNumberPair []                 getBits () {
        return bits;
    }
    
    
}
