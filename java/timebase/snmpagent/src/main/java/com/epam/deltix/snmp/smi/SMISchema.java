package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMISchema {
    public static SMICategory       createRoot () {
        return (new SMICategoryImpl (null, new SMIOID (), null, null));
    }
    
    public static SMICategory       createDeltixNode () {
        return (new SMICategoryImpl (null, new SMIOID (1, 3, 6, 1, 4, 1, 39977), "deltix", null));
    }
}
