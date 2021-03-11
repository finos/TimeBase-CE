package com.epam.deltix.qsrv.hf.pub.util;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.lang.Disposable;

import java.io.EOFException;

public class LiveCursorWatcher extends Thread implements Disposable {
    protected final TickCursor        cursor;
    private final MessageListener     listener;

    private final Object        lock = new Object();
    private volatile boolean    stopped = false;

    public LiveCursorWatcher(TickCursor cursor, MessageListener listener) {
        this(cursor, listener, true);
    }

    public LiveCursorWatcher(TickCursor cursor, MessageListener listener, boolean startThread) {
        super("Live Cursor Watcher for " + cursor);
        setDaemon(true);

        if (listener == null)
            throw new IllegalArgumentException();

        this.cursor = cursor;
        this.listener = listener;
        this.cursor.setAvailabilityListener(new Runnable() {
            @Override
            public void run() {
                notifyDataAvailable();
            }
        });

        if (startThread)
            start();
    }

    private void        notifyDataAvailable() {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void         run() {
        for (;;) {
            synchronized (lock) {
                try {
                    if (cursor.next())
                        listener.onMessage(cursor.getMessage());
                    else
                        break;
                } catch (UnavailableResourceException e) {
                     try {
                        synchronized (lock) {
                            lock.wait ();
                        }
                        if (stopped)
                            return;
                    } catch (InterruptedException ie) {
                         // continue
                    }
                } catch (CursorIsClosedException ie) {
                    return;
                } catch (deltix.util.io.UncheckedIOException ex) {
                    if (ex.getCause() instanceof EOFException)
                        return;
                    else
                        throw ex;
                }
            }
        }
    }

    @Override
    public void interrupt() {
        close();
        super.interrupt();
    }

    @Override
    public void         close() {
        stopped = true;
        notifyDataAvailable();
    }

    public interface MessageListener {
        void onMessage(InstrumentMessage msg);
    }
}
