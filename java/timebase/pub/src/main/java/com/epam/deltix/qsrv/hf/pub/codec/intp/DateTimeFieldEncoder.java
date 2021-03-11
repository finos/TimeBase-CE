package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.util.time.GMT;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

/**
 *
 */
class DateTimeFieldEncoder extends FieldEncoder {
    DateTimeFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final long t = getter.getLong(obj);
        setLong (t, ctxt);
    }

    void                    writeNull(EncodingContext ctxt) {
        setLong(DateTimeDataType.NULL, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == DateTimeDataType.NULL; 
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        long        t;
        
        try {
            t = GMT.parseDateTimeMillis (value.toString()).getTime ();
            setLong (t, ctxt);
        } catch (ParseException x) {
            throwConstraintViolationException(value);
        }
    }

    @Override
    void                    setLong (long value, EncodingContext ctxt) {
        if (!isNullable && value == DateTimeDataType.NULL)
            throwNotNullableException();
        else
            ctxt.out.writeLong(value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        final long value = getter.getLong(message);
        return isNull(value);
    }
}
