package com.epam.deltix.util.io.offheap;

import com.epam.deltix.util.collections.OffHeapByteQueue;
import com.epam.deltix.util.io.PollingThread;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.waitstrat.ParkWaitStrategy;
import com.epam.deltix.util.io.waitstrat.WaitStrategy;
import com.epam.deltix.util.lang.Pollable;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class OffHeapPollableInputStream extends InputStream implements Pollable {

    static final PollingThread          POLLER;

    static {
        synchronized (OffHeapPollableInputStream.class) {
            (POLLER = new PollingThread(new BusySpinIdleStrategy())).start();
        }
    }

    private OffHeapByteQueue            queue;
    private WaitStrategy                waitStrategy;

    OffHeapPollableInputStream(OffHeapByteQueue queue) throws IOException {
        this(queue, new ParkWaitStrategy());
    }

    OffHeapPollableInputStream(OffHeapByteQueue queue, WaitStrategy waitStrategy) {
        this.queue = queue;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public void                         poll() {
        if (queue.getTailSequence().changed())
            waitStrategy.signal();
    }

    @Override
    public int                          read() throws IOException {
        int res;
        while ((res = queue.poll()) < 0) {
            waitUnchecked();
        }

        return res;
    }

    @Override
    public int                          read(byte b[], int off, int len)
            throws IOException {

        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int count;
        while ((count = queue.poll(b, off, len)) <= 0) {
            waitUnchecked();
        }

        return count;
    }

    private void                        waitUnchecked() throws IOException {
        try {
            waitStrategy.waitSignal();
        } catch (InterruptedException e) {
            //ignore
        }
    }

    @Override
    public void                         close() {
        POLLER.remove(this);
    }
}
