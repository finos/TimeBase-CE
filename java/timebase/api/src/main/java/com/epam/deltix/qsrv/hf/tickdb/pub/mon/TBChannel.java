package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

import java.util.Date;

/**
 *
 */
public interface TBChannel extends ChannelStats {
    public InstrumentChannelStats []    getInstrumentStats ();

    public long                         getOpenTime ();

    public long                         getCloseTime ();

    public Date                         getOpenDate ();

    public Date                         getCloseDate ();
}
