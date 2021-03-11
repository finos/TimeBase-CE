package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;

/**
 * 
 */
public final class MOPrimitive extends MONode <SMIPrimitive> {
    private boolean                     isValid = false;
    private Object                      value = null;    
    
    public MOPrimitive (
        MOContainer             parent, 
        SMIPrimitive            prototype
    ) 
    {
        super (parent, prototype);                
    }

    public MOPrimitive (
        MOContainer             parent, 
        SMIPrimitive            prototype,
        SMIOID                  index
    ) 
    {
        super (parent, prototype, index);                
    }

    @Override
    public void         clearCache () {
        isValid = false;
    }
    
    @Override
    public void         setCacheValid () {
        this.isValid = true;
    }
    
    public void         setValue (Object value) {
        this.value = value;
        this.isValid = true;
    }
    
    @Override
    public Object       getValue () {
        if (!isValid) {
            value = getRoot ().getSPI ().get (getProto ().getOid (), getIndex ());
            isValid = true;
        }
        
        return (value);
    }            
    
    @Override
    public void         set (SMIOID oid, Object value) {
        // for now accept without a precise oid match requirement. 
        // need to figure out the pattern with .0        
        SMIOID              myOid = getOid ();
        
        if (!oid.startsWith (myOid))
            throw new IllegalArgumentException (oid + " not under " + this);
        
        setValue (value);
    }

    @Override
    public String toString () {
        return (super.toString () + " = " + (isValid ? value : "INVALID"));
    }
    
    
}
