package com.epam.deltix.qsrv.hf.pub.util;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;

/**
 * Similar to {@link LiveCursorWatcher} but automatically closes cursor.
 *
 * Usage example:
 * <pre>
 * LiveCursorWatcherEx watcher = LiveCursorWatcherEx.start ("my-stream", new MessageListener () { ... });
 * ...
 * watcher.close();
 * </pre>
 */
public final class LiveCursorWatcherEx extends LiveCursorWatcher {

    public static LiveCursorWatcherEx start (DXTickDB tickDB, String streamKey, MessageListener listener) {
        DXTickStream stream = tickDB.getStream(streamKey);
        TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(false, true));
        LiveCursorWatcherEx watcher = new LiveCursorWatcherEx(cursor, listener);
        watcher.start();
        return watcher;
    }

    private LiveCursorWatcherEx(TickCursor cursor, MessageListener listener) {
        super(cursor, listener, false);
    }

    public void close () {
        super.close();
        cursor.close();
    }
}
