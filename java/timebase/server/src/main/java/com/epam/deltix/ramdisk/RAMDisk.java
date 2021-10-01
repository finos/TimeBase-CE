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

import java.util.logging.Logger;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.NotificationHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.PropertyMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.PropertyMonitorHandler;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.lang.Assertions;
import net.jcip.annotations.GuardedBy;

/**
 *
 */
public class RAMDisk implements PropertyMonitorHandler {

    public enum Properties {
        cacheSize,
        usedCacheSize,
        numPages,
        numOpenedFiles,
        bytesWritten,
        bytesRead,
        queueLength,
        writerState,
        failures
    }

    /**
     *  The size of data cached on a single page. Normally 8K.
     */
    public static final int     PAGE_SIZE =
        DataCache.PAGE_SIZE;
    
    /**
     *  The approximate amount of heap consumed by a page. This includes
     *  {@link #PAGE_SIZE} plus relatively small overhead of the paging system.
     */
    public static final long    PAGE_SIZE_WITH_OVERHEAD =
        DataCache.PAGE_SIZE_WITH_OVERHEAD;

    static final boolean        ASSERTIONS_ENABLED = Assertions.ENABLED;

    public static final String  LOGGER_NAME = "deltix.ramdisk";
    public static final Logger  LOGGER = Logger.getLogger (LOGGER_NAME);

    final DataCache                 dataCache;
    final RAFCache                  rafCache;

    final NotificationHandler       handler = new NotificationHandler("RAMDisk", Properties.values(), 1000);

    @GuardedBy ("fds")
    private final ObjectHashSet<FD>     fds = new ObjectHashSet<FD> ();

    private RAMDisk (int maxNumOpenFiles, DataCache dataCache) {
        this.dataCache = dataCache;
        if (dataCache != null)
            dataCache.setHandler(handler);

        this.rafCache = new RAFCache (maxNumOpenFiles);
    }

    public static RAMDisk   createNonCached (int maxNumOpenFiles) {
        return (new RAMDisk (maxNumOpenFiles, null));
    }

    public static RAMDisk   createCached (
        int                     maxNumOpenFiles,
        long                    memorySize,
        long                    preallocateSize
    )
    {
        if (memorySize < DataCache.PAGE_SIZE_WITH_OVERHEAD)
            throw new IllegalArgumentException (
                "Cache size too small: " + memorySize +
                "; minimum: " + DataCache.PAGE_SIZE_WITH_OVERHEAD
            );

        Runtime     rt = Runtime.getRuntime ();

        long    maxMem = (rt.maxMemory () - rt.totalMemory () + rt.freeMemory ());
        long    limit = Math.max((long) (0.75 * maxMem), maxMem - (2L << 30)); // 2GB

        if (memorySize > limit) {
            RAMDisk.LOGGER.warning (
                "Limiting specified max RAMDisk size (" +
                memorySize + ") to 0.75 of max memory (" + maxMem +
                "), i.e. down to " + limit
            );

            memorySize = limit;
        }

        RAMDisk.LOGGER.info ("Initializing RAMDisk. Data cache size = " + memorySize / (1024*1024) + "MB." +
                (preallocateSize > 0 ? " Initial Cache allocation = " + preallocateSize / (1024*1024) + "MB.": ""));

        long        numPages = memorySize / DataCache.PAGE_SIZE_WITH_OVERHEAD;
        long        initial = preallocateSize / DataCache.PAGE_SIZE_WITH_OVERHEAD;

        return (createCacheByNumPages (maxNumOpenFiles, numPages, initial));
    }

    public static RAMDisk   createCacheByNumPages (
        int                     maxNumOpenFiles,
        long                    numPages,
        long                    initialNumPages
    )
    {
        return (new RAMDisk (maxNumOpenFiles, new DataCache (numPages, initialNumPages)));
    }

    public void             start () {
        if (dataCache != null)
            dataCache.start ();

        //started = true;
    }

    public void             shutdownNoWait () {
        if (dataCache != null) {
            dataCache.startShutdown();
            dataCache.shutdownNoWait ();
        }
    }

    public void             startShutdown() {
        if (dataCache != null)
            dataCache.startShutdown ();
    }

    public void             setShutdownTimeout(long timeout) {
        if (dataCache != null)
            dataCache.setShutdownTimeout (timeout);
    }

    public void             shutdownAndWait () throws InterruptedException {
        if (dataCache != null) {
            dataCache.startShutdown();
            dataCache.shutdownAndWait ();
        }
    }

    boolean                 register (FD fd) {
        boolean result;
        synchronized (fds) {
            result = fds.add(fd);
        }

        if (handler.hasListeners())
            handler.propertyChanged(Properties.numOpenedFiles, rafCache.getNumOpenFiles());
        return result;
    }

    boolean                 unregister (FD fd) {
        boolean result;
        synchronized (fds) {
            result = fds.remove(fd);
        }

        if (handler.hasListeners())
            handler.propertyChanged(Properties.numOpenedFiles, rafCache.getNumOpenFiles());
        return result;
    }

    public GlobalStats      getStats () {
        GlobalStats             ret = new GlobalStats ();

        getStats (ret);

        return (ret);
    }

    public void             getStats (GlobalStats out) {
        synchronized (fds) {
            out.numOpenVirtualFiles = fds.size ();
        }

        out.numOpenFiles = rafCache.getNumOpenFiles ();
        
        if (dataCache == null) {
            out.numAllocPages = 0;
            out.numFreePages = 0;
        }
        else
            dataCache.getStats (out);
    }

    @Override
    public void addPropertyMonitor(PropertyMonitor listener) {
        handler.addPropertyMonitor(listener);
    }

    @Override
    public void removePropertyMonitor(PropertyMonitor listener) {
        handler.removePropertyMonitor(listener);
    }
}