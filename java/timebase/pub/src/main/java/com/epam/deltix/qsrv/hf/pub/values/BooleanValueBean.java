package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Util;

/**
 *  Holds a BOOLEAN value.
 */
public final class BooleanValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private int                 value;
    private final boolean       isNullable;

    public BooleanValueBean (VarcharDataType type) {
        super (type);
        isNullable = type.isNullable ();
        value = isNullable ? BooleanDataType.NULL : BooleanDataType.FALSE;
    }

    public int          getRaw () {
        return (value);
    }

    @Override
    public boolean      getBoolean () throws NullValueException {
        if (value == BooleanDataType.NULL)
            throw NullValueException.INSTANCE;

        return (value == BooleanDataType.TRUE);
    }


    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getBoolean ()));
    }

    @Override
    public boolean      isNull () {
        return (value == BooleanDataType.NULL);
    }

    @Override
    public void         writeNull () {
        value = BooleanDataType.NULL;
    }

    @Override
    public void         writeBoolean (boolean value) {
        this.value = value ? BooleanDataType.TRUE : BooleanDataType.FALSE;
    }

    @Override
    public void         writeString (CharSequence value) {
        writeBoolean (Util.equals (value, "true"));
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : Boolean.valueOf (getBoolean ()));
    }
}
