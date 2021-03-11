package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.tickdb.lang.parser.DateParser;

/**
 *  '...'T literal.
 */
public final class TimeConstant extends Constant {
    public final long               nanoseconds;
    public final String             text;

    public TimeConstant (long location, String text) {
        super (location);
        
        this.text = text;
        this.nanoseconds = DateParser.parseTimeLiteral (location, text);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeStringLiteral (text, s);
        s.append ('T');
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            text.equals (((TimeConstant) obj).text)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + text.hashCode ());
    }
}
