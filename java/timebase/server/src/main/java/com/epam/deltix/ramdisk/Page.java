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

import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.memory.MemorySizeEstimator;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.*;
import java.util.Arrays;

/**
 *
 */
final class Page extends QuickList.Entry <Page> {
    private static final boolean    DEBUG_STATE_TRANSITIONS = false;
    private static final boolean    DEBUG_LOADS = false;
    private static final boolean    DEBUG_WRITES = false;

    static final long           OVERHEAD =
        MemorySizeEstimator.ARRAY_OVERHEAD +
        MemorySizeEstimator.OBJECT_OVERHEAD +
        MemorySizeEstimator.SIZE_OF_POINTER * 6 + 16;

    /**
     *  Value for validLength meaning that this page is not loaded.
     */
    static final int            UNLOADED = 0;

    /**
     *  Value for {link #startAddress} meaning that this page is not mapped.
     *  Also, in unmapped pages {link #fd} == null.
     */
    static final long           UNMAPPED = -1;

    /**
     *  Value for {link #index} meaning that this page is not mapped.
     *  Also, in unmapped pages {link #fd} == null.
     */
    static final int            UNMAPPED_IDX = -1;

    /**
     *  The constructor initializes the state to this value.
     */
    static final byte           NEW = 0;

    /**
     *  Unmapped page, linked to the free page list (pool). The constructor
     *  initializes the state to this value; the page must be
     *  linked to the free pool by the caller.
     */
    static final byte           FREE = 1;

    /**
     *  Mapped clean page, linked to the clean list, available for takeover.
     */
    static final byte           CLEAN = 2;

    /**
     *  Mapped dirty page, not linked to any list, available for save.
     */
    static final byte           DIRTY = 3;

    /**
     *  Freshly re-mapped page, checked out for load and read-only.
     */
    static final byte           READING = 4;

    /**
     *  Freshly re-mapped page, checked out for load and read-write.
     */
    static final byte           WRITING = 5;

    /**
     *  Dirty page, which was given out to the writer thread for saving.
     *  It will be saved momentarily and become CLEAN, unless it is written to,
     *  and then it transitions back to DIRTY. Pages in SAVING state are already
     *  removed from the dirty range.
     */
    static final byte           SAVING = 6;

    FD                          fd = null;
    private long                startAddress = UNMAPPED;
    private final byte []       data;
    private long                index = UNMAPPED_IDX;
    int                         validLength = UNLOADED;
    private byte                state = NEW;
    private int                 dirtyOffset = UNLOADED;
    private long                modified = Long.MIN_VALUE;
    volatile WriterThread       writer;

    public Page (int length) {
        //assert length <= Short.MAX_VALUE : "length is too large: " + length;

        data = new byte [length];
    }

    byte                        getState () {
        return (state);
    }

    private int                 getOffsetForRead (long address) {
        assert startAddress >= 0 : this + " is not loaded";

        long    longOffset = address - startAddress;
        
        assert longOffset >= 0 && longOffset < validLength :
            this + " is wrong for " + address;

        return ((int) longOffset);
    }

    private int                 getOffsetForWrite (long address) {
        assert startAddress >= 0 : this + " is not loaded";

        long    longOffset = address - startAddress;
        
        assert longOffset >= 0 && longOffset <= validLength :
            this + " is wrong for " + address + 
            ", or it's outside the valid data range.";

        return ((int) longOffset);
    }

    byte                        read (long srcAddress) {
        assert state != FREE : this + " is unmapped";
        assert fd != null;

        return (data [getOffsetForRead (srcAddress)]);
    }

    int                         read (
        long                        srcAddress,
        byte []                     dest,
        int                         destOffset,
        int                         length
    )
    {
        assert state != FREE : this + " is unmapped";
        assert fd != null;

        int     offset = getOffsetForRead (srcAddress);
        int     avail = validLength - offset;

        if (length > avail)
            length = avail;

        System.arraycopy (data, offset, dest, destOffset, length);        
        return (length);
    }

    void                        fileTruncated (long length) {
        validLength = getOffsetForWrite (length);
        dirtyOffset = 0;
        
        if (modified == Long.MIN_VALUE)
            modified = TimeKeeper.currentTime;
    }

    void                        write (long destAddress, byte b) {
        assert state == WRITING || state == DIRTY :
            this + " is not writable (WRITING | DIRTY)";

        assert fd != null;

        int     offset = getOffsetForWrite (destAddress);

        data [offset] = b;

        if (offset < dirtyOffset)
            this.dirtyOffset = offset;
        
        if (validLength <= offset) {
            int end = offset + 1;
            
            validLength = end;
            fd.setLogicalLength (startAddress + end);
        }

        if (modified == Long.MIN_VALUE)
            modified = TimeKeeper.currentTime;

        if (DEBUG_WRITES)
            System.out.println ("PAGE WRITE:    " + this);        
    }

    int                         write (
        long                        destAddress,
        byte []                     src,
        int                         srcOffset,
        int                         length
    )
    {
        assert state == WRITING || state == DIRTY :
            this + " is not writable (WRITING | DIRTY)";

        assert fd != null;
        
        final int     offset = getOffsetForWrite (destAddress);
        int     avail = data.length - offset;

        if (avail < 1)
            throw new IllegalStateException ("Can't write even 1 byte to " + this + " at " + destAddress);

        if (length > avail)
            length = avail;

        System.arraycopy (src, srcOffset, data, offset, length);
        
        int     end = offset + length;
        
        if (validLength < end) {
            validLength = end;
            
            fd.setLogicalLength (startAddress + end);
        }

        if (modified == Long.MIN_VALUE)
            modified = TimeKeeper.currentTime;

        if (DEBUG_WRITES)
            System.out.println ("PAGE WRITE:    " + this);

        if (offset < dirtyOffset)
            dirtyOffset = offset;

        return (length);
    }

    long                         getIndex () {
        assert index != UNMAPPED_IDX : this + " is unmapped";

        return (index);
    }

    /**
     *  Returns the inclusive start address covered by this page.
     */
    long                        getStartAddress () {
        assert startAddress != UNMAPPED : this + " is unmapped";

        return (startAddress);
    }

    /**
     *  Returns the exclusive end address covered by this page.
     */
    long                        getEndAddress () {
        assert startAddress != UNMAPPED && validLength != UNLOADED :
            this + " is unmapped";

        return (startAddress + validLength);
    }
    //
    //  State transitions
    //
    /**
     *  Map a free page for checkout. The page is unlinked from the free list
     *  and mapped to the new owner fd.
     *
     * @param fd            New owner fd.
     * @param address       File address.
     * @param targetState   Target state, either {@link #READING} or {@link #WRITING}.
     */
    void                        freeToCheckout (
        FD                          fd,
        long                        address,
        byte                        targetState
    )
    {
        assert
            targetState == READING ||
            targetState == WRITING :
            "Illegal target state: " + getStateName (targetState);

        assert state == FREE || state == NEW : this + " is not FREE or NEW";

        if (DEBUG_STATE_TRANSITIONS)
            System.out.println (
                "PAGE STATE:    freeToCheckout: " + this + " -> " + getStateName (targetState) +
                "; mapped to " + fd + " at " + address
            );

        if (state == FREE)
            unlink ();
        
        map (fd, address);
        state = targetState;
    }

    /**
     *  Take over a clean page and re-map it to a different fd for checkout.
     *  The page is removed from the previous owner's index,
     *  unlinked from the clean list, marked as unloaded and mapped to the
     *  new owner fd.
     *
     * @param fd            New owner fd.
     * @param address       File address.
     * @param targetState   Target state, either {@link #READING} or {@link #WRITING}.
     */
    void                        takeover (
        FD                          fd,
        long                        address,
        byte                        targetState
    )
    {
        assert
            targetState == READING ||
            targetState == WRITING :
            "Illegal target state: " + getStateName (targetState);

        assert state == CLEAN : this + " is not IN_CLEAN_LIST";

        if (DEBUG_STATE_TRANSITIONS)
            System.out.println (
                "PAGE STATE:    cleanToCheckout: " + this + " -> CLEAN; mapped to " +
                fd + " at " + address
            );

        unmap (targetState);
        map (fd, address);
    }

    /**
     *  Mark a page as dirty. This method is called before a synchronized write.
     *  If the page is dirty, nothing is done. If the page is clean, it is
     *  unlinked from the clean list, and marked as DIRTY (this includes
     *  its being registered in the dirty page range).
     */
    void                        mappedToDirty () {
        if (state == DIRTY)
            return;

        assert state == CLEAN || state == SAVING :
            this + ": expected DIRTY | SAVING";

        if (DEBUG_STATE_TRANSITIONS)
            System.out.println (
                "PAGE STATE:    mappedToDirty: " + this + " -> DIRTY"
            );

        if (state == CLEAN)
            unlink ();

        setDirty ();
    }

    /**
     *  Check in a page that was checked out for load. If the page was checked
     *  out for read-write, it is now marked as DIRTY (this includes
     *  its being registered in the dirty page range). Otherwise, the page
     *  is marked CLEAN. The caller (DataCache) must take care of linking
     *  this page to the clean list and notifying threads waiting for clean
     *  pages.
     *
     *  @return  Whether the page is clean
     */
    boolean                     checkIn () {
        assert state == READING || state == WRITING :
            this + " is not checked out (" + state + ")";

        if (state == READING) {
            if (DEBUG_STATE_TRANSITIONS)
                System.out.println (
                    "PAGE STATE:    checkIn: " + this + " -> CLEAN"
                );

            state = CLEAN;

            return (true);
        }
        else {
            if (DEBUG_STATE_TRANSITIONS)
                System.out.println (
                    "PAGE STATE:    checkIn: " + this + " -> DIRTY"
                );

            setDirty ();

            return (false);
        }
    }

    /**
     *  Free a mapped page. This method is typically called when a file is
     *  closed or truncated. The page is marked as unloaded and, if CLEAN,
     *  is unlinked from the clean list. This method does not take care of
     *  updating the dirty page range, because it is typically more
     *  efficient to do that once for all pages (on truncate).
     */
    void                        mappedToFree () {
        if (DEBUG_STATE_TRANSITIONS)
            System.out.println ("PAGE STATE:    mappedToFree: " + this + " -> FREE");

        writer = null;
        unmap (FREE);
    }

    /**
     *  Transitions a DIRTY page to SAVING, simultaneously copying the data into
     *  the supplied job object.
     *
     *  @param job  Page data is copied into this object.
     */
    void                        dirtyToSaving (SavePageJob job) {
        assert state == DIRTY : this + " is not DIRTY";

        if (DEBUG_STATE_TRANSITIONS)
            System.out.println (
                "PAGE STATE:    dirtyToSaving: " + this + " -> SAVING"
            );

        job.address = startAddress + dirtyOffset;
        job.length = validLength - dirtyOffset;
        System.arraycopy (data, dirtyOffset, job.data, 0, job.length);

        dirtyOffset = validLength;

        state = SAVING;
    }

    void                        savingFailed(long address) {
        modified = TimeKeeper.currentTime;
        fd.pageIndex.modified = modified;

        dirtyOffset = (int) (address - startAddress);
    }

    /**
     *  If the page is in SAVING state, transition it to CLEAN and return true.
     *  In this case, the caller must link the page to the clean list and
     *  notify threads waiting for clean pages. The page might also be in DIRTY
     *  mode, if it were written to at the same time as it was being saved.
     *  In this case, return false. The caller should do nothing in the latter
     *  case.
     *
     *  @return Whether the page is clean.
     */
    boolean                     savingToClean () {
        if (state == DIRTY)
            return (false);

        assert state == SAVING : this + ": expected SAVING | DIRTY";

        if (DEBUG_STATE_TRANSITIONS)
            System.out.println ("PAGE STATE:    savingToClean: " + this + " -> CLEAN");

        state = CLEAN;

        fd.pageIndex.jobCompleted (this);
        
        return (true);
    }

    private void                setDirty () {
        fd.pageIndex.setPageDirty (this);
        state = DIRTY;
    }

    private void                map (
        FD                          fd,
        long                        address
    )
    {
        long        h = fd.getPrivateHeaderLength ();

        assert address >= h :
            "Address " + address +
            " is in the private header range (" + h + ") for " + fd;

        assert address < DataCache.MAX_FILE_LENGTH :
            "Address " + address +
            " is too large (max " + DataCache.MAX_FILE_LENGTH + ")";

        long        n = address - h;

        this.fd = fd;
        this.index = (n >> DataCache.PAGE_SIZE_LOG2);
        this.startAddress = h + (n & DataCache.PAGE_SIZE_MASK);
        this.dirtyOffset = UNLOADED;
        this.modified = Long.MIN_VALUE;

        fd.pageIndex.set (index, this);
    }

    private void                unmap (byte targetState) {
        if (fd != null)
            fd.pageIndex.clear (index);

        if (state == CLEAN)
            unlink ();

        validLength = UNLOADED;
        fd = null;
        startAddress = UNMAPPED;
        index = UNMAPPED_IDX;
        dirtyOffset = UNLOADED;
        modified = Long.MIN_VALUE;

        if (RAMDisk.ASSERTIONS_ENABLED)
            Arrays.fill (data, (byte) -1);

        state = targetState;
    }

    /**
     *  Load this page, if necessary (determined by validLength == UNLOADED).
     *
     *  @throws IOException
     */
    void                        load (FD checkfd, long logicalLength)
        throws IOException
    {
        assert fd == checkfd : this + " is not mapped to " + checkfd;
        assert validLength == UNLOADED : this + " is already loaded";
        assert state == READING || state == WRITING :
            this + " is not checked out";

        long        available = logicalLength - startAddress;

        if (DEBUG_LOADS)
            RAMDisk.LOGGER.warning ("PAGE LOAD:    " + this + ": size=" + available);

        if (available < 0)
            throw new IllegalArgumentException("Attempt to load page at " + startAddress + " beyond " + logicalLength);

        validLength =
            available < DataCache.PAGE_SIZE ?
                (int) available :
                DataCache.PAGE_SIZE;

        int         numRead = validLength > 0 ? fd.directRead (startAddress, data, 0, validLength) : 0;
        
        this.dirtyOffset = validLength;
        this.modified = Long.MIN_VALUE;

        assert numRead == validLength :
            "Failed to load " + validLength + " bytes from " +
            fd + " at " + startAddress + ". Loaded " + numRead + " bytes";
    }

    @Override
    public String               toString () {
        StringBuilder       sb = new StringBuilder ();

        sb.append ("Page #");
        sb.append (System.identityHashCode (this));
        sb.append (" (");
        sb.append (getStateName ());

        if (fd == null)
            sb.append ("; UNMAPPED");
        else {
            sb.append ("; fd: ");
            sb.append (fd);
            sb.append ("; start: ");
            sb.append (startAddress);
            sb.append ("; index: ");
            sb.append (index);
            sb.append ("; validLength: ");
            sb.append (validLength);
            sb.append ("; dirtyOffset: ");
            sb.append (dirtyOffset);
        }

        sb.append (")");

        return (sb.toString ());
    }

    public long                 getModifiedTime() {
        return modified;
    }

    public boolean              isCompleted() {
        return validLength == DataCache.PAGE_SIZE;
    }

    public static String        getStateName (byte loc) {
        switch (loc) {
            case FREE:                  return ("FREE");
            case CLEAN:                 return ("CLEAN");
            case DIRTY:                 return ("DIRTY");
            case READING:               return ("READING");
            case WRITING:               return ("WRITING");
            case SAVING:                return ("SAVING");
            default:                    return ("#" + loc);
        }
    }

    public String               getStateName () {
        return (getStateName (state));
    }
}