package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMITable extends SMIComplexNode {
    public SMIRow               addAugmentingRow (
        int                         id,
        String                      name, 
        String                      description,
        SMIRow                      augmentedRow
    );    
    
    public SMIRow               addIndexedRow (
        int                         id,
        String                      name, 
        String                      description,
        int                         numIndexes,
        boolean                     lastIndexIsImplied
    );    
    
    public SMIRow               getConceptualRow ();   

}
