package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public class FieldIdentifier extends Element {
    public final String                 fieldName;

    public FieldIdentifier (long location, String fieldName) {
        super (location);
        this.fieldName = fieldName;
    }

    @Override
    public void                         print (StringBuilder s) {
        GrammarUtil.escapeVarId (fieldName, s);
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            fieldName.equals (((FieldIdentifier) obj).fieldName)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + fieldName.hashCode ());
    }
}
