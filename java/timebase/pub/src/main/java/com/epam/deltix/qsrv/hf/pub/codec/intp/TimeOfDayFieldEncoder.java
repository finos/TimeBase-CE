package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class TimeOfDayFieldEncoder extends FixedSizeIntegerFieldEncoder {
    TimeOfDayFieldEncoder (NonStaticFieldLayout f) {
        super (f, 4);
    }

    void                    writeNull(EncodingContext ctxt) {
        setLong(TimeOfDayDataType.NULL, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == TimeOfDayDataType.NULL; 
    }

    @Override
    boolean isNull(CharSequence value) {
        return value == null;
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        setInt (TimeOfDayDataType.staticParse (value), ctxt);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.getInt(message) == TimeOfDayDataType.NULL;
    }
}
