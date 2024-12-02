package com.epam.deltix.qsrv.hf.blocks;

public abstract class ObjectConcurrentPool<T> {

    private final Object[] array;
    private int size;

    public ObjectConcurrentPool(final int size, final int capacity) {
        if (size < 0 || size > capacity) {
            throw new IllegalArgumentException("size is negative or more capacity");
        }

        final Object[] array = new Object[capacity];

        for (int i = 0; i < size; i++) {
            array[i] = newItem();
        }

        this.size = size;
        this.array = array;
    }

    @SuppressWarnings("unchecked")
    public T borrow() {
        synchronized (array) {
            if (size > 0) {
                return (T) array[--size];
            }
        }

        return newItem();
    }

    public void release(final T object) {
        if (object != null) {
            synchronized (array) {
                if (size < array.length) {
                    array[size++] = object;
                }
            }
        }
    }

    public void trim() {
        // NoOp
    }

    protected abstract T newItem();

}
