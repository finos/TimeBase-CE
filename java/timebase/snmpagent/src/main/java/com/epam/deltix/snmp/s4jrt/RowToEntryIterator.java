package com.epam.deltix.snmp.s4jrt;

import java.util.Iterator;

/**
 *
 */
public final class RowToEntryIterator <EntryType> 
    implements Iterator <EntryType> 
{
    private final Iterator <RowImpl <EntryType>> rowIter;

    public RowToEntryIterator (Iterator <RowImpl <EntryType>> rowIter) {
        this.rowIter = rowIter;
    }

    @Override
    public boolean          hasNext () {
        return (rowIter.hasNext ());
    }

    @Override
    public EntryType        next () {
        return (rowIter.next ().entry);
    }

    @Override
    public void             remove () {
        rowIter.remove ();
    }        
}
