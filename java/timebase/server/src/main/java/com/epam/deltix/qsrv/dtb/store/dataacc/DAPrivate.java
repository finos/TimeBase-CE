package com.epam.deltix.qsrv.dtb.store.dataacc;

/**
 *
 */
public interface DAPrivate {
    public long                 getCurrentTimestamp ();

    public void                 asyncDataInserted (
            DataBlock               db,
            int                     dataOffset,
            int                     msgLength,
            long                    timestamp
    );

    public void                 asyncDataDropped (
            DataBlock               db,
            int                     dataOffset,
            int                     msgLength,
            long                    timestamp
    );

    void                        checkedOut(TimeSlice slice);
}
