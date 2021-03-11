package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;

/**
 *
 */
final class ScaledDoubleFieldEncoder extends DoubleFieldEncoder {
    private final int       precision;
    
    ScaledDoubleFieldEncoder (NonStaticFieldLayout f, int precision) {
        super (f);
        this.precision = precision;
    }

    @Override
    void                    writeDouble (double value, EncodingContext ctxt) {
        if (precision < 0)
            ctxt.out.writeScaledDouble (value);
        else
            ctxt.out.writeScaledDouble (value, precision);
    }
}
