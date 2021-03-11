package com.epam.deltix.qsrv.hf.blocks;

import java.util.ArrayList;

import net.jcip.annotations.GuardedBy;

/**
 * Object pool. Thread-safe with exception of unit-test-only methods marked otherwise.
 * @author Andy
 *         Date: Sep 22, 2009 10:29:44 PM
 */
public abstract class ObjectPool<T> {
    // buffer will shrink by half of extra space
    static final int SHRINK_FACTOR = 2;
    static final int TRIM_FACTOR = 2;

    @GuardedBy("pool")
    private final ArrayList<T> pool;
    @GuardedBy("pool")
    private int minSize;
    @GuardedBy("pool")
    private int maxSize;

    private final int limit;

    public ObjectPool(int initialCapacity) {
        this(initialCapacity, Integer.MAX_VALUE);
    }

    public ObjectPool(int initialCapacity, int limitCapacity) {
        pool = new ArrayList<T>(initialCapacity);
        for (int i = 0; i < initialCapacity; i++)
            pool.add (newItem());
        minSize = maxSize = initialCapacity;

        limit = limitCapacity;
    }

    public final T borrow () {
        synchronized (pool) {
            int size = pool.size();
            if (size == 0) {
                minSize = 0;
                maxSize ++; // lets account for future release (maxSize is only used to avoid trimToSize())
                return newItem();
            } else {
                size--;
                if (size < minSize)
                    minSize = size;
                return pool.remove(size);
            }
        }
    }

    protected abstract T newItem();

    public final boolean release(T msg) {

        if (msg != null) {
            synchronized (pool) {
                int size = pool.size();

                boolean add = size < limit;
                if (add) {
                    pool.add(msg);
                    size++;
                }

                if (size > maxSize)
                    maxSize = size;

                return add;
            }
        }

        return false;
    }


    public final void trim() {
        synchronized (pool) {
            if (minSize > 0) {
                final int size = pool.size();

                // buffer has at least minSize elements that were not used since last trim()
                final int shrinkSize = (size - minSize / SHRINK_FACTOR);

                // yet up to (maxSize - size) may be released soon, so better keep ArrayList capacity
                if (maxSize / size > TRIM_FACTOR) {
                    pool.trimToSize();
                    maxSize = shrinkSize;
                }

                for (int i = size - 1; i >= shrinkSize; i--)
                    pool.remove(i);

                minSize = shrinkSize; // reset
            }
        }
    }

    /** NO SYNCHRONIZATION: TEST ONLY */
    int size() {
        return pool.size();
    }

    int minSize() {
        return minSize;
    }

    int maxSize() {
        return maxSize;
    }
}