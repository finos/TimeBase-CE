package com.epam.deltix.qsrv.dtb.store.dataacc;

/**
 *  Thrown when a writer attempts to insert into a block, but there is no room.
 *  By the time this exception is thrown, the previous time slice has been 
 *  checked in (for the accessor), and a new time slice, contained in this
 *  exception, has been checked out to the accessor.
 */
public class SwitchTimeSliceException extends RuntimeException {
    public final TimeSlice          newTimeSlice;

    public SwitchTimeSliceException (TimeSlice newTimeSlice) {
        this.newTimeSlice = newTimeSlice;
    }
        
    @Override
    public Throwable                fillInStackTrace () {
        return (null);
    }
}
