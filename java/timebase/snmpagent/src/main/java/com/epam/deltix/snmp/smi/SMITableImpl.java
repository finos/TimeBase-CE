package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMITableImpl
    extends SMINodeImpl <SMIPrimitiveContainer> 
    implements SMITable 
{
    private static final Integer []     NO_IDS = { };
    
    private SMIRowImpl          entry = null;
    
    public SMITableImpl (
        SMICategory             parent, 
        SMIOID                  oid, 
        String                  name, 
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public SMIRowImpl           getConceptualRow () {
        return (entry);
    }   
    
    private void                checkRowUnset () 
        throws IllegalArgumentException 
    {
        if (entry != null)
            throw new IllegalArgumentException (
                "Entry already registered: " + entry
            );
    }

    @Override
    public SMIRow               addAugmentingRow (
        int                         id,
        String                      name, 
        String                      description,
        SMIRow                      augmentedRow
    )
    {
        checkRowUnset ();
        
        entry = 
            new SMIRowImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description,
                augmentedRow
            );
        
        return (entry);
    }    
    
    @Override
    public SMIRow               addIndexedRow (
        int                         id,
        String                      name, 
        String                      description,
        int                         numIndexes,
        boolean                     lastIndexIsImplied
    )
    {
        checkRowUnset ();
        
        entry = 
            new SMIRowImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description,
                numIndexes, 
                lastIndexIsImplied
            );
        
        return (entry);
    }  
    
    @Override
    public SMINode          getChildById (int id) {
        if (entry != null && id == entry.getOid ().getLast ())
            return (entry);
        
        return (null);
    }

    @Override
    public Integer []       getChildIds () {
        if (entry == null)
            return (NO_IDS);
        
        return (new Integer [] { entry.getOid ().getLast () });
    }
    
    
}
