package com.epam.deltix.snmp.smi;

/**
 *
 */
class SMIAugmentedIndexImpl implements SMIAugmentedIndexInfo {
    private SMIRow              augmentedRow;

    SMIAugmentedIndexImpl (SMIRow augmentedRow) {
        this.augmentedRow = augmentedRow;
    }
        
    @Override
    public SMIRow               getAugmentedRow () {
        return (augmentedRow);
    }    
}
