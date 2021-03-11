package com.epam.deltix.ramdisk;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.NotificationHandler;
import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.*;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.logging.Level;

import static com.epam.deltix.ramdisk.RAMDisk.Properties;

/**
 *
 */
final class DataCache {
    private static final boolean    DEBUG_GET_JOB = false;

    static final int            PAGE_SIZE_LOG2 = 16; // 65K
    static final int            PAGE_SIZE = 1 << PAGE_SIZE_LOG2;
    static final long           PAGE_SIZE_MASK = ~(PAGE_SIZE - 1);
    static final long           PAGE_SIZE_WITH_OVERHEAD = Page.OVERHEAD + PAGE_SIZE;

    static final int            MB = 1024 * 1024;
    
    public static final long    MAX_FILE_LENGTH = 
        ((long) Integer.MAX_VALUE) << PAGE_SIZE_LOG2;

    private QuickList <Page>            free = new QuickList <Page> ();
    private QuickList <Page>            clean = new QuickList <Page> ();
    private PriorityQueue<PageIndex>    dirtyFiles = new PriorityQueue<PageIndex> ();    
    private WriterThread                writer;
    private long                        pagesCanAllocate;
    private long                        numAllocatedPages = 0;
    //private long                        numFreePages = 0;

    private long                        shutdownTimeout = Long.MAX_VALUE;
    private boolean                     shutdownInProgress = false;

    private int                         maxRetryAttempts = Integer.getInteger("RAMDisk.maxRetryAttempts", 1000);

    private final QuickList <PageContainer>   pool = new QuickList <PageContainer> ();

    // Pages mapping (FD + Index) -> Page
    final LongToObjectHashMap<Page>     pages = new LongToObjectHashMap<Page>();

    // SMTP support
    NotificationHandler                 handler;

    DataCache (long numPages, long initial) {
        pagesCanAllocate = numPages;

        initial = Math.min(numPages, initial);

        for (int i = 0; i < initial; i++) {
            Page page = new Page (PAGE_SIZE);
            numAllocatedPages++;
            pagesCanAllocate--;

            page.mappedToFree();
            free.linkLast(page);
        }
    }

    void                        setHandler(NotificationHandler handler) {
        this.handler = handler;

        handler.propertyChanged(Properties.cacheSize, pagesCanAllocate * PAGE_SIZE / MB);
        handler.propertyChanged(Properties.usedCacheSize, numAllocatedPages * PAGE_SIZE / MB);
        handler.propertyChanged(Properties.numPages, pagesCanAllocate);
        handler.propertyChanged(Properties.numOpenedFiles, 0);
        handler.propertyChanged(Properties.bytesWritten, 0);
        handler.propertyChanged(Properties.bytesRead, 0);
        handler.propertyChanged(Properties.queueLength, 0);
        handler.propertyChanged(Properties.writerState, "");
    }

    synchronized void           getStats (GlobalStats out) {
        out.numAllocPages = numAllocatedPages;
        out.numPages = pagesCanAllocate + numAllocatedPages;
    }

    public void                 start () {
        if (writer == null || !writer.isAlive()) {
            (writer = new WriterThread (this)).start();

            // assuming that listeners added on initialization
            if (handler.hasListeners())
                handler.propertyChanged(Properties.writerState, writer.getState().toString());
        }
        else
            throw new IllegalStateException("Already started");
    }

    public void                 shutdownNoWait () {
        if (writer != null)
            writer.shutdown ();

        // assuming that listeners added on initialization
        if (handler.hasListeners())
            handler.propertyChanged(Properties.writerState, "");
        
        writer = null;
    }

    public void                 shutdownAndWait () throws InterruptedException {
        
        if (writer != null) {
            writer.shutdown ();
            writer.join ();
        }
        writer = null;

        shutdownInProgress = false;
    }

    static long                  getPageIndex (FD fd, long address) {
        long        h = fd.getPrivateHeaderLength ();

        assert address >= h :
            "Address " + address +
            " is in the private header range (" + h + ") for " + fd;

        assert address < DataCache.MAX_FILE_LENGTH :
            "Address " + address +
            " is too large (max " + DataCache.MAX_FILE_LENGTH + ")";

        long        n = address - h;

        return (n >> PAGE_SIZE_LOG2);
    }

    static int                  getLastPageIndex (FD fd) {
        long        len = fd.getLogicalLength ();

        assert len <= DataCache.MAX_FILE_LENGTH :
            fd + " has too high logical length " + len +
            " (max " + DataCache.MAX_FILE_LENGTH + ")";

        long        h = fd.getPrivateHeaderLength ();
        long        n = len - h;

        if (n == 0)
            return (-1);
        
        assert n > 0 :
            fd + ": logical length " + len +
            " is shorter than private header length " + h;

        return ((int) ((n - 1) >> PAGE_SIZE_LOG2));
    }

    private Page                getFreePage (
        FD                          fd,
        long                        address,
        byte                        targetState
    )
    {
        Page page = free.getFirst ();

        if (page != null) {
            page.freeToCheckout (fd, address, targetState);
            return (page);
        }

        if (pagesCanAllocate > 0) {
            page = new Page (PAGE_SIZE);
            numAllocatedPages++;
            pagesCanAllocate--;

            // assuming that listeners added on initialization
            if (handler.hasListeners())
                handler.propertyChanged(Properties.usedCacheSize, numAllocatedPages * PAGE_SIZE / MB);

            page.freeToCheckout (fd, address, targetState);
            return (page);
        }

        return (null);
    }

    PageContainer               checkOutContainer() {
        synchronized (pool) {
            PageContainer free = pool.getFirst();
            if (free == null)
                return new PageContainer();

            free.unlink();
            return free;
        }
    }

    void                       checkIn(PageContainer c) {
        synchronized (pool) {
            pool.linkLast(c);
            c.checkedOutPage = null;
        }
    }

    private Page                checkOutPageForTakeover (
        FD                          fd,
        long                        address,
        byte                        targetState
    )
        throws InterruptedException
    {
        assert Thread.holdsLock (this);

        Page        page;
        
        for (;;) {
            page = getFreePage (fd, address, targetState);

            if (page != null) 
                return (page);

            page = clean.getFirst ();

            if (page != null) {
                //numFreePages--;
                page.unlink();
                page.takeover (fd, address, targetState);
                
                return (page);
            }

            RAMDisk.LOGGER.fine ("Out of clean pages.");
            wait ();
        }
    }

    private static void         assertPageMapped (FD fd, long srcAddress, Page p) {
        assert p.fd == fd : p + " is wrongly in the index of " + fd;

        if (srcAddress > fd.getPrivateHeaderLength()) {
            int validLength = p.validLength;
            assert validLength != Page.UNLOADED : p + " in the index but not loaded";
        
            assert
                p.getStartAddress () <= srcAddress &&
                p.getEndAddress () >= srcAddress :
                    p + " is wrong for address " + srcAddress;
        }
    }

    public synchronized byte    read (FD fd, long srcAddress, PageContainer c)
        throws InterruptedException
    {
        long                        pi = getPageIndex (fd, srcAddress);
        Page                        page = fd.pageIndex.get (pi);

        c.checkedOutPage = page == null ? checkOutPageForTakeover (fd, srcAddress, Page.READING) : null;

        if (page != null) {
            assertPageMapped (fd, srcAddress, page);

            return page.read (srcAddress);
        }

        return -1;
    }

    public synchronized int     read (
        FD                          fd,
        long                        srcAddress,
        byte []                     dest,
        int                         destOffset,
        int                         length,
        PageContainer               c
    )
        throws InterruptedException
    {
        long                        pi = getPageIndex (fd, srcAddress);
        Page                        page = fd.pageIndex.get (pi);

        c.checkedOutPage = page == null ? checkOutPageForTakeover (fd, srcAddress, Page.READING) : null;

        if (page != null) {
            assertPageMapped (fd, srcAddress, page);

            return page.read (srcAddress, dest, destOffset, length);
        }

        return -1;
    }

    public synchronized void    write (
        FD                          fd,
        long                        destAddress,
        byte                        b,
        PageContainer               c
    )
        throws InterruptedException
    {
        assert writer != null : "Data Cache not started";

        long                        pi = getPageIndex (fd, destAddress);
        Page                        page = fd.pageIndex.get (pi);

        c.checkedOutPage = page == null ? checkOutPageForTakeover (fd, destAddress, Page.WRITING) : null;

        if (page != null) {
            assertPageMapped (fd, destAddress, page);

            page.mappedToDirty ();
            page.write(destAddress, b);

            writer.wakeUp ();
        }
    }

    public synchronized int     write (
        FD                          fd,
        long                        destAddress,
        byte []                     src,
        int                         srcOffset,
        int                         length,
        PageContainer               c
    )
        throws InterruptedException
    {
        assert writer != null : "Data Cache not started";
        
        long                        pi = getPageIndex (fd, destAddress);
        Page                        page = fd.pageIndex.get (pi);

        c.checkedOutPage = page == null ? checkOutPageForTakeover (fd, destAddress, Page.WRITING) : null;

        if (page != null) {
            assertPageMapped (fd, destAddress, page);

            page.mappedToDirty();

            int                         ret =
                    page.write (destAddress, src, srcOffset, length);

            writer.wakeUp ();
            return (ret);
        }

        return -1;
    }

    synchronized void           checkIn (Page page) {
        if (page.checkIn ()) {
            clean.linkLast (page);
            notify ();
        } else {
            linkDirtyPageIndex(page.fd.pageIndex);
        }
    }

    synchronized Page           checkOutPageForWarmUp (FD fd, Page previousPage)
        throws NoFreePagesException
    {
        final PageIndex     pi = fd.pageIndex;
        final long          length = fd.getLogicalLength ();
        long                address =
            previousPage == null ?
                fd.getPrivateHeaderLength () :
                previousPage.getEndAddress ();

        for (; address < length; address += PAGE_SIZE) {
            if (pi.get (getPageIndex (fd, address)) == null) {
                Page        page = getFreePage (fd, address, Page.READING);

                if (page == null)
                    throw new NoFreePagesException ();

                return (page);
            }
        }

        return (null);
    }

    synchronized void           beingClosed (FD fd, boolean checkSave) {
        PageIndex       pi = fd.pageIndex;
        boolean         hadUnsavedData = checkSave && pi.hasUnsavedPages ();
        boolean         pagesHaveBeenFreed = false;

        Iterator<Page> it = this.pages.iterator();
        while (it.hasNext()) {
            Page page = it.next();

            if (page != null && page.fd == fd) {
                page.mappedToFree();
                free.linkLast(page);
                //numFreePages++;
                pagesHaveBeenFreed = true;
            }
        }

        fd.pageIndex = null;

        if (hadUnsavedData && RAMDisk.LOGGER.isLoggable (Level.WARNING))
            RAMDisk.LOGGER.log (
                Level.WARNING,
                fd.getFile () + " was closed with unsaved data",
                new Throwable ("Location")
            );

        if (pagesHaveBeenFreed)
            notify ();
    }

    synchronized void           truncating (FD fd, long newLength) {
        if (fd.pageIndex.truncate(newLength))
            notify();
    }

    synchronized void           startShutdown () {
        shutdownInProgress = true;
    }

    synchronized void           waitForFlush (FD fd) 
        throws IOException, InterruptedException
    {
        long timeout = shutdownInProgress ? shutdownTimeout : 0;

        long time = TimeKeeper.currentTime;
        while (fd.pageIndex.hasUnsavedPages ()) {
            wait (timeout);

            if (shutdownInProgress) {
                shutdownTimeout -= (TimeKeeper.currentTime - time);

                if (shutdownTimeout <= 0) {
                    shutdownTimeout = 1;
                    break;
                }
            }
        }

        if (fd.pageIndex.hasUnsavedPages ())
            RAMDisk.LOGGER.severe(fd + ": closed without flushing all data");
    }

    synchronized void           pageNotSaved (Page page, long address, long length) {
        page.savingFailed(address);
        page.mappedToDirty ();
        
        page.writer = null;

        onError();
    }

    synchronized void           onError() {
        maxRetryAttempts--;
        if (maxRetryAttempts <= 0)
            throw new IllegalStateException("Number of maximum retries exceeded.");
    }

    synchronized boolean        pageSaved (SavePageJob job) {
        Page page = job.page;

        if (!job.writer.equals(page.writer))
            return false;

        // reset writer
        page.writer = null;

        if (page.savingToClean ()) {
            clean.linkLast (page);
            //numFreePages++;
            
            notify ();
        } else {
            linkDirtyPageIndex(page.fd.pageIndex);
        }

        return true;
    }

    synchronized long        getNextJobTime (SavePageJob job) {

        long jobTime = job.fd != null && job.fd.pageIndex != null ?
                job.fd.pageIndex.modified : Long.MAX_VALUE;
        
        PageIndex next = dirtyFiles.peek();
        long time = next != null ? next.modified : Long.MAX_VALUE;

//        if (jobTime > time && next != null) {
//            job.fd = next.getFD();
//            dirtyFiles.poll();
//        }

        return Math.min(time, jobTime);
    }

    synchronized boolean        getNextJob (SavePageJob job) {
        for (;;) {
            if (job.fd != null) {
                PageIndex       pi = job.fd.pageIndex;
                
                //
                //  Check pi != null, in case the file was closed between
                //  allocating the job and this call.
                //
                if (pi != null && pi.getNextJob(job)) {
                    if (DEBUG_GET_JOB)
                        System.out.println ("GET JOB: returning " + job.page);

                    return (true);
                }
            }

            //  Done with this file, move to next.
            PageIndex index = dirtyFiles.poll();

            // assuming that listeners added on initialization
            if (handler.hasListeners())
                handler.propertyChanged(Properties.queueLength, dirtyFiles.size());
            
            if (index == null) {
                if (DEBUG_GET_JOB)
                    System.out.println ("GET JOB: no dirty files.");

                assert dirtyFiles.isEmpty () :
                    "curpi == null but !dirtyFiles.isEmpty ()";

                return (false);
            }

            job.fd = index.getFD ();

            if (job.fd.pageIndex != null && job.fd.pageIndex.hasUnsavedPages()) {
                job.fd.pageIndex.startSaving ();
                job.fd.pageIndex.queued = false;
                job.followsCleanRange = false;
            } else {
                job.fd = null;
            }

            if (DEBUG_GET_JOB)
                System.out.println ("GET JOB: use " + job.fd + "; next: " + index);
        }
    }

    void                        free(Page page) {
        page.mappedToFree ();
        free.linkLast (page);
        //numFreePages++;
    }

    void                     linkDirtyPageIndex (PageIndex pi) {
        assert Thread.holdsLock (this);
        
        if (!pi.queued) {
            assert !dirtyFiles.contains(pi): "dirtyFiles already contains PageIndex: "  + pi;

            dirtyFiles.add(pi);
            pi.queued = true;

            // assuming that listeners added on initialization
            if (handler.hasListeners())
                handler.propertyChanged(Properties.queueLength, dirtyFiles.size());

            writer.wakeUp ();
        } else {
            assert dirtyFiles.contains(pi);
        }
    }

    void                        relink(PageIndex pi) {
        assert Thread.holdsLock (this);

        if (pi.queued)
            dirtyFiles.remove(pi);

        assert !dirtyFiles.contains(pi): "dirtyFiles contains such PageIndex";
        
        dirtyFiles.add(pi);
        pi.queued = true;

        // assuming that listeners added on initialization
        if (handler.hasListeners())
            handler.propertyChanged(Properties.queueLength, dirtyFiles.size());

        writer.wakeUp ();
    }

    public void setShutdownTimeout(long timeout) {
        this.shutdownTimeout = timeout;
    }

//    void                        checkLists() {
//        checkFreePages();
//        checkCleanPages();
//    }
//
//    void                        checkFreePages() {
//        Page page = free.getFirst();
//        while (page != null && page.next() != null) {
//            byte state = page.getState();
//            assert state == Page.FREE || state == Page.NEW : page + " is NOT FREE or NEW";
//            page = page.next();
//        }
//
//        page = free.getLast();
//        while (page != null && page.previous() != null) {
//            byte state = page.getState();
//            assert state == Page.FREE || state == Page.NEW : page + " is NOT FREE or NEW";
//            page = page.previous();
//        }
//    }
//
//    void                        checkCleanPages() {
//        Page page = clean.getFirst();
//        while (page != null && page.next() != null) {
//            assert page.getState() == Page.CLEAN : page + " is not CLEAN";
//            page = page.next();
//        }
//
//        page = clean.getLast();
//        while (page != null && page.previous() != null) {
//            assert page.getState() == Page.CLEAN : page + " is not CLEAN";
//            page = page.previous();
//        }
//    }
}