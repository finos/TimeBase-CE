package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;

/**
 *
 */
public interface PQExecutor {

    InstrumentMessageSource executePreparedQuery (
            PreparedQuery           pq,
            SelectionOptions        options,
            TickStream[]            streams,
            CharSequence[]          ids,
            boolean                 fullScan,
            long                    time,
            Parameter[]             params);
}
