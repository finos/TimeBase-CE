package com.epam.deltix.qsrv.dtb.store.pub;

public class EntityTimeRange extends TimeRange {

    public EntityTimeRange(int entity) {
        this.entity = entity;
    }

    public int entity;
}
