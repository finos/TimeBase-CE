package com.epam.deltix.qsrv.dtb.store.dataacc;

/**
 *
 */
public interface DataBlockInfo {
    public int                  getEntity ();
    
    public long                 getEndTime ();

    public long                 getStartTime ();

    public int                  getDataLength ();

    public void                 clear();
}
