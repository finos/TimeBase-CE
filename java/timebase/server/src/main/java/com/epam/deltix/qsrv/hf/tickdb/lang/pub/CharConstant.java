package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *  DATE '...' literal.
 */
public final class CharConstant extends Constant {
    public final char             ch;

    public CharConstant (long location, char ch) {
        super (location);
        this.ch = ch;
    }

    protected void      print (int outerPriority, StringBuilder s) {
        if (ch == '\'')
            s.append ("''''C");
        else {
            s.append ('\'');
            s.append (ch);
            s.append ("'C");
        }
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            ch == (((CharConstant) obj).ch)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + ch);
    }
}
