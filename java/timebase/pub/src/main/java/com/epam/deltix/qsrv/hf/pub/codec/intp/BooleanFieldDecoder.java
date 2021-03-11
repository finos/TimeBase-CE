package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class BooleanFieldDecoder extends FieldDecoder {
    BooleanFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        final byte v1 = ctxt1.in.readByte ();
        assert isNullable || v1 != BooleanDataType.NULL : getNotNullableMsg();
        final byte v2 = ctxt2.in.readByte ();
        assert isNullable || v2 != BooleanDataType.NULL : getNotNullableMsg();
        return (v1 - v2);
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final byte v = ctxt.in.readByte ();
        assert isNullable || v != BooleanDataType.NULL : getNotNullableMsg();


        if (setter instanceof JavaValueSetterMethod && v == BooleanDataType.NULL)
            setNullValue(obj);
        else if (fieldType == boolean.class) {
            setter.setBoolean(obj, v == BooleanDataType.TRUE);
        } else if (fieldType == byte.class) {
            setter.setByte(obj, v);
        } else
            throw new RuntimeException("Unsupported field type: " + fieldType);
    }

    private void setNullValue(Object obj) throws IllegalAccessException, InvocationTargetException {
        if (fieldType == byte.class) {
            setter.setByte(obj,  BooleanDataType.NULL);
        } else if (fieldType == boolean.class) {
            JavaValueSetterMethod setterMethod = (JavaValueSetterMethod) setter;
            if (setterMethod.hasNullifier())
                setterMethod.nullifyValue(obj);
            else
                throw new UnsupportedOperationException("Can`t write NULLABLE BOOLEAN using setter: \"" + setterMethod.getSetterName() + "()\" that parameter type = boolean.class without NULLIFIER method.");
        } else
            throw new RuntimeException ("Unsupported field type: " + fieldType);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        if (setter instanceof JavaValueSetterMethod) {
            setNullValue(obj);
        } else {
            if (fieldType == boolean.class)
                // #12059 temporarily convert NULL to false
                // throw new IllegalArgumentException ("Field type is non-nullable");
                setter.setBoolean(obj, false);
            else if (fieldType == byte.class)
                setter.setByte(obj, BooleanDataType.NULL);
            else
                throw new RuntimeException("Unsupported field type: " + fieldType);
        }
    }

    @Override
    protected void setNull(Object obj, int idx) {
        Class<?> t = obj.getClass();
        if (t == boolean.class)
            throw new IllegalArgumentException("Field type is non-nullable");
        else if (t == byte.class)
            Array.setByte(obj, idx, BooleanDataType.NULL);
        else
            throw new RuntimeException("Unsupported field type: " + t);
    }

    @Override
    public byte getByte (DecodingContext ctxt) {
        return (ctxt.in.readByte ());
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final byte v = ctxt.in.readByte();
        assert isNullable || v != BooleanDataType.NULL : getNotNullableMsg();
        return v == BooleanDataType.NULL ? null : String.valueOf(v != 0);
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (1);
    }

    public boolean isNull(DecodingContext ctxt) {
        return isNull(ctxt.in.readByte());
    }

    @Override
    public boolean isNull(long value) {
        return value == BooleanDataType.NULL;
    }
}
