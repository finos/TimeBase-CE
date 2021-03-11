package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.FwdStringCodec;

/**
 *
 */
class FwdStringFieldEncoder extends StringFieldEncoder {
    FwdStringFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    void            setString (CharSequence value, EncodingContext ctxt) {
        FwdStringCodec.write(value, ctxt.out);
    }
}
