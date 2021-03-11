package com.epam.deltix.util.io;

import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Pollable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 *
 */
public class PollingThread extends Thread {
    private final IdleStrategy      idleStrategy;

    private final List<Pollable>    pollableList = new ArrayList<>();
    private volatile boolean        stopped = false;

    public PollingThread() {
        this(new BusySpinIdleStrategy());
    }

    public PollingThread(IdleStrategy idleStrategy) {
        super("POLLING THREAD");
        setDaemon(true);
        this.idleStrategy = idleStrategy;
    }

    public void                 add(Pollable pollable) {
        synchronized (pollableList) {
            pollableList.add(pollable);
        }

        wakeUp();
    }

    public void                 remove(Pollable pollable) {
        synchronized (pollableList) {
            pollableList.remove(pollable);
        }

        wakeUp();
    }

    @Override
    public void                 run() {
        while (!stopped) {
            int size = 0;
            synchronized (pollableList) {
                size = pollableList.size();
            }

            if (size == 0) {
                LockSupport.park();

                if (stopped)
                    break;
            }

            synchronized (pollableList) {
                for (int i = 0; i < pollableList.size(); ++i)
                    pollableList.get(i).poll();
            }

            idleStrategy.idle();
        }
    }

    private void                wakeUp() {
        LockSupport.unpark(this);
    }

    public void                 shutdown() {
        synchronized (pollableList) {
            pollableList.clear();
        }
        stopped = true;

        wakeUp();
    }
}
