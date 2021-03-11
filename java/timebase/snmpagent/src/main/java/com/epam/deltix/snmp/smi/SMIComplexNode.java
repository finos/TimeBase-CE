package com.epam.deltix.snmp.smi;

/**
 *
 */
public interface SMIComplexNode extends SMINode {
    public SMINode              getChildById (int id);

    public Integer []           getChildIds ();
}
