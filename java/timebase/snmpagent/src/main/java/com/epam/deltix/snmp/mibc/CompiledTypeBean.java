package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.snmp.smi.SMIType;

/**
 *
 */
class CompiledTypeBean 
    extends CompiledEntityBean <TypeDefinition> 
    implements CompiledType 
{
    SMIType                 type = null;
    
    CompiledTypeBean (TypeDefinition def) {
        super (def);
    }

    @Override
    public SMIType          getType () {
        if (type == null)
            throw new IllegalStateException ("uncompiled");
        
        return (type);
    }        
}
