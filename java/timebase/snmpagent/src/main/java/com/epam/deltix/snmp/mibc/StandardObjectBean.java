package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.smi.*;

/**
 *
 */
public abstract class StandardObjectBean 
    extends StandardEntityBean 
    implements CompiledObject
{
    private final SMIOID        oid;
    private final SMIType       type;
    
    protected StandardObjectBean (String id, SMIOID oid, SMIType type) {
        super (id);
        
        this.oid = oid;
        this.type = type;
    }

    @Override
    public SMIOID               getOid () {
        return (oid);
    }  
    
    @Override
    public SMIType              getType () {
        return (type);
    }
    
    @Override
    public SMIAccess            getAccess () {
        return (null);
    }
    
    @Override
    public String               getDescription () {
        return (null);
    }

    @Override
    public CompiledIndexInfo    getIndexInfo () {
        return (null);
    }        
}
