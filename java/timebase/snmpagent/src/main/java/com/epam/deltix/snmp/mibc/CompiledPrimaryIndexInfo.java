package com.epam.deltix.snmp.mibc;

/**
 *
 */
public interface CompiledPrimaryIndexInfo extends CompiledIndexInfo {
    public CompiledObject []        getIndexedChildren ();
    
    public boolean                  isLastImplied ();
}
