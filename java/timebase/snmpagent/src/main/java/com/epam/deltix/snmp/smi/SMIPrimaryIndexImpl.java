package com.epam.deltix.snmp.smi;

/**
 *
 */
class SMIPrimaryIndexImpl implements SMIPrimaryIndexInfo {
    final SMIPrimitive []       indexedChildren;
    private final boolean       isLastImplied;

    SMIPrimaryIndexImpl (int depth, boolean isLastImplied) {
        this.indexedChildren = new SMIPrimitive [depth];
        this.isLastImplied = isLastImplied;
    }        
    
    @Override
    public SMIPrimitive []      getIndexedChildren () {
        return (indexedChildren);
    }

    @Override
    public boolean              isLastImplied () {
        return (isLastImplied);
    }    
}
