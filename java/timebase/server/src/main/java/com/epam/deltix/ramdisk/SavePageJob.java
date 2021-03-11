package com.epam.deltix.ramdisk;

/**
 *
 */
final class SavePageJob {

    SavePageJob(WriterThread writer) {
        this.writer = writer;
    }

    Page                page;
    FD                  fd;
    long                address;
    final byte []       data = new byte [DataCache.PAGE_SIZE];
    int                 length;

    /**
     *  Whether at the time of assigning the job all pages prior to the current
     *  one were clean. This flag triggers the clean commit event handler.
     */
    boolean             followsCleanRange;

    /**
     *  Whether at the time of assigning this job the current page was the last one
     *  in the dirty range. Note that isLastDirtyPage does not imply
     *  followsCleanRange, as pages prior to the current one may have become
     *  dirty. This flag triggers the auto-commit logic (if enabled).
     */
    boolean             isLastDirtyPage;

    WriterThread        writer;
}
