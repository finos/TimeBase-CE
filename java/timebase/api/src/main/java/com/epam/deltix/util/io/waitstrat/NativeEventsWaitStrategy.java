package com.epam.deltix.util.io.waitstrat;

/**
 *
 */
public class NativeEventsWaitStrategy implements WaitStrategy {
    private InterprocessLock    lock;

    public NativeEventsWaitStrategy(String name) {
        lock = new InterprocessLock(name);
    }

    public void                 waitSignal() {
        lock.waitFor();
    }

    public void                 signal() {
        lock.signal();
    }

    public void                 close() {
        lock.close();
    }
}
