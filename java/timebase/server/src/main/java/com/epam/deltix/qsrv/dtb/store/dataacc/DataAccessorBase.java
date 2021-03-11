package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.qsrv.dtb.store.pub.*;
import net.jcip.annotations.GuardedBy;

/**
 *  Lock ordering: this, then current time slice.
 */
public abstract class DataAccessorBase implements DataAccessor, DAPrivate {  
    protected TimeSliceStore                      store;

    @GuardedBy("this")
    protected TimeSlice                           currentTimeSlice;    
    
    public DataAccessorBase () {
    }

    //
    //  DataAccessor IMPLEMENTATION
    //
    @Override
    public synchronized void                 associate (TSRoot store) {
        this.store = (TimeSliceStore) store;
    }

    public synchronized  void   associate (TimeSlice slice) {
        this.currentTimeSlice = slice;
    }
    //
    //  INTERNALS
    //  
    protected void              assertOpen () {
        if (currentTimeSlice == null)
            throw new IllegalStateException ("not open");
    }

    @Override
    public synchronized void    close () {
        currentTimeSlice = null;
    }

    public abstract AccessorBlockLink       getBlockLink (int entity, long ffToTimestamp);

    public abstract AccessorBlockLink       getBlockLink (int entity, DataBlock block);
    
}
