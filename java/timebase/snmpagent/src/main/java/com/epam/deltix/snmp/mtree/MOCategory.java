package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;

/**
 * 
 */
public final class MOCategory extends MOContainer {    
    MOCategory (
        MONode <?>              parent, 
        SMIPrimitiveContainer   prototype
    )
    {
        super (parent, prototype);
        
        setUpScalarChildren ();
    }   
}
