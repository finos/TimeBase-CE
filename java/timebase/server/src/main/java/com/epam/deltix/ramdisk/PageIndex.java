package com.epam.deltix.ramdisk;

import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.lang.MathUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *  A sub-object of FD, which is only used when Data Cache is enabled.
 */
final class PageIndex extends QuickList.Entry <PageIndex> implements Comparable<PageIndex> {
    private static final long    NONE = -1;

    private static final AtomicInteger      idGenerator = new AtomicInteger(0);

    // unique id of pageIndex
    private final int                       id;

    private final FD                        fd;

    private long                            lastPageIndex = 0;
    private long                            firstDirtyPageIndex = NONE;
    private long                            lastDirtyPageIndex = NONE;
    private long                            nextPageIndexToSave = NONE;

    long                        modified = Long.MIN_VALUE; // time of last modification
    boolean                     queued = false;

    PageIndex (FD fd) {
        this.fd = fd;
        this.id = idGenerator.getAndIncrement();
    }

    FD                          getFD () {
        return (fd);
    }

    void                        startSaving () {
        assert firstDirtyPageIndex != NONE : this + " is clean";

        nextPageIndexToSave = firstDirtyPageIndex;
    }

    boolean                     getNextJob (SavePageJob job) {
        if (nextPageIndexToSave == NONE)
            return (false);

        assert nextPageIndexToSave >= firstDirtyPageIndex :
            "nextPageIndexToSave == " + nextPageIndexToSave +
            " < firstDirtyPageIndex == " + firstDirtyPageIndex;

        final Page      p = get(nextPageIndexToSave);
        
        p.dirtyToSaving (job);

        job.page = p;
        p.writer = job.writer; // assign writer thread
        job.followsCleanRange = (nextPageIndexToSave == firstDirtyPageIndex);

        for (;;) {
            if (nextPageIndexToSave == lastDirtyPageIndex) {
                job.isLastDirtyPage = true;
                nextPageIndexToSave = NONE;
                break;
            }

            nextPageIndexToSave++;

            Page    next = get(nextPageIndexToSave);

            if (next != null && next.getState () == Page.DIRTY) {
                job.isLastDirtyPage = false;
                break;
            }
        }

        return (true);
    }

    void                        jobCompleted (Page page) {
        long         idx = page.getIndex ();

        assert get(idx) == page :
            "jobCompleted called with " + page +
            " but pages [" + idx + "] == " + get(idx);

        if (idx != firstDirtyPageIndex) {
            //
            //  firstDirtyPageIndex could drop if a page prior to currently
            //  saved got dirty. But it could not possibly go up.
            //
            assert firstDirtyPageIndex < idx :
                "firstDirtyPageIndex jumped up from " + idx +
                " to " + firstDirtyPageIndex;

            return;
        }

        //
        //  If we are here, pages [firstDirtyPageIndex] has become clean.
        //  Advance firstDirtyPageIndex.
        //
        for (;;) {
            if (firstDirtyPageIndex == lastDirtyPageIndex) {
                setClean ();
                break;
            }

            firstDirtyPageIndex++;

            Page    next = get(firstDirtyPageIndex);

            if (next != null && next.getState () == Page.DIRTY) {
                modified = next.getModifiedTime();
                fd.dataCache.relink(this);
                break;
            }
        }       
    }

//    Iterable<Page>                     getPages () {
//        return pages.values();
//    }

    private long                       getLastPageIndex() {
        return lastPageIndex;
    }

    /**
     *  Returns whether this index has pages in the DIRTY or UNSAVED state.
     *  This is determined by examining the dirty range.
     */
    boolean                     hasUnsavedPages () {
        return (lastDirtyPageIndex != NONE);
    }

    void                        setPageDirty (Page page) {
        assert this == page.fd.pageIndex;

        long     idx = page.getIndex ();

        assert get(idx) == page :
            "setPageDirty called with " + page +
            " but pages [" + idx + "] == " + get(idx);

        if (lastDirtyPageIndex == NONE) {
            modified = page.getModifiedTime();
            if (!queued)
                fd.dataCache.linkDirtyPageIndex (this);
            else
                fd.dataCache.relink(this);

            firstDirtyPageIndex = lastDirtyPageIndex = idx;
        }
        else if (idx < firstDirtyPageIndex) {
            modified = page.getModifiedTime();
            fd.dataCache.relink(this);
            firstDirtyPageIndex = idx;
        }
        else if (idx > lastDirtyPageIndex) {
            fd.dataCache.linkDirtyPageIndex (this);
            lastDirtyPageIndex = idx;
        } else {
            fd.dataCache.linkDirtyPageIndex (this);
        }
    }

    /**
     *  Returns true if any pages have been freed.
     */
    boolean                        truncate(long length) {
        boolean         pagesHaveBeenFreed = false;

        long             newNumPages = length > fd.getPrivateHeaderLength() ?
                DataCache.getPageIndex (fd, DataCache.PAGE_SIZE + length - 1) : 1;

        long             lastIndex = getLastPageIndex();

        for (long ii = newNumPages; ii <= lastIndex; ii++) {
            Page    page = get(ii);

            if (page != null) {
                fd.dataCache.free(page);
                pagesHaveBeenFreed = true;

                clear(ii);
            }
        }

        if (newNumPages > 0) {
            Page    page = get(newNumPages - 1);

            if (page != null) {
                page.fileTruncated (length);
                modified = page.getModifiedTime();
                fd.dataCache.relink(this);
            }
        }

        shrink (newNumPages);

        return pagesHaveBeenFreed;
    }

    private void                setClean () {
        modified = Long.MIN_VALUE;
        firstDirtyPageIndex = NONE;
        lastDirtyPageIndex = NONE;
    }

//    private void                setClean (int idx) {
//        if (idx == firstDirtyPageIndex)
//            if (idx == lastDirtyPageIndex)
//                setAllClean ();
//            else
//                firstDirtyPageIndex++;
//        else if (idx == lastDirtyPageIndex)
//            lastDirtyPageIndex--;
//        else
//            assert idx > firstDirtyPageIndex && idx < lastDirtyPageIndex:
//                idx + " is out of expected range: " +
//                firstDirtyPageIndex + " .. " + lastDirtyPageIndex;
//    }

//    private void                extend (int minSize) {
//        int         currentSize = pages.length;
//
//        if (currentSize < minSize) {
//            int         newSize = Util.doubleUntilAtLeast (currentSize, minSize);
//            Page []  newArray = new Page [newSize];
//            System.arraycopy (pages, 0, newArray, 0, currentSize);
//            pages = newArray;
//        }
//    }

    private void                 shrink (long newSize) {
//        if (pages != null && pages.length > 50 && pages.length > newSize * 2) {
//            Page []  newArray = new Page [newSize];
//            System.arraycopy (pages, 0, newArray, 0, newSize);
//            pages = newArray;
//        }
        
        if (firstDirtyPageIndex >= newSize)
            setClean ();
        else if (lastDirtyPageIndex >= newSize)
            lastDirtyPageIndex = newSize - 1;

        if (nextPageIndexToSave >= newSize)
            nextPageIndexToSave = NONE;

        lastPageIndex = newSize;
    }

//    void                        ensureCapacity (int size) {
//        if (pages == null)
//            pages = new Page [size];
//        else
//            extend (size);
//    }

    private long                getIndex(long pageIndex) {
        return ((long) id) + (pageIndex << 32);
    }

    void                        clear (long index) {
        long i = getIndex(index);
        Page page = fd.dataCache.pages.remove(i, null);
        if (page != null)
            assert page.fd.pageIndex == this;        
    }

    void                        set (long index, Page page) {
        long i = getIndex(index);
        fd.dataCache.pages.put(i, page);

        if (index > lastPageIndex)
            lastPageIndex = index;
    }

    Page                        get (long index) {
        final long i = getIndex(index);
        Page page = fd.dataCache.pages.get(i, null);
        assert page == null || page.fd.pageIndex == this;

        return page;        
    }

    @Override
    public String               toString () {
        return (
            "PageIndex (fd: " + fd + "; dirty range: " +
            firstDirtyPageIndex + " .. " + lastDirtyPageIndex + ")"
       );
    }

    @Override
    public int                  compareTo(PageIndex index) {
        return MathUtil.compare(modified, index.modified);
    }
}
