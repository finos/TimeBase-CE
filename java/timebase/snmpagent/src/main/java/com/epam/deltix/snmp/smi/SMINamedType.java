package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMINamedType extends SMIType {
    public static final SMINamedType      DisplayString_INSTANCE = 
        new SMINamedType (SMIOctetStringType.INSTANCE, "DisplayString");
    
    public static final SMINamedType      Integer32_INSTANCE = 
        new SMINamedType (SMIIntegerType.INSTANCE, "Integer32");
    
    private final SMIType           base;    
    private final String            name;
    
    public SMINamedType (SMIType base, String name) {
        this.base = base;    
        this.name = name;
    }

    public SMIType              getBase () {
        return base;
    }

    public String               getName () {
        return name;
    }        
}
