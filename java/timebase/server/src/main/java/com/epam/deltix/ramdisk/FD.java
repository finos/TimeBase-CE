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

import com.epam.deltix.util.lang.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.logging.Level;

/**
 *  Access to this class must be synchronized by the caller, as is the case
 *  with I/O objects in most frameworks.
 */
public class FD implements Disposable  {
    private static final boolean    DEBUG_SAVE = false;
    
    public static final GrowthPolicy    DEFAULT_GROWTH_POLICY =
        new MixedGrowthPolicy (2, 1 << 10, 1 << 24);

    private static final long       PHYSICAL_LENGTH_UNKNOWN = -1;

    private boolean                 isOpen = false;
    private boolean                 isReadOnly;
    private final RAMDisk           ramdisk;
    final DataCache                 dataCache;
    private final RAFCache          rafCache;
    private final File              file;
    private long                    logicalLength = 0;
    private long                    physicalLength = PHYSICAL_LENGTH_UNKNOWN;

    /**
     *  RAFCache cookie, managed and guarded by RAFCache. Do not use directly.
     */
    RAFCache.Cookie                 rafCacheCookie = null;

    /**
     *  Field for asserting that two RAFs are never opened for the same FD.
     *  Managed and guarded by RAFCache. Do not use directly.
     */
    RAFCache.RAF                    uniqueRAFcheck = null;

    /**
     *  Page index cookie, managed and guarded by DataCache. Do not use directly.
     */
    protected PageIndex             pageIndex;

    public FD (RAMDisk ramdisk, File file) {
        this.ramdisk = ramdisk;
        this.rafCache = ramdisk.rafCache;
        this.dataCache = ramdisk.dataCache;
        this.file = file;
        this.pageIndex = dataCache != null ? new PageIndex (this) : null;
    }

    public FD (RAMDisk ramdisk, File file, boolean useCache) {
        this.ramdisk = ramdisk;
        this.rafCache = ramdisk.rafCache;
        this.file = file;

        this.dataCache = useCache ? ramdisk.dataCache : null;
        this.pageIndex = dataCache != null ? new PageIndex (this) : null;
    }

    public final File           getFile () {
        return file;
    }

    public final boolean        isOpen () {
        return (isOpen);
    }
    
    public final boolean        isReadOnly () {
        return (isReadOnly);
    }

    public final long           getLogicalLength () {
        return logicalLength;
    }

    public final long           getPhysicalLength () {
        return physicalLength;
    }

    public void                 open (boolean readOnly) throws IOException {
        if (!ramdisk.register (this))
            throw new IllegalStateException (this + " is already open");

        isOpen = true;
        isReadOnly = readOnly;

        physicalLength = file.length ();

        onOpen ();
    }

    public void                 format () throws IOException {
        if (!ramdisk.register (this))
            throw new IllegalStateException (this + " is already open");
        
        isOpen = true;
        isReadOnly = false;

        physicalLength = file.length ();
        
        onFormat ();
    }

    public void                 warmUp () 
        throws IOException, NoFreePagesException
    {
        assertOpen ();

        if (dataCache == null)
            return;

        Page            page = null;

        for (;;) {
            page = dataCache.checkOutPageForWarmUp (this, page);

            if (page == null)
                break;

            try {
                page.load (this, logicalLength);
            } finally {
                dataCache.checkIn (page);
            }
        }
    }

    //
    //  Customization API
    //
    /**
     *  Override to modify {@link #DEFAULT_GROWTH_POLICY} (2X, min 1KB, max 16MB).
     *
     *  @return         Custom growth policy.
     */
    public GrowthPolicy         getGrowthPolicy () {
        return DEFAULT_GROWTH_POLICY;
    }

    /**
     *  Override to force auto-commit at the end of the save cycle.
     * 
     *  @return false
     */
    public boolean              getAutoCommit () {
        return (false);
    }

    /**
     *  Called asynchronously by the framework when file is committed.
     *  Any exceptions thrown from this method will be logged, but otherwise
     *  ignored. The default implementation does nothing.
     */
    protected void              onCleanCommit (long cleanLength) 
        throws IOException
    {
        if (DEBUG_SAVE) {
            System.out.println (
                "COMMIT " + this + "; clean length: " + cleanLength
            );             
        }
    }

    /**
     *  Override to provide alternative method of persisting the logical
     *  length. The default implementation calls {@link #trimToSize}.
     */
    protected void              onCommitLength () throws IOException {
        trimToSize ();
    }

    /**
     *  Override to provide alternative method of persisting the logical
     *  length. The default implementation sets logical length to the physical
     *  length.
     */
    protected void              onOpen () throws IOException {
        logicalLength = physicalLength;
    }

    /**
     *  Override to provide alternative method of formatting the file.
     *  The default implementation sets logical length to the result of
     *  {@link #getPrivateHeaderLength}.
     */
    protected void              onFormat () throws IOException {
        logicalLength = getPrivateHeaderLength ();
    }

    /**
     *  Override to provide additional handling after file truncation; for
     *  example, the subclass may want to fully commit the new length.
     *  The default implementation does nothing.
     */
    protected void              onTruncate () throws IOException {
    }

    /**
     *  Override to reserve private space at the beginning of the file.
     *  The default implementation returns 0.
     */
    public long                 getPrivateHeaderLength () {
        return (0);
    }
    //
    //  Direct I/O
    //
    private void                assertOpen () {
        if (!isOpen)
            throw new IllegalStateException (this + " is closed");
    }

    protected final void        directForce () throws IOException {
        directForce(false);
    }

    protected final void        directForce (boolean metadata)
        throws IOException
    {
        assertOpen ();

        RAFCache.RAF    raf = rafCache.checkOut (this);

        try {
            synchronized (raf) {
                FileChannel     fc = raf.getChannel ();

                fc.force (metadata);

                //  For some reason we can be here without any exceptions
                //  but with a suddenly closed file.
                if (Thread.interrupted ())
                    throw new InterruptedIOException ();

                if (!fc.isOpen ())
                    throw new IOException ("FileChannel is closed.");
            }
        } finally {
            rafCache.checkIn (this, raf);
        }
    }

    public final void           trimToSize () throws IOException {
        assertOpen ();

        if (physicalLength > logicalLength) {
            RAFCache.RAF    raf = rafCache.checkOut (this);

            try {
                synchronized (raf) {
                    raf.setLength (logicalLength);
                }
            } finally {
                rafCache.checkIn (this, raf);
            }
            
            physicalLength = logicalLength;
        }
    }

    protected final int         directRead (long srcOffset)
        throws IOException
    {
        assertOpen ();

        RAFCache.RAF    raf = rafCache.checkOut (this);

        try {
            synchronized (raf) {
                raf.seek (srcOffset);
                return (raf.read ());
            }
        } finally {
            rafCache.checkIn (this, raf);
        }
    }

    protected final int         directRead (
        long                        srcOffset,
        byte []                     dest,
        int                         destOffset,
        int                         length
    )
        throws IOException
    {
        assertOpen ();

        RAFCache.RAF    raf = rafCache.checkOut (this);

        try {
            synchronized (raf) {
                raf.seek (srcOffset);

                int     n = raf.read (dest, destOffset, length);
                
                if (n == length)
                    return (n);

                if (n < 0)
                    return (0);
                //
                //  Not sure we ever getBytes a partial read, but nothing
                //  says it's impossible... Just in case, do the read loop.
                //
                destOffset += n;
                length -= n;

                while (n < length) {
                    int     m = raf.read (dest, destOffset, length);

                    if (m < 0)
                        break;

                    destOffset += m;
                    length -= m;
                    n += m;
                }

                return (n);
            }
        } finally {
            rafCache.checkIn (this, raf);
        }
    }

    /**
     *  This method is critical in order to enforce the specified growth policy,
     *  rather than grow the length by tiny increments.
     *
     *  @param minLength    Minimum required file length.
     *
     *  @throws IOException If we fail to resize the file.
     */
    private final void          applyGrowthPolicy (
        RandomAccessFile            raf,
        long                        minLength
    )
        throws IOException
    {
        assert Thread.holdsLock (raf);

        GrowthPolicy        growthPolicy = getGrowthPolicy ();

        if (growthPolicy != null && minLength > physicalLength) {
            if (physicalLength == PHYSICAL_LENGTH_UNKNOWN) {
                physicalLength = raf.length ();
                
                if (minLength <= physicalLength)
                    return;
            }
            else
                assert physicalLength == raf.length () :
                    "Cached length " + physicalLength +
                    " does not match actual " + raf.length ();

            physicalLength = growthPolicy.computeLength (physicalLength, minLength);

            //  No need to extend if precise match
            if (physicalLength > minLength)
                raf.setLength (physicalLength);
        }
    }

    protected final void        directWrite (
        long                        destOffset,
        byte []                     bytes,
        int                         srcOffset,
        int                         length
    )
        throws IOException
    {
        assertOpen ();

        RAFCache.RAF    raf = rafCache.checkOut (this);

        try {
            synchronized (raf) {
                applyGrowthPolicy (raf, destOffset + length);
                raf.seek (destOffset);
                raf.write (bytes, srcOffset, length);
            }
        } finally {
            rafCache.checkIn (this, raf);
        }
    }

    protected final void        directWrite (
        long                        destOffset,
        int                         b
    )
        throws IOException
    {
        assertOpen ();

        RAFCache.RAF    raf = rafCache.checkOut (this);

        try {
            synchronized (raf) {
                applyGrowthPolicy (raf, destOffset + 1);
                raf.seek (destOffset);
                raf.write (b);
            }
        } finally {
            rafCache.checkIn (this, raf);
        }        
    }

    protected final void        setLogicalLength (long length) {
        // TODO: when RAMDisk is used, related pages should be truncated
        // in case when logical less than current. Use 'truncate' for such cases.

//        assert length >= logicalLength :
//            "setLogicalLength (" + length +
//            "), less than current logicalLength = " + logicalLength;

        logicalLength = length;
    }
    //
    //  Cached I/O implementation.
    //
    public final int            read (long srcAddress) throws IOException {
        if (srcAddress >= logicalLength)
            return (-1);

        if (dataCache == null)
            return (directRead (srcAddress));

        PageContainer c = dataCache.checkOutContainer();

        try {
            byte value = dataCache.read(this, srcAddress, c);

            if (c.checkedOutPage != null) {
                Page     page = c.checkedOutPage;
                try {
                    page.load (this, logicalLength);
                    return (page.read (srcAddress));
                } finally {
                    dataCache.checkIn (page);
                }
            }

            return (value & 0xFF);

        } catch (InterruptedException ix) {
            throw new InterruptedIOException ();
        } finally {
            dataCache.checkIn(c);
        }
    }

    public final int            read (
        long                        srcAddress,
        byte []                     dest,
        int                         destOffset,
        int                         length
    )
        throws IOException
    {
        if (length == 0)
            return (0);

        long                avail = logicalLength - srcAddress;

        if (avail < length) {
            if (avail <= 0)
                return (0);
            else
                length = (int) avail;
        }

        if (dataCache == null)
            return (directRead (srcAddress, dest, destOffset, length));

        PageContainer c = dataCache.checkOutContainer();

        int                 remain = length;

        try {
            for (;;) {
                int nread = dataCache.read (this, srcAddress, dest, destOffset, remain, c);

                if (c.checkedOutPage != null) {
                    Page    page = c.checkedOutPage;
                    try {
                        page.load (this, logicalLength);

                        nread = page.read (srcAddress, dest, destOffset, remain);
                    } finally {
                        dataCache.checkIn (page);
                    }
                }

                assert nread > 0;

                remain -= nread;

                if (remain == 0)
                    break;

                srcAddress += nread;
                destOffset += nread;
            }
        } catch (InterruptedException ix) {
            throw new InterruptedIOException ();
        } finally {
            dataCache.checkIn (c);
        }
        
        return (length);
    }

    public final void           write (
        long                        destAddress,
        byte                        b
    )
        throws IOException
    {
        if (dataCache == null) {
            if (destAddress >= logicalLength)
                logicalLength = destAddress + 1;

            directWrite (destAddress, b);
            return;
        }

        PageContainer c = dataCache.checkOutContainer();
        
        try {
            dataCache.write (this, destAddress, b, c);

            if (c.checkedOutPage != null) {
                Page     page = c.checkedOutPage;

                try {
                    page.load (this, logicalLength);

                    page.write (destAddress, b);
                } finally {
                    dataCache.checkIn (page);
                }
            }
        } catch (InterruptedException ix) {
            throw new InterruptedIOException ();
        } finally {
            dataCache.checkIn(c);
        }
    }

    public final void           write (
        long                        destAddress,
        byte []                     src,
        int                         srcOffset,
        int                         length
    )
        throws IOException
    {
        if (length == 0)
            return;

        if (dataCache == null) {
            long                end = destAddress + length;

            if (end > logicalLength)
                logicalLength = end;

            directWrite (destAddress, src, srcOffset, length);
            return;
        }

        int                 remain = length;

        PageContainer c = dataCache.checkOutContainer();

        try {
            for (;;) {
                int nwritten = dataCache.write (this, destAddress, src, srcOffset, remain, c);

                if (c.checkedOutPage != null) {
                    Page    page = c.checkedOutPage;

                    try {
                        page.load (this, logicalLength);

                        nwritten = page.write (destAddress, src, srcOffset, remain);
                    } finally {
                        dataCache.checkIn (page);
                    }
                }

                assert nwritten > 0;

                remain -= nwritten;

                if (remain == 0)
                    break;

                destAddress += nwritten;
                srcOffset += nwritten;
            }
        } catch (InterruptedException ix) {
            throw new InterruptedIOException ();
        } finally {
            dataCache.checkIn(c);
        }
    }

    /**
     *  Wait for all changes to be saved to disk. 
     */
    public final void           waitForFlush ()
        throws IOException, InterruptedException
    {
        assertOpen ();

        if (dataCache != null)
            dataCache.waitForFlush (this);
    }

    public final void           truncate (long length) throws IOException {
        assertOpen ();

        if (logicalLength == length)
            return;

        if (logicalLength < length)
            throw new IllegalArgumentException (
                "length: " + length +
                " is greater than current logical length: " + logicalLength
            );

        if (dataCache != null)
            dataCache.truncating (this, length);

        logicalLength = length;
        
        onTruncate ();
    }

    public final void           closeNoSave () {
        closeInternal (false);
    }

    private void                closeInternal (boolean checkSave) {
        // Ensure we only go through the closing once.
        if (!ramdisk.unregister (this))
            return;

        if (dataCache != null)
            dataCache.beingClosed (this, checkSave);

        rafCache.beingClosed (this);
        isOpen = false;
    }

    public void                 close() {
        if (!isReadOnly) {
            try {
                waitForFlush ();
            } catch (Throwable x) {
                RAMDisk.LOGGER.log (Level.SEVERE, "Error waiting for flush", x);
            }

            try {
                onCommitLength ();
            } catch (Throwable x) {
                RAMDisk.LOGGER.log (Level.SEVERE, "Error committing length", x);
            }
        }

        closeInternal (true);
    }

    @Override
    public String               toString () {
        return ("FD (@" + hashCode () + "; file: " + file.getName () + ")");
    }
}