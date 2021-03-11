package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class AugmentedIndexInfo extends IndexInfo {
    public final String             refId;

    public AugmentedIndexInfo (long location, String refId) {
        super (location);
        
        this.refId = refId;
    }
    
}
