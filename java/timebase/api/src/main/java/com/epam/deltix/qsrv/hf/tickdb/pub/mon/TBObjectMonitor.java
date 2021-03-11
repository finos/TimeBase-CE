package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

/**
 *
 */
public interface TBObjectMonitor {

    void objectCreated(TBObject obj, int id);

    void objectDeleted(TBObject obj, int id);
}
