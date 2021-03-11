package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.DateParser;


/**
 *  '...'D literal.
 */
public final class DateConstant extends Constant {
    public final long               nanoseconds;
    public final String             text;

    public DateConstant (long location, String text) {
        super (location);
        
        this.text = text;
        this.nanoseconds = DateParser.parseDateLiteral (location, text);
    }

    public DateConstant (String text) {
        this (Location.NONE, text);
    }
    
    protected void      print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeStringLiteral (text, s);
        s.append ('D');
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            text.equals (((DateConstant) obj).text)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + text.hashCode ());
    }
}
