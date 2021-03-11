package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.util.collections.generated.IntegerHashSet;

public class ListEntityFilter implements EntityFilter {

    final IntegerHashSet entities;

    public ListEntityFilter(int[] entities) {
        this.entities = new IntegerHashSet(entities);
    }

    @Override
    public boolean acceptAll() {
        return false;
    }

    @Override
    public boolean accept(int entity) {
        return entities.contains(entity);
    }

    @Override
    public boolean restrictAll() {
        return entities.size() == 0;
    }

    @Override
    public long             acceptFrom(int entity) {
        return Long.MIN_VALUE;
    }
}
