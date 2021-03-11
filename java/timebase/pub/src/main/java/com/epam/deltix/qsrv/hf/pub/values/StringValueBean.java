package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  Holds a VARCHAR value.
 */
public final class StringValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private final StringBuilder value = new StringBuilder ();
    private boolean             isNull;
    private final boolean       isNullable;

    public StringValueBean (VarcharDataType type) {
        super (type);
        isNullable = type.isNullable ();
        isNull = isNullable;
    }

    public CharSequence getRaw () {
        return (isNull ? null : value);
    }

    @Override
    public String       getString () throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;

        return (value.toString ());
    }

    @Override
    public boolean      isNull () {
        return (isNull);
    }

    @Override
    public void         writeNull () {
        isNull = true;
    }

    @Override
    public void         writeString (CharSequence s) {
        value.setLength (0);
        value.append (s);
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull ? null : value.toString ());
    } 
}
