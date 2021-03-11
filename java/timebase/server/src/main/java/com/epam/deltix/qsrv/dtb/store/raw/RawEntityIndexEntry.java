package com.epam.deltix.qsrv.dtb.store.raw;

/**
 *
 */
public class RawEntityIndexEntry {
    private final int               entity;
    int                             firstChildIdx;
    int                             lastChildIdx;

    public RawEntityIndexEntry (int entity, int firstChildIdx, int lastChildIdx) {
        this.entity = entity;
        this.firstChildIdx = firstChildIdx;
        this.lastChildIdx = lastChildIdx;
    }

    public int                  getEntity () {
        return entity;
    }

    public int                  getFirstChildIdx () {
        return firstChildIdx;
    }

    public int                  getLastChildIdx () {
        return lastChildIdx;
    }        
}
