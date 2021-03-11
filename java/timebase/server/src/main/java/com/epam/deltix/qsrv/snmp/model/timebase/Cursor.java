package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.*;

/**
 *
 */
@Description ("Information about an open cursor")
public interface Cursor {
    @Id(1) @Index()
    @Description ("Cursor identifier")
    public int                  getCursorId();

    @Id(2)
    @Description ("The timestamp of the last message returned by this cursor")
    public String               getCursorLastMessageTime();
}
