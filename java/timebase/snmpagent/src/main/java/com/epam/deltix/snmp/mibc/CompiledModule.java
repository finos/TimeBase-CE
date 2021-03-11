package com.epam.deltix.snmp.mibc;

import java.util.Collection;

/**
 *
 */
public interface CompiledModule {
    public String                           getId ();
    
    public CompiledEntity                   resolve (long location, String id);
    
    public Collection <CompiledEntity>      entities ();        
}
