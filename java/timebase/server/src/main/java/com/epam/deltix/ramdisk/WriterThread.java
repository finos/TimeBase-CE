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
package com.epam.deltix.ramdisk;

import com.epam.deltix.util.runtime.*;
import com.epam.deltix.util.time.TimeKeeper;

import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

/**
 *
 */
class WriterThread extends Thread {
    private final DataCache     cache;
    private volatile boolean    stop = false;
    private volatile boolean    running = true;

    public WriterThread (DataCache cache) {
        super ("Writer Thread (" + cache + ")");

        this.cache = cache;
    }

    public void                 wakeUp () {
        if (!running)
            LockSupport.unpark (this);
    }

    public void                 shutdown () {
        stop = true;
        wakeUp ();
    }

    @Override
    public void                 run () {
        SavePageJob       job = new SavePageJob (this);

        try {
            while (!stop) {
                long time = cache.getNextJobTime(job);
                long delay = TimeKeeper.currentTime - time;

                if (delay < 30 && delay >= 0)
                    Thread.sleep(30 - delay);

                if (!cache.getNextJob (job)) {

                    if (stop) // we should not parking when stopped
                        break;

                    running = false;
                    LockSupport.park ();
                    running = true;

                    if (Thread.interrupted ()) {
                        if (stop)
                            break;

                        RAMDisk.LOGGER.severe ("Interrupted, but not shutdown");
                    }
                    continue;
                }

                final FD        fd = job.fd;
                final long      address = job.address;
                final int       length = job.length;
                
                try {
                    //System.out.println (fd + ": directWrite (pos=" + address + ";length=" + length + ")");
                    fd.directWrite (address, job.data, 0, length);
                } catch (Throwable x) {
                    RAMDisk.LOGGER.log (
                        Level.SEVERE,
                        "Error saving " + job.length +
                        " bytes at " + job.address + " to " + fd +
                            ";\nERROR IGNORED, BUT DATA NOT SAVED.",
                        x
                    );
                    //
                    //  Try and recover, in case the problem is temporary...
                    //
                    cache.pageNotSaved (job.page, address, length);
                    Thread.sleep(30);
                    continue;
                }

                if (fd.getAutoCommit() && job.isLastDirtyPage && job.followsCleanRange) {
                    try {
                        // if page is not re-taken - call clean commit
                        if (job.writer.equals(job.page.writer))
                            fd.onCleanCommit (address + length);
                    } catch (Throwable x) {
                        cache.onError();

                        RAMDisk.LOGGER.log (
                            Level.SEVERE,
                            "Error in onCommit () in " + fd + "; IGNORED.",
                            x
                        );

                        cache.handler.propertyChanged(RAMDisk.Properties.failures, x);
                    }
                }

                //
                //  The following call will allow the file to close
                //  (if it's being closed); therefore, it
                //  must **follow** auto-commit.
                //
                cache.pageSaved (job);

            }
        } catch (Throwable x) {
            RAMDisk.LOGGER.log (
                Level.SEVERE,
                "CRITICAL ERROR IN WriterThread. Timebase will shutdown.",
                x
            );
            
            cache.handler.propertyChanged(RAMDisk.Properties.failures, x);
            cache.setShutdownTimeout(1); // won't wait
            Shutdown.asyncTerminate();
        }

        RAMDisk.LOGGER.fine (this + " is terminating.");
    }
}