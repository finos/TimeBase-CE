package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMIField {
    private final String                 name;
    private final SMIType                type;

    public SMIField (String name, SMIType type) {
        this.name = name;
        this.type = type;
    }

    public String                       getName () {
        return name;
    }

    public SMIType                      getType () {
        return type;
    }        
}
