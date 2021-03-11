package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.util.concurrent.Signal;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 *
 */
public class StreamState {
    private final boolean[] properties = new boolean[TickStreamProperties.COUNT];
    private final AtomicReferenceArray<Signal> locks =
            new AtomicReferenceArray<Signal>(TickStreamProperties.COUNT);

    public StreamState(boolean state) {
        this();
        set(state);
    }

    public StreamState() {
        for (int i = 0; i < locks.length(); i++)
            locks.set(i, new Signal());
    }
    
    public void         set(boolean state) {
        synchronized (properties) {
            for (int i = 0; i < properties.length; i++)
                 properties[i] = state;
        }
    }

    public void         reset(int property) {
        synchronized (properties) {
            properties[property] = false;
        }
    }

    public boolean      set(int property) {
        synchronized (properties) {
            if (properties[property])
                return false;
            properties[property] = true;
        }

        return true;
    }

    public void         setNotify(int property) {
        set(property);
        getLock(property).set();
    }

    private Signal       getLock(int property) {
        return locks.get(property);
    }
    
    public void         notifyLocks() {
        for (int i = 0; i < locks.length(); i++)
            locks.get(i).set();
    }

    public  void        wait(int property) throws InterruptedException {
        getLock(property).await();
    }

    public boolean      get(int property) {
        synchronized (properties) {
            return properties[property];
        }
    }

    public void monitor(int property) {
        getLock(property).reset();
    }

}
