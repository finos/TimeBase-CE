package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class CharFieldDecoder extends FieldDecoder {
    CharFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        final char v1 = getChar(ctxt1);
        assert isNullable || v1 != CharDataType.NULL : getNotNullableMsg();
        final char v2 = getChar(ctxt2);
        assert isNullable || v2 != CharDataType.NULL : getNotNullableMsg();
        return (v1 - v2);
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final char v = getChar(ctxt);
        assert isNullable || v != CharDataType.NULL : getNotNullableMsg();
        setter.setChar(obj, v);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setChar(obj, CharDataType.NULL);
    }

    @Override
    protected void setNull(Object obj, int idx) {
        Array.setChar(obj, idx, CharDataType.NULL);
    }

    @Override
    public char     getChar (DecodingContext ctxt) {
        return (ctxt.in.readChar ());
    }

    @Override
    int getInt(DecodingContext ctxt) {
        return (ctxt.in.readChar ());
    }

    @Override
    long getLong(DecodingContext ctxt) {
        return (ctxt.in.readChar ());
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final char v = getChar(ctxt);
        return (v == CharDataType.NULL) ? null : String.valueOf(v);
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (2);
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        return ctxt.in.readChar() == CharDataType.NULL;
    }

    @Override
    public boolean isNull(long value) {
        return value == CharDataType.NULL;
    }
}
