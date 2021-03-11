package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMINameNumberPair {
    private final String                 name;
    private final Number                 number;

    public SMINameNumberPair (String name, Number number) {
        this.name = name;
        this.number = number;
    }

    public String                       getName () {
        return name;
    }

    public Number                       getNumber () {
        return number;
    }        
}
