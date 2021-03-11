package com.epam.deltix.snmp.mibc;

/**
 *
 */
class CompiledAugmentedIndexBean implements CompiledAugmentedIndexInfo {
    private final CompiledObject          augmentedObject;

    public CompiledAugmentedIndexBean (CompiledObject augmentedObject) {
        this.augmentedObject = augmentedObject;
    }
    
    @Override
    public CompiledObject getAugmentedObject () {
        return (augmentedObject);
    }
    
}
