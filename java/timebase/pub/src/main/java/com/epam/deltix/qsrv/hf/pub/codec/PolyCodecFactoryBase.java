package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.lang.Factory;

/**
 *
 */
abstract class PolyCodecFactoryBase <C, F>
    implements Factory <C>
{
    protected final Factory <F> []          fixedFactories;

    public PolyCodecFactoryBase (Factory <F> [] fixedFactories) {
        this.fixedFactories = fixedFactories;
    }
}
