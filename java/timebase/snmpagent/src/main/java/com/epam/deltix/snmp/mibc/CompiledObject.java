package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.smi.*;

/**
 *
 */
public interface CompiledObject extends CompiledEntity {
    public SMIOID               getOid ();
    
    public SMIType              getType ();
    
    public SMIAccess            getAccess ();
    
    public String               getDescription ();
    
    public CompiledIndexInfo    getIndexInfo ();
}
