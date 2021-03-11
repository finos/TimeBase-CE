package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;

/**
 *
 */
final class ScaledDoubleFieldDecoder extends DoubleFieldDecoder {
    ScaledDoubleFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public double           readDouble (DecodingContext ctxt) {
        return (ctxt.in.readScaledDouble ());
    }

    @Override
    public void             skip (DecodingContext ctxt) {
        if (baseIdx >= 0)
            ctxt.doubleBaseValues[baseIdx] = readDouble(ctxt);
        else
            ctxt.in.readScaledDouble();
    }
}
