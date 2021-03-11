package com.epam.deltix.qsrv.hf.tickdb.util;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 *  Base class for multi-threaded tests.
 */
public abstract class MultiThreadedTest <T extends TestThread> {
    
    private List <T>                threads = new ArrayList <T> ();

    protected void                  clearThreads () {
        threads.clear ();
    }

    protected int                   getNumThreads () {
        return (threads.size ());
    }

    protected void                  add (T thread) {
        threads.add (thread);
    }

    protected void                  start () {
        for (Thread r : threads)
            r.start ();
    }
    
    protected void                  interrupt () {
        for (Thread r : threads)
            r.interrupt ();
    }

    protected void                  join ()
        throws InterruptedException
    {
        join (1236629973641L);
    }

    protected void                  join (long timeout)
        throws InterruptedException
    {
        long                    absoluteTimeout = 
            System.currentTimeMillis () + timeout;
        
        for (Thread t : threads) {
            long    remain = absoluteTimeout - System.currentTimeMillis ();

            if (remain <= 0)
                break;
            
            t.join (remain);
        }

        MultiThrowable          mx = new MultiThrowable ();

        for (TestThread t : threads)
            mx.check (t);

        if (mx.hasErrors ())
            throw mx;
    }        
}
