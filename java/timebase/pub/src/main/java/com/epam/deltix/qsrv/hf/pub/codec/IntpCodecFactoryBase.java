package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.util.lang.Factory;

/**
 *
 */
abstract class IntpCodecFactoryBase <C>
    implements Factory <C>
{
    protected final RecordLayout    layout;

    public IntpCodecFactoryBase (RecordClassDescriptor cd) {
        this.layout = new RecordLayout (cd);
    }

    public IntpCodecFactoryBase (
        TypeLoader              loader,
        RecordClassDescriptor   cd
    )
    {
        this.layout = new RecordLayout (loader, cd);
    }
}
