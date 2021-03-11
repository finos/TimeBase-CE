package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMIRowImpl
    extends SMIPrimitiveContainerImpl <SMITableImpl> 
    implements SMIRow 
{    
    private final SMIIndexInfo        indexInfo;
    
    /**
     *  Row with primary index
     */
    public SMIRowImpl(
            SMITableImpl parent,
            SMIOID oid,
            String name,
            String description,
            int numIndexedChildren,
            boolean lastIndexImplicit
    )
    {
        super (parent, oid, name, description);        
        
        if (numIndexedChildren < 1)
            throw new IllegalArgumentException ("No indexes defined");
        
        indexInfo = new SMIPrimaryIndexImpl (numIndexedChildren, lastIndexImplicit);
    }

    /**
     *  Augmenting row
     */
    SMIRowImpl (
        SMITableImpl            parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description,
        SMIRow                  augmentsRow
    )
    {
        super (parent, oid, name, description);        
        
        indexInfo = new SMIAugmentedIndexImpl (augmentsRow);
    }

    @Override
    public SMIIndexInfo         getIndexInfo () {
        return (indexInfo);
    }
        
    @Override
    public SMIPrimitive         addObjectType (
        int                         id, 
        String                      name, 
        SMIType                     type, 
        SMIAccess                   access,
        String                      description,
        int                         indexDepth
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
        
        if (indexDepth >= 0) {
            if (!(indexInfo instanceof SMIPrimaryIndexImpl))
                throw new IllegalArgumentException (
                    this + " was not constructed with the primary index flags"
                );
            
            SMIPrimaryIndexImpl     pi = (SMIPrimaryIndexImpl) indexInfo;
            SMIPrimitive []         indexes = pi.indexedChildren;
            
            if (indexDepth >= indexes.length)
                throw new IllegalArgumentException (
                    "Index depth " + indexDepth + " > max " + indexes.length
                );
            
            if (indexes [indexDepth] != null)
                throw new IllegalArgumentException(
                        String.format("Index at depth %d is already set: %s [%s] clashes with %s [%s]", indexDepth,
                                indexes[indexDepth], indexes[indexDepth].getDescription(),
                                node, node.getDescription())
                );
            
            indexes [indexDepth] = node;
        }            
        
        return (node);
    }
}
