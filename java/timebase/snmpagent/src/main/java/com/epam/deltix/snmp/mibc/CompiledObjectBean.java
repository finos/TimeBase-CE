package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.snmp.smi.*;

/**
 *
 */
class CompiledObjectBean 
    extends CompiledEntityBean <ObjectDefinition> 
    implements CompiledObject 
{
    SMIOID                      oid = null;
    SMIType                     type = null;
    CompiledIndexInfo           indexInfo = null;
    
    CompiledObjectBean (ObjectDefinition def) {
        super (def);
    }

    @Override
    public SMIOID               getOid () {
        if (oid == null)
            throw new IllegalStateException ("uncompiled");
        
        return (oid);
    }
    
    @Override
    public SMIType              getType () {
        return (type);
    }
    
    @Override
    public SMIAccess            getAccess () {
        if (def instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition    otd = (ObjectTypeDefinition) def;
            
            return (SMIAccess.valueOf (otd.maxAccess.replace ('-', '_')));
        }
        
        return (null);
    }
    
    @Override
    public String               getDescription () {
        if (def instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition    otd = (ObjectTypeDefinition) def;
            
            return (otd.description);
        }
        
        return (null);
    }

    @Override
    public CompiledIndexInfo    getIndexInfo () {
        return (indexInfo);
    }        
}
