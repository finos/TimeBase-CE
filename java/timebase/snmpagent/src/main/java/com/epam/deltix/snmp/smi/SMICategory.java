package com.epam.deltix.snmp.smi;

import java.util.Collection;

/**
 *
 */
public interface SMICategory extends SMIPrimitiveContainer {
    public SMICategory              addObjectIdentifier (int id, String name);
    
    public SMITable                 addTable (
        int                             id,
        String                          name,
        String                          description
    );
    
    public SMIPrimitive             addObjectType (
        int                             id, 
        String                          name, 
        SMIType                         type, 
        SMIAccess                       access,
        String                          description
    );            
}
