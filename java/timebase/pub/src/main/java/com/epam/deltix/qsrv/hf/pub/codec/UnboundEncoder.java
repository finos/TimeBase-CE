package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.WritableValue;

/**
 *
 */
public interface UnboundEncoder extends WritableValue {
    public boolean              nextField ();
    
    public NonStaticFieldInfo   getField ();

    /**
     * Checks that NOT NULLABLE restriction was not violated (Optional).
     */
    public void endWrite();
}
