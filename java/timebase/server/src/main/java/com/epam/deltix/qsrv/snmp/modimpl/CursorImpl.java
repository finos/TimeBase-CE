package com.epam.deltix.qsrv.snmp.modimpl;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBCursor;
import com.epam.deltix.qsrv.snmp.model.timebase.Cursor;

/**
 *
 */
public class CursorImpl implements Cursor {
    private int id;
    private TBCursor cursor;

    public CursorImpl(TBCursor cursor) {
        this.id = (int) cursor.getId();
        this.cursor = cursor;
    }

    @Override
    public int getCursorId() {
        return id;
    }

    @Override
    public String getCursorLastMessageTime() {
        return SnmpUtil.formatDateTimeMillis(cursor.getLastMessageSysTime());
    }
}
