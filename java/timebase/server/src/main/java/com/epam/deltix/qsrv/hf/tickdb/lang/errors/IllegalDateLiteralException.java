package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalDateLiteralException extends CompilationException {
    public enum Field {
        FORMAT,
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        FRACTION,
        TIMEZONE
    }
    
    public final Field      field;
    
    public IllegalDateLiteralException (
        long                location,
        String              text,
        Field               field
    )
    {
        super ("Illegal " + field + " in date literal '" + text + "'", location);
        
        this.field = field;
    }
}
