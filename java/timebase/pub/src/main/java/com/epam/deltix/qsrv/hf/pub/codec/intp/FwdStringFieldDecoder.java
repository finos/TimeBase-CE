package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.FwdStringCodec;

/**
 *
 */
class FwdStringFieldDecoder extends StringFieldDecoder {
    FwdStringFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        return ctxt.in.readInt() == FwdStringCodec.NULL;
    }

    @Override
    CharSequence    getCharSequence (DecodingContext ctxt) {
        return FwdStringCodec.read(ctxt.in);        
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (4);
    }
}
