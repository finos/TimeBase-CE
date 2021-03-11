package com.epam.deltix.qsrv.dtb.store.dataacc;

/**
 *
 */
public interface BlockProcessor {

    public void         process (DataBlock block);

    public void         complete();

    public DataBlock    allocate();
}
