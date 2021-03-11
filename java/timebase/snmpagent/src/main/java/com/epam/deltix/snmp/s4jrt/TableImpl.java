package com.epam.deltix.snmp.s4jrt;

import com.epam.deltix.snmp.pub.*;
import java.util.Iterator;
import org.snmp4j.agent.mo.*;

/**
 *
 */
public class TableImpl <EntryType> implements Table <EntryType> {
    private final EntrySupport <EntryType>                          support;
    public final DefaultMOMutableTableModel <RowImpl <EntryType>>   model
        = new DefaultMOMutableTableModel <RowImpl <EntryType>> ();
    
    public TableImpl (EntrySupport <EntryType> support) {
        this.support = support;
    }
    //
    //  IMPLEMENT Table <EntryType>
    //
    @Override
    public void                     add (EntryType entry) {
        model.addRow (new RowImpl <EntryType> (support, entry));
    }

    @Override
    public void                     remove (EntryType obj) {
        model.removeRow (support.getIndex (obj));
    }

    @Override
    public int                      size () {
        return (model.getRowCount ());
    }

    @Override
    public Iterator <EntryType>     iterator () {
        return (new RowToEntryIterator <EntryType> (model.iterator ()));
    }        
}
