package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *  Nullable CharSequence.
 */
public final class Varchar {
    private CharSequence            value = null;        
    private RuntimeBuilder sb = null;

    private final static class      RuntimeBuilder implements CharSequence {

        StringBuilder           value;

        public RuntimeBuilder() {
            this.value = new StringBuilder();
        }

        public RuntimeBuilder(CharSequence arg) {
            this.value = new StringBuilder(arg);
        }

        @Override
        public int              length() {
            return value.length();
        }

        public void             setLength(int length) {
            value.setLength(length);
        }

        @Override
        public char             charAt(int index) {
            return value.charAt(index);
        }

        public void             append(CharSequence arg) {
            value.append(arg);
        }

        @Override
        public CharSequence     subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public boolean          equals(Object obj) {
            if (obj instanceof Varchar)
                return Util.equals(value, ((Varchar)obj).value);

            if (obj instanceof CharSequence)
                return Util.equals(value, (CharSequence)obj);

            return super.equals(obj);
        }

        @Override
        public int              hashCode() {
            return Util.hashCode(value);
        }
    }
            
    public CharSequence             get () {
        return (value);
    }
    
    public void                     set (CharSequence arg) {        
        if (arg instanceof String)
            set ((String) arg);
        else if (arg == null) 
            value = null;
        else {
            if (sb == null)
                sb = new RuntimeBuilder (arg);
            else {
                sb.setLength (0);
                sb.append (arg);
            }
            
            value = sb;
        }
    }
    
    public void                     set (String arg) {
        value = arg;
    }
    
    public void                     readAlphanumeric (
        MemoryDataInput                 in, 
        int                             numSizeBits, 
        int                             n
    )
    {
        if (sb == null)
            sb = new RuntimeBuilder();

        if (AlphanumericCodec.staticRead (in, numSizeBits, n, sb.value) != null)
            this.value = sb;
        else
            this.value = null;
    }

    @Override
    public boolean                  equals(Object obj) {
        if (obj instanceof Varchar)
            return Util.equals(value, ((Varchar)obj).value);

        if (obj instanceof CharSequence)
            return Util.equals(value, (CharSequence)obj);

        return super.equals(obj);
    }

    @Override
    public int                      hashCode() {
        return Util.hashCode(value);
    }
}
