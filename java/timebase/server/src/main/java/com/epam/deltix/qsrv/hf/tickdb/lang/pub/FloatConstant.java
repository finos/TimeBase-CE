package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.lang.Util;

/**
 *
 */
public final class FloatConstant extends Constant {
    public final long               significand;
    public final int                exponent;

    public FloatConstant (long location, long significand, int exponent) {
        super (location);
        this.significand = significand;
        this.exponent = exponent;
    }

    public FloatConstant (long significand, int exponent) {
        this (NO_LOCATION, significand, exponent);
    }

    public static FloatConstant     parseText (CharSequence text) {
        return (parseText (NO_LOCATION, text));
    }

    public static FloatConstant     parseText (long location, CharSequence text) {
        long    s = 0;
        int     eadd = 0;
        int     e = 0;
        boolean dot = false;
        boolean exp = false;
        boolean expIsNegative = false;
        int     len = text.length ();

        for (int ii = 0; ii < len; ii++) {
            char    c = text.charAt (ii);

            if (!exp && c == '.')
                dot = true;
            else if (!exp && (c == 'e' || c == 'E')) {
                exp = true;

                c = text.charAt (ii + 1);

                if (c == '-') {
                    ii++;
                    expIsNegative = true;
                }
                else if (c == '+')
                    ii++;
            }
            else if (c >= '0' && c <= '9') {
                int     d = c - '0';

                if (exp)
                    e = e * 10 + d;
                else if (dot && s == 0 && d == 0)   // 0.000000xxx
                    eadd--;
                else {
                    s = s * 10 + d;
                    
                    if (dot)
                        eadd--;
                }
            }
            else
                throw new IllegalArgumentException (text.toString ());
        }

        if (expIsNegative)
            e = -e;

        e += eadd;

        if (s == 0) // small correction for things such as .0
            e = 0;
        else        //  strip trailing zeroes
            while (s % 10 == 0) {
                s /= 10;
                e++;
            }

        return (new FloatConstant (location, s, e));
    }

    public double       toDouble () {
        return (Double.parseDouble (toFloatString ()));
    }

    public String       toFloatString () {
        if (exponent == 0)
            return (String.valueOf (significand));
        else
            return (significand + "E" + exponent);
    }
    
    @Override
    protected void      print (int outerPriority, StringBuilder s) {
        s.append (toFloatString ());
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            significand == ((FloatConstant) obj).significand &&
            exponent == ((FloatConstant) obj).exponent
        );
    }

    @Override
    public int                      hashCode () {
        return (
            (super.hashCode () * 41 + Util.hashCode (significand)) * 31 + exponent
        );
    }
}
