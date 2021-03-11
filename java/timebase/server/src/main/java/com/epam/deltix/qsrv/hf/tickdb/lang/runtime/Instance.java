package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.*;

/**
 *  Variable holding an object value.
 */
public final class Instance {
    private static final int    NULL = -1;
    
    private byte []             buffer = null;
    private int                 length = NULL;

      
    public Instance () {
    }
            
    private void                setLength (int n) {            
        int     blength = (buffer == null) ? 0 : buffer.length;                

        if (blength < n) {
            blength = Util.doubleUntilAtLeast (blength < 64 ? 64 : blength, n);
            buffer = new byte [blength];
        }
        
        length = n;
    }
    
    public int                  length () {
        return (length);
    }
    
    public boolean              isNull () {
        return (length == NULL);
    }
    
    public byte []              bytes () {
        return (buffer);
    }
    
    public void                 set (Instance other) {
        if (other == null)
            length = NULL;
        else {
            setLength (other.length);
            System.arraycopy (other.buffer, 0, buffer, 0, length);
        }
    }
    
    public void                 decode (MemoryDataInput mdi) {
        if (mdi.readBoolean ())  // is null
            length = NULL;
        else {
            setLength (mdi.readPackedUnsignedInt ());
            mdi.readFully (buffer, 0, length);
        }
    }
    
    public void                 encode (MemoryDataOutput out) {
        boolean     isNull = length == NULL;
        
        out.writeBoolean (isNull);
        
        if (!isNull) {
            out.writePackedUnsignedInt (length);            
            out.write (buffer, 0, length);
        }
    }
}
