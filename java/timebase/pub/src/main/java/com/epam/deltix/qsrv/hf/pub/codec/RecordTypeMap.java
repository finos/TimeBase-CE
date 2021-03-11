package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *
 */
public final class RecordTypeMap<T> {
    private final T []                      lookup;
    private final StringBuilder             names = new StringBuilder();
    
    public RecordTypeMap (T [] types) {
        final int       num = types.length;
        
        if (num > 256)
            throw new IllegalArgumentException (
                "Too many classes: " + types.length + " (max 256)"
            );
        
        this.lookup = types;

        // dump names
        names.append("[");
        for (int i = 0; i < num; i++)
            names.append(i > 0 ? ", " : "").append(lookup[i]);

        names.append("]");
    }

    public T[]         getTypes() {
        return lookup;
    }

    private static String           getName (Object mtype) {
        return String.valueOf(mtype);
    }
    
    public final int                getCode (final T mtype) {
        //  Inline first few iterations
        try {
            if (mtype.equals(lookup [0]))
                return (0);
            
            if (mtype.equals(lookup [1]))
                return (1);
            
            if (mtype.equals(lookup [2]))
                return (2);
            
            if (mtype.equals(lookup [3]))
                return (3);
            
            if (mtype.equals(lookup [4]))
                return (4);
            
            if (mtype.equals(lookup [5]))
                return (5);
            
            if (mtype.equals(lookup [6]))
                return (6);
            
            for (int code = 7; ; code++) {
                if (mtype.equals(lookup [code]))
                    return (code);
            }
        } catch (ArrayIndexOutOfBoundsException x) {
            throw new IllegalArgumentException (
                "Message type [" + getName (mtype) + "] is not allowed. Expected: " + names
            );
        }
    }
}
