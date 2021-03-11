package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMIPrimitive extends SMINode {
    public SMIType          getType ();
    
    public SMIAccess        getAccess ();
}
