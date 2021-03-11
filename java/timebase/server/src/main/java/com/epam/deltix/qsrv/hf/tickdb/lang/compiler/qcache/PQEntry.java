package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.time.TimeKeeper;

/**
 *
 */
class PQEntry extends QuickList.Entry <PQEntry> {
    final PQKey                         key;
    final PreparedQuery                 query;
    long                                timestamp;
    
    PQEntry (PQKey key, PreparedQuery query) {
        this.key = key;
        this.query = query;
        this.timestamp = TimeKeeper.currentTime;
    }        
}
