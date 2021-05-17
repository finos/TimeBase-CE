/*
 * Copyright 2021 EPAM Systems, Inc
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
