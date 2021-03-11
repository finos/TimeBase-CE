package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

import java.util.Date;

/**
 *
 */
public interface ChannelStats {
    public long             getTotalNumMessages ();

    public long             getLastMessageTimestamp ();

    public long             getLastMessageSysTime ();

    public Date             getLastMessageDate ();

    public Date             getLastMessageSysDate ();
}
