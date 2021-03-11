package com.epam.deltix.qsrv.testsetup;

import java.util.Iterator;
import java.util.List;

public class CycleIterator<T> {
    private List<T> data;
    private Iterator<T> it;
    private boolean reseted;

    public CycleIterator(List<T> data) {
        this.data = data;
    }

    public boolean isReseted() {
        return reseted;
    }

    public T next() {
        reseted = it != null && !it.hasNext();

        if (it == null || !it.hasNext())
            it = data.iterator();

        return it.next();
    }
}
