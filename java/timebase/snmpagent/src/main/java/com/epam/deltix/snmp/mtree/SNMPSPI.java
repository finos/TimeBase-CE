package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.SMIOID;

/**
 *
 */
public interface SNMPSPI {
    public interface Handler {
        public void set (SMIOID oid, Object value);
    }
    
    public Object           get (SMIOID oid, SMIOID index);
    
    public void             walk (SMIOID root, Handler handler);
}
