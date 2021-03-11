package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class PackedUIntFieldDecoder extends IntegerFieldDecoder {
    public PackedUIntFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public long         getLong (DecodingContext ctxt) {
        return CodecUtils.readPackedUnsignedInt(ctxt.in);
    }

    @Override
    public void         skip (DecodingContext ctxt) {
        ctxt.in.readPackedUnsignedInt ();
    }

    @Override
    public boolean isNull(long value) {
        return value == IntegerDataType.PUINT30_NULL;
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setValue(obj, IntegerDataType.PUINT30_NULL);
    }
}
