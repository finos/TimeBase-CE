package com.epam.deltix.snmp.s4jrt;

import org.snmp4j.smi.*;

/**
 *
 */
public interface EntrySupport <EntryType> {
    public OID                      getIndex (EntryType entry);
    
    public Variable                 getValue (EntryType entry, int column);
    
    public int                      getNumColumns ();
}
