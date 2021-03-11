package com.epam.deltix.qsrv.dtb.store.dataacc;

public abstract class AbstractBlockProcessor implements BlockProcessor {

    public void         complete() { }

    public DataBlock    allocate() {
        return new DataBlock();
    }
}
