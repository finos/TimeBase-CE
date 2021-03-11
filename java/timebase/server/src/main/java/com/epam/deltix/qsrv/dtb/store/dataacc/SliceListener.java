package com.epam.deltix.qsrv.dtb.store.dataacc;

public interface SliceListener {

    void        checkoutForInsert(TimeSlice slice);

    void        checkoutForRead(TimeSlice slice);
}
