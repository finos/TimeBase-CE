/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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