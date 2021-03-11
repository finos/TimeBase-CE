package com.epam.deltix.snmp.smi;

/**
 *
 */
class SMIPrimitiveNodeImpl 
    extends SMINodeImpl <SMIPrimitiveContainerImpl> 
    implements SMIPrimitive 
{
    private final SMIType           type;
    private final SMIAccess         access;
    
    SMIPrimitiveNodeImpl (
        SMIPrimitiveContainerImpl   parent,
        SMIOID                      oid, 
        String                      name, 
        SMIType                     type, 
        SMIAccess                   access,
        String                      description
    )
    {
        super (parent, oid, name, description);    
        
        this.type = type;
        this.access = access;
    }

    @Override
    public SMIAccess            getAccess () {
        return access;
    }

    @Override
    public SMIType              getType () {
        return type;
    }
    
    
}
