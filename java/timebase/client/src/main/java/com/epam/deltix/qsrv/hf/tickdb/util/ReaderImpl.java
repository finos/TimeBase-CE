package com.epam.deltix.qsrv.hf.tickdb.util;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;

import java.io.IOException;

/**
 *
 */
public class ReaderImpl implements Reader {

    private TickCursor cursor;
    private long                endTime;

    public ReaderImpl(final TickCursor cursor, final long endTime) {
        if (cursor == null)
            throw new IllegalArgumentException("TickCursor is null");

        this.cursor = cursor;
        this.endTime = endTime;
    }

    @Override
    public boolean next() {
        boolean has_next = cursor.next();
        if (has_next && cursor.getMessage().getTimeStampMs() > endTime)
            return false;

        return has_next;
    }

    @Override
    public Object getMessage() {
        return cursor.getMessage();
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }
}
