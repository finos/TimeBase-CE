package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMIPrimaryIndexInfo extends SMIIndexInfo {
    public SMIPrimitive []          getIndexedChildren ();
    
    public boolean                  isLastImplied ();
}
