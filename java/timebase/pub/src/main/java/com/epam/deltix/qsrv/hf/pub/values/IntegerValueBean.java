package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  Holds an INTEGER value.
 */
public final class IntegerValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private long            value;
    private final long      nullValue;
    private final long      min;
    private final long      max;
    private final boolean   isNullable;

    public IntegerValueBean (IntegerDataType type) {
        super (type);
        nullValue = type.getNullValue ();
        isNullable = type.isNullable ();
        Number []   range = type.getRange ();
        min = range [0].longValue ();
        max = range [1].longValue ();

        if (min > max)
            throw new IllegalArgumentException ("min > max");

        value =
            isNullable ?
                nullValue :
            min <= 0 && 0 <= max ?
                0 :
                min;   // Set to something legal.
    }

    public long         getRaw () {
        return (value);
    }

    @Override
    public long         getLong () throws NullValueException {
        if (value == nullValue)
            throw NullValueException.INSTANCE;

        return (value);
    }

    @Override
    public int          getInt () throws NullValueException {
        long                lv = getLong ();
        
        if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE)
            throw new IllegalStateException (lv + " cannot be converted to int");

        return ((int) lv);
    }

    @Override
    public void         writeLong (long value) {
        if (value == nullValue) {
            if (!isNullable)
                throw new IllegalArgumentException ("NULL");
        }
        else {
            if (value < min)
                throw new IllegalArgumentException (value + " < " + min);

            if (value > max)
                throw new IllegalArgumentException (value + " > " + max);
        }

        this.value = value;
    }

    @Override
    public void         writeInt (int value) {
        writeLong (value);
    }
    
    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getLong ()));
    }

    @Override
    public boolean      isNull () {
        return (value == nullValue);
    }

    @Override
    public void         writeNull () {
        writeLong (nullValue);
    }

    @Override
    public void         writeString (CharSequence s) {
        writeLong (DataType.parseLong (s.toString ()));
    }

    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : value);
    }        
}
