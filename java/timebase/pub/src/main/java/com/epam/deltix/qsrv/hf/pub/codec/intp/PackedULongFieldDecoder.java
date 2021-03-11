package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class PackedULongFieldDecoder extends IntegerFieldDecoder {
    public PackedULongFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public long         getLong (DecodingContext ctxt) {
        return CodecUtils.readPackedUnsignedLong(ctxt.in);
    }

    @Override
    public void         skip (DecodingContext ctxt) {
        ctxt.in.readPackedUnsignedLong ();
    }

    @Override
    public boolean isNull(long value) {
        return value == IntegerDataType.PUINT61_NULL; 
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setLong(obj, IntegerDataType.PUINT61_NULL);
    }
}
