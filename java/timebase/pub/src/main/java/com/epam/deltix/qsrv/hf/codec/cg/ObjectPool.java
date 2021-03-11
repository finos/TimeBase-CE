package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 * Pool of objects of the same type. Thread-unsafe. Reset makes all allocated instances available again.
 */
public abstract class ObjectPool<T> {

    private final ObjectArrayList<T> pool;
    private int availIdx = 0;

    public ObjectPool() {
        this(5);
    }

    protected ObjectPool(int capacity) {
        pool = new ObjectArrayList<>(capacity);
    }

    public abstract T       newItem();

    public final T          borrow() {
        if (availIdx < pool.size())
            return pool.get(availIdx++);
        else {
            availIdx = Integer.MAX_VALUE; // give up on reusage until reset
            final T item = newItem();
            pool.add(item);
            return item;
        }
    }

    public final void       reset() {
        availIdx = 0;
    }
}
