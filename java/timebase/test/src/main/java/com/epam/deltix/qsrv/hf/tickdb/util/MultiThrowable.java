package com.epam.deltix.qsrv.hf.tickdb.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MultiThrowable extends RuntimeException {
    private List <TestThread>       errorThreads = new ArrayList <TestThread> ();
    private List <Throwable>        errors = new ArrayList <Throwable> ();

    public MultiThrowable () {
    }

    public void                     check (TestThread t) {
        if (t.isAlive ()) {
            errorThreads.add (t);
            errors.add (null);
        }
        else {
            Throwable   x = t.getError ();

            if (x != null) {
                errorThreads.add (t);
                errors.add (x);
            }
        }
    }

    public boolean                  hasErrors () {
        return (errorThreads.size () > 0);
    }
    
    @Override
    public void                     printStackTrace (PrintStream s) {
        for (int ii = 0; ii < errorThreads.size (); ii++) {
            TestThread      t = errorThreads.get (ii);
            Throwable       x = errors.get (ii);
            
            if (x == null)
                s.printf ("Thread %s did not terminate\n", t.getName ());
            else {
                s.printf ("In thread %s: ", t.getName ());
                x.printStackTrace (s);
            }
        }
    }

    @Override
    public void                     printStackTrace (PrintWriter s) {
        for (int ii = 0; ii < errorThreads.size (); ii++) {
            TestThread      t = errorThreads.get (ii);
            Throwable       x = errors.get (ii);

            if (x == null)
                s.printf ("Thread %s did not terminate\n", t.getName ());
            else {
                s.printf ("In thread %s: ", t.getName ());
                x.printStackTrace (s);
            }
        }
    }
}
