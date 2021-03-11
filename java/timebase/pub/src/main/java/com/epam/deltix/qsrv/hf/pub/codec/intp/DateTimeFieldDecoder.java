package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.time.GMT;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class DateTimeFieldDecoder extends FieldDecoder {
    DateTimeFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        return (MathUtil.compare (getLong (ctxt1), getLong (ctxt2)));
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        long        t = getLong (ctxt);
        assert isNullable || !isNull(t) : getNotNullableMsg();

        setter.setLong(obj, t);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setLong(obj, Long.MIN_VALUE);
    }

    public boolean isNull(DecodingContext ctxt) {
        return isNull(ctxt.in.readLong());
    }

    @Override
    public long     getLong (DecodingContext ctxt) {
        return (ctxt.in.readLong ());
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final long v = getLong(ctxt);
        assert isNullable || !isNull(v) : getNotNullableMsg();
        return (isNull(v) ? null : GMT.formatDateTimeMillis(v));
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (8);
    }

    @Override
    public boolean isNull(long value) {
        return value == DateTimeDataType.NULL;
    }
}
