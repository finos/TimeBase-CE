package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Disposable;

/**
 *
 */
public class InterprocessLock implements Disposable {

//    static {
//        System.load(Home.getFile("bin").getAbsolutePath() +
//                    "/interproc" +
//                    System.getProperty("os.arch") + ".dll");
//    }

    private long                    handle;

    /**
     * Named system event.
     * Create or open system event.
     * @param name - name of event.
     */
    public InterprocessLock(String name) {
        handle = init0(name, true);
    }

    /**
     * Wait for 'set' state.
     * Than event auto-resets.
     */
    public void                     waitFor() {
        wait0(handle);
    }

    public void                     signal() {
        set0(handle);
    }

    public void                     close() {
        close0(handle);
    }

    private native long             init0(String name, boolean autoreset);
    private native boolean          wait0(long handle);
    private native boolean          set0(long handle);
    private native boolean          reset0(long handle);
    private native void             close0(long handle);
}
