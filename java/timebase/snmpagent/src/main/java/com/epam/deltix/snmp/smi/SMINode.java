package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMINode {
    public SMIComplexNode   getParent ();
    
    public int              getId ();
    
    public SMIOID           getOid ();

    public String           getName ();
    
    public String           getDescription ();
}
