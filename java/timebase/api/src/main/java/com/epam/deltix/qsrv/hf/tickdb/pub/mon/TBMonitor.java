package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

/**
 *
 */
public interface TBMonitor {
    public boolean                  getTrackMessages ();

    public void                     setTrackMessages (boolean value);

    public TBCursor []              getOpenCursors ();

    public TBLoader []              getOpenLoaders ();

    public TBLock[]                 getLocks();

    public TBObject                 getObjectById (long id);

    public void                     addObjectMonitor(TBObjectMonitor monitor);

    public void                     removeObjectMonitor(TBObjectMonitor monitor);

    public void                     addPropertyMonitor(String component, PropertyMonitor monitor);
}