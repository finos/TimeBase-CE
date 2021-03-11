package com.epam.deltix.qsrv.dtb.store.impl;

/**
 *
 */
final class TSIntermediateFolder extends TSFolder {
    TSIntermediateFolder (
        TSFolder                        parent,
        long                            version,
        int                             id,
        long                            startTS
    )
    {
        super (parent, id, startTS);
        this.version = version;
    }          
}
