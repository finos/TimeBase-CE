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
package com.epam.deltix.ramdisk;

import com.epam.deltix.util.collections.QuickList;

import java.io.*;

/**
 * 
 */
public class RAFCache {
    static class Cookie extends QuickList.Entry <Cookie> {
        private FD                          fd;
        RAF                                 raf;
        private int                         checkOutRefCount = 0;
    }

    private int                             maxNumOpenFiles = 100;
    private int                             numOpenFiles = 0;
    private final QuickList <Cookie>        lru = new QuickList <Cookie> ();

    public RAFCache (int maxNumOpenFiles) {
        this.maxNumOpenFiles = maxNumOpenFiles;
    }

    public enum RAFState {
        OPEN,
        CLOSING,
        CLOSED
    }

    public class RAF extends RandomAccessFile {
        private final FD        fd;
        private RAFState        state = RAFState.OPEN;

        RAF (FD fd) throws FileNotFoundException {
            super (fd.getFile (), fd.isReadOnly () ? "r" : "rw");

            this.fd = fd;

            if (RAMDisk.ASSERTIONS_ENABLED) {
                assert fd.uniqueRAFcheck == null : "Unexpected: " + fd.uniqueRAFcheck;

                fd.uniqueRAFcheck = this;
            }
        }

        @Override
        public void         close () throws IOException {
            if (state == RAFState.CLOSING)
                return;
            
            state = RAFState.CLOSING;

            if (RAMDisk.ASSERTIONS_ENABLED) {
                assert fd.uniqueRAFcheck == this :
                    "Unexpected: " + fd.uniqueRAFcheck + " != " + this;

                fd.uniqueRAFcheck = null;
            }

            super.close ();

            state = RAFState.CLOSED;
        }

        public RAFState     getState () {
            return (state);
        }

        @Override
        public String       toString () {
            return "RAF (@" + hashCode () + "; fd: " + fd + "; state: " + state + ")";
        }
    }

    synchronized int            getNumOpenFiles () {
        return (numOpenFiles);
    }

    synchronized void           beingClosed (FD fd) {
        Cookie      cookie = fd.rafCacheCookie;

        if (cookie != null) {
            try {
                cookie.raf.close ();
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException( "Unexpected error closing RandomAccessFile " + fd.getFile(), iox);
            }

            cookie.unlink ();
            numOpenFiles--;
            fd.rafCacheCookie = null;
            notify ();
        }
    }

    synchronized RAF            checkOut (FD fd)
        throws IOException
    {
        for (;;) {
            //
            // Return cached file
            //
            Cookie      cookie = fd.rafCacheCookie;

            if (cookie != null) {
                if ((cookie.checkOutRefCount++) == 0)
                    cookie.unlink ();

                return (cookie.raf);
            }
            //
            //  Allocate new, if allowed
            //
            if (numOpenFiles < maxNumOpenFiles) {
                cookie = new Cookie ();

                cookie.raf = new RAF (fd);

                numOpenFiles++;
                fd.rafCacheCookie = cookie;
                cookie.fd = fd;
                cookie.checkOutRefCount = 1;

                return (cookie.raf);
            }
            //
            //  Close LRU file
            //
            cookie = lru.getFirst ();

            if (cookie != null) {
                assert cookie.checkOutRefCount == 0 :
                    "Checked out cookie found in the LRU list";
                
                RAMDisk.LOGGER.fine ("Recycling a file ...");

                //  Unlink from previous owner
                cookie.fd.rafCacheCookie = null;

                //  Check out to new owner
                cookie.unlink ();
                numOpenFiles--;
                cookie.raf.close (); // If we fail here, the cookie is simply lost.

                cookie.raf = new RAF (fd);

                numOpenFiles++;
                cookie.fd = fd;
                cookie.checkOutRefCount = 1;
                fd.rafCacheCookie = cookie;

                return (cookie.raf);
            }

            //RAMDisk.LOGGER.info ("RAFCache is out of files (" + numOpenFiles + " open); will wait.");

            try {
                wait ();
            } catch (InterruptedException x) {
                throw new InterruptedIOException ("while waiting for a RAF");
            }
        }       
    }

    synchronized void           checkIn (FD fd, RAF raf) {
        Cookie      cookie = fd.rafCacheCookie;

        if (cookie == null)  // file closed
            return;

        assert raf.fd == fd : "raf.fd: " + raf.fd + "; fd: " + fd;
        assert cookie.fd == fd : "cookie.fd: " + cookie.fd + "; fd:" + fd;
        assert cookie.raf == raf: "cookie.raf: " + cookie.raf + "; raf:" + raf;
        assert cookie.checkOutRefCount > 0 : "checkOutRefCount: " + cookie.checkOutRefCount;

        if ((--cookie.checkOutRefCount) == 0) {
            lru.linkLast (fd.rafCacheCookie);
            notify ();
        }
    }
}