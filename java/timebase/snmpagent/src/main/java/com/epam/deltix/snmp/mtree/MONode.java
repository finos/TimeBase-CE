package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;

/**
 *
 */
public abstract class MONode <T extends SMINode> implements SNMPSPI.Handler {
    private final MONode <?>                     parent;
    private final T                              prototype;
    private final SMIOID                         index;
    
    protected MONode (
        MONode <?>          parent, 
        T                   prototype
    )
    {
        this.parent = parent; 
        this.prototype = prototype;
        this.index = null;    
        
        getRoot ().registerName (this);
    }

    protected MONode (
        MONode <?>          parent, 
        T                   prototype,
        SMIOID              index
    )
    {
        this.parent = parent; 
        this.prototype = prototype;
        this.index = index;            
    }

    public abstract void    clearCache ();
    
    public abstract void    setCacheValid ();
    
    public MORoot           getRoot () {
        return (getParent ().getRoot ());
    }

    public T                getProto () {
        return prototype;
    }
    
    public final SMIOID     getOid () {
        return (prototype.getOid ());
    }
    
    public MONode <?>       getParent () {
        return parent;
    }
    
    public SMIOID           getIndex () {
        return index;
    }

    public Object           getValue () {
        return (this);
    }        
        
    @Override
    public String           toString () {
        return (prototype + " [" + index + "]");
    }        
}
