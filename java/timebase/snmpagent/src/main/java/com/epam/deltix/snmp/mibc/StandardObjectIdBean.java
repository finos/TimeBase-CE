package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.smi.SMIOID;

/**
 *
 */
public final class StandardObjectIdBean extends StandardObjectBean {

    public StandardObjectIdBean (String id, SMIOID oid) {
        super (id, oid, null);
    }
    
    public StandardObjectIdBean (String id, int ... ids) {
        this (id, new SMIOID (ids));
    }
    
    public StandardObjectIdBean (String id, SMIOID prefix, int suffix) {
        this (id, new SMIOID (prefix, suffix));
    }        
}
