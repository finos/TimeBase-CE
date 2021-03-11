package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMIRow extends SMIPrimitiveContainer {
    @Override
    public SMITable                 getParent ();
    
    public SMIPrimitive             addObjectType (
        int                             id, 
        String                          name, 
        SMIType                         type, 
        SMIAccess                       access,
        String                          description,
        int                             indexDepth
    );    
                
    public SMIIndexInfo             getIndexInfo ();
}
