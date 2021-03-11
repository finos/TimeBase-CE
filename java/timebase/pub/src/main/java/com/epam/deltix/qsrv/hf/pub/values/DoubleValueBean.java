package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  Holds a float value.
 */
public final class DoubleValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private double          value;
    private final double    min;
    private final double    max;
    private final boolean   isNullable;

    private static double   nvl (Number n, double def) {
        return (n == null ? def : n.doubleValue ());
    }

    public DoubleValueBean (FloatDataType type) {
        super (type);
        isNullable = type.isNullable ();
        min = nvl (type.getMin (), Double.MIN_VALUE);
        max = nvl (type.getMax (), Double.MAX_VALUE);

        if (min > max)
            throw new IllegalArgumentException ("min > max");

        value = 
            isNullable ?
                Double.NaN :
            min <= 0 && 0 <= max ?
                0 :
                min;   // Set to something legal.
    }

    public double       getRaw () {
        return (value);
    }

    @Override
    public double       getDouble () throws NullValueException {
        if (Double.isNaN (value))
            throw NullValueException.INSTANCE;

        return (value);
    }

    @Override
    public float        getFloat () throws NullValueException {
        double              dv = getDouble ();

        if (dv < Float.MIN_VALUE || dv > Float.MAX_VALUE)
            throw new IllegalStateException (dv + " cannot be converted to float");

        return ((float) dv);
    }

    @Override
    public void         writeDouble (double value) {
        if (Double.isNaN (value)) {
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
    public void         writeFloat (float value) {
        writeDouble (value);
    }

    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getFloat ()));
    }

    @Override
    public boolean      isNull () {
        return (Double.isNaN (value));
    }

    @Override
    public void         writeNull () {
        writeDouble (Double.NaN);
    }

    @Override
    public void         writeString (CharSequence s) {
        writeDouble (DataType.parseDouble (s.toString ()));
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : value);
    } 
}
