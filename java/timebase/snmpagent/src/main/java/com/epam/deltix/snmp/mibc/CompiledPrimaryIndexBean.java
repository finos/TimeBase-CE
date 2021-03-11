package com.epam.deltix.snmp.mibc;

/**
 *
 */
class CompiledPrimaryIndexBean implements CompiledPrimaryIndexInfo {
    private final boolean           isLastImplied;
    private final CompiledObject [] indexes;

    public CompiledPrimaryIndexBean (
        boolean                     isLastImplied,
        CompiledObject []           indexes
    )
    {
        this.isLastImplied = isLastImplied;
        this.indexes = indexes;
    }
    
    @Override
    public CompiledObject []        getIndexedChildren () {
        return (indexes);
    }

    @Override
    public boolean                  isLastImplied () {
        return (isLastImplied);
    }
    
}
