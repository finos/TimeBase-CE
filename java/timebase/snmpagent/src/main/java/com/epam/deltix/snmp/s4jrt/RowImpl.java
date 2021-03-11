package com.epam.deltix.snmp.s4jrt;

import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

/**
 *
 */
public final class RowImpl <EntryType> implements MOTableRow {
    final EntrySupport <EntryType>              support;
    final OID                                   index;
    final EntryType                             entry;
    
    public RowImpl (
        EntrySupport <EntryType>                support,
        EntryType                               entry
    )
    {
        this.support = support;
        this.index = support.getIndex (entry);
        this.entry = entry;
    }        

    @Override
    public MOTableRow               getBaseRow () {
        return (null);
    }

    @Override
    public OID                      getIndex () {
        return (index);
    }

    @Override
    public Variable                 getValue (int column) {
        return (support.getValue (entry, column));
    }

    @Override
    public void                     setBaseRow (MOTableRow baseRow) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    public int                      size () {   
        return (support.getNumColumns ());
    }
}
