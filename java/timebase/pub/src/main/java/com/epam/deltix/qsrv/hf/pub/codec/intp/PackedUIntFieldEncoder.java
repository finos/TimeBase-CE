package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.util.text.CharSequenceParser;

/**
 *
 */
class PackedUIntFieldEncoder extends IntegerFieldEncoder {
    public PackedUIntFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    void                    writeNull(EncodingContext ctxt) {
        setLong(IntegerDataType.PUINT30_NULL, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == IntegerDataType.PUINT30_NULL; 
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        setLong (CharSequenceParser.parseInt (value), ctxt);
    }

    @Override
    void                    setLongImpl (long value, EncodingContext ctxt) {
        CodecUtils.writePackedUnsignedInt((int) value, ctxt.out);
    }    
}
