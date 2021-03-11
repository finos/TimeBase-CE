package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class PrimaryIndexInfo extends IndexInfo {
    public final String []          columnIds;
    public boolean                  lastIsImplied;

    public PrimaryIndexInfo (
        long                        location,
        String []                   columnIds, 
        boolean                     lastIsImplied        
    )
    {
        super (location);
        
        this.columnIds = columnIds;
        this.lastIsImplied = lastIsImplied;
    }        
}
