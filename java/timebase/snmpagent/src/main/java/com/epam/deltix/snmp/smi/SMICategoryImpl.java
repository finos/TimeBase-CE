package com.epam.deltix.snmp.smi;

import java.util.*;

/**
 *
 */
final class SMICategoryImpl
    extends SMIPrimitiveContainerImpl <SMICategoryImpl>
    implements SMICategory 
{
    SMICategoryImpl (
        SMICategoryImpl         parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public SMICategory          addObjectIdentifier (int id, String name) {
        checkChild (id, name);
        
        SMICategoryImpl      node = 
            new SMICategoryImpl (this, new SMIOID (getOid (), id), name, null);
        
        registerChild (node);
        
        return (node);
    }
    
    @Override
    public SMITable             addTable (
        int                         id,
        String                      name, 
        String                      description
    )
    {
        checkChild (id, name);
        
        SMITableImpl      node = 
            new SMITableImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description
            );
        
        registerChild (node);
        
        return (node);
    }
    
    @Override
    public SMIPrimitive         addObjectType (
        int                         id, 
        String                      name, 
        SMIType                     type, 
        SMIAccess                   access,
        String                      description
    )
    {
        checkChild (id, name);
        
        SMIPrimitiveNodeImpl      node = 
            new SMIPrimitiveNodeImpl (
                this, 
                new SMIOID (getOid (), id), 
                name,
                type,
                access,
                description
            );
        
        registerChild (node);
        
        return (node);
    }        
}
