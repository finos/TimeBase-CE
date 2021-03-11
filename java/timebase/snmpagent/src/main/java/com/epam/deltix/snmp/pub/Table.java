package com.epam.deltix.snmp.pub;

/**
 *
 */
public interface Table <EntryType> extends Iterable <EntryType> {       
    public int                  size ();
    
    public void                 add (EntryType obj);
    
    public void                 remove (EntryType obj);    
}
