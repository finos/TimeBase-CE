package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class TimeOfDayFieldDecoder extends FixedSizeIntegerFieldDecoder {
    TimeOfDayFieldDecoder (NonStaticFieldLayout f) {
        super (f, 4);
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final int v = getInt (ctxt);
        assert isNullable || v != TimeOfDayDataType.NULL : getNotNullableMsg();
        return (v == TimeOfDayDataType.NULL ? null : TimeOfDayDataType.staticFormat (v));
    }

    @Override
    public boolean isNull(long value) {
        return value == TimeOfDayDataType.NULL;
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setInt(obj, TimeOfDayDataType.NULL);
    }
}
