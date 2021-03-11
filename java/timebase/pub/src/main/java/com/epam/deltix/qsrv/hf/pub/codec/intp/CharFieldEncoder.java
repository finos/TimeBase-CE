package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class CharFieldEncoder extends FieldEncoder {
    CharFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final char v = getter.getChar(obj);
        if (!isNullable && v == CharDataType.NULL)
            throwNotNullableException();

        ctxt.out.writeChar(v);
    }

    void                    writeNull(EncodingContext ctxt) {
        ctxt.out.writeChar (CharDataType.NULL);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        if (value == null || value.length() != 1)
            throw new IllegalArgumentException(String.valueOf(value));

        setChar(value.charAt(0), ctxt);
    }

    @Override
    void                    setChar (char value, EncodingContext ctxt) {
        if (!isNullable && value == CharDataType.NULL)
            throwNotNullableException();

        ctxt.out.writeChar (value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.getChar(message) == CharDataType.NULL;
    }
}
