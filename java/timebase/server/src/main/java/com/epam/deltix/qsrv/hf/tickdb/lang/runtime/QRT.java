package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public abstract class QRT {
    public static byte      bpos (boolean b) {
        return (b ? (byte) 1 : 0);
    }
    
    public static byte      bneg (boolean b) {
        return (b ? 0 : (byte) 1);
    }
    
    public static byte      unnull (byte b) {
        return (bpos (b == 1));
    }
    
    public static byte      beq (byte a, byte b) {
        return (a == b ? (byte) 1 : 0);
    }

    public static byte      bneq (byte a, byte b) {
        return (a != b ? (byte) 1 : 0);
    }

    public static byte      band (byte a, byte b) {
        return ((byte) (unnull (a) & unnull (b)));
    }

    public static byte      bor (byte a, byte b) {
        return ((byte) (unnull (a) | unnull (b)));
    }

    public static byte      bnot (byte a) {
        return ((byte) (1 - unnull (a)));
    }

    public static byte      seq (CharSequence a, CharSequence b) {
        return (bpos (Util.equals (a, b)));
    }

    public static byte      sneq (CharSequence a, CharSequence b) {
        return (bneg (Util.equals (a, b)));
    }

    public static byte      slt (CharSequence a, CharSequence b) {
        return (bpos (Util.compare (a, b, false) < 0));
    }

    public static byte      sle (CharSequence a, CharSequence b) {
        return (bpos (Util.compare (a, b, false) <= 0));
    }

    public static byte      sgt (CharSequence a, CharSequence b) {
        return (bpos (Util.compare (a, b, false) > 0));
    }

    public static byte      sge (CharSequence a, CharSequence b) {
        return (bpos (Util.compare (a, b, false) >= 0));
    }

    public static byte      slike (CharSequence a, CharSequence b) {
        return (bpos(StringUtils.wildcardMatch(a, b, false)));
    }

    public static byte      snlike (CharSequence a, CharSequence b) {
        return (bpos(!StringUtils.wildcardMatch(a, b, false)));
    }
}
