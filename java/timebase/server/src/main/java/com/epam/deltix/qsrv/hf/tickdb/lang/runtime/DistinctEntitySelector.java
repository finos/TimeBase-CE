package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;

/**
 *  Emits an empty message
 */
public class DistinctEntitySelector extends DelegatingInstrumentMessageSource {
    public DistinctEntitySelector (InstrumentMessageSource source) {
        super (source);
    }


}
