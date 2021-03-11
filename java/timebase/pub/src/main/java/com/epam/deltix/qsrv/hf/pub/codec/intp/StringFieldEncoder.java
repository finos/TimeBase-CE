package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.BinaryUtils;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.collections.generated.ByteArrayList;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class StringFieldEncoder extends FieldEncoder {
    StringFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Object fieldValue = getter.get(obj);
        if (!isNullable && fieldValue == null)
            throwNotNullableException();

        CharSequence      s;
        
        try {
            if (fieldValue instanceof ByteArrayList)
                s = BinaryUtils.toStringBuilder((ByteArrayList) fieldValue);
            else
                s = (CharSequence) fieldValue;
        } catch (ClassCastException cx) {
            throw new ClassCastException (
                fieldDescription + ": " + fieldValue.getClass().getName() + " cannot be cast to CharSequence"
            );
        }

        setString (s, ctxt);
    }

    void                    writeNull(EncodingContext ctxt) {
        setString(null, ctxt);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        ctxt.out.writeString (value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.get(message) == null;
    }
}
