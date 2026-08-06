package com.epam.deltix.util.collections;

import com.epam.deltix.util.collections.generated.IntegerList;

import java.util.AbstractList;
import java.util.Arrays;

public class IntegerRingedList extends AbstractList<Integer> implements IntegerList {
    private static final int MAX_ARRAY_LENGTH = 2147483639;
    protected int[] array;
    private int first;
    private int size;

    public IntegerRingedList() {
        this(16);
    }

    public IntegerRingedList(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Initial capacity (" + capacity + ") is negative");
        } else {
            this.array = new int[capacity];
            this.first = this.size = 0;
        }
    }

    public int size() {
        return this.size;
    }

    public Integer get(int index) {
        return this.getInteger(index);
    }

    public int getInteger(int index) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException();
        } else {
            return this.getIntegerNoRangeCheck(index);
        }
    }

    public int getIntegerNoRangeCheck(int index) {
        index += this.first;
        index = index >= this.array.length ? index - this.array.length : index;
        return this.array[index];
    }

    public boolean add(int x) {
        this.ensureFreeSpace(1);
        int index = this.first + this.size++;
        index = index >= this.array.length ? index - this.array.length : index;
        this.array[index] = x;
        return true;
    }

    public void set(int index, int element) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException();
        } else {
            index += this.first;
            index = index >= this.array.length ? index - this.array.length : index;
            this.array[index] = element;
        }
    }

    public int first() {
        return this.array[this.first];
    }

    public int last() {
        return this.get(this.size - 1);
    }

    public int pop() {
        int res = this.array[this.first++];
        this.first = this.first >= this.array.length ? this.first - this.array.length : this.first;
        --this.size;
        return res;
    }

    public boolean contains(int elem) {
        return this.indexOf(elem) >= 0;
    }

    public int indexOf(int elem) {
        throw new UnsupportedOperationException();
    }

    public int lastIndexOf(int elem) {
        throw new UnsupportedOperationException();
    }

    public int[] toIntArray() {
        int[] result = new int[this.size];
        this.toArray(result, 0);
        return result;
    }

    public void toArray(int[] data, int offset) {
        if (this.size > data.length - offset) {
            throw new IllegalArgumentException();
        } else {
            int i = 0;

            for(int j = this.first; i < this.size; ++i) {
                data[i] = this.array[j++];
                j = j == this.array.length ? 0 : j;
            }

        }
    }

    private void ensureFreeSpace(int required) {
        long requiredCapacity = (long)this.size + (long)required;
        this.ensureCapacity(requiredCapacity);
    }

    public void ensureCapacity(long minCapacity) {
        if (minCapacity > (long)this.array.length) {
            this.extend(minCapacity);
        }

    }

    private void extend(long requiredCapacity) {
        if (requiredCapacity > 2147483639L) {
            throw new OutOfMemoryError("required capacity=" + requiredCapacity + " exceeds max");
        } else {
            int newCapacity = (int)Math.min(2147483639L, requiredCapacity + (requiredCapacity >>> 1));
            int[] newArray = new int[newCapacity];
            System.arraycopy(this.array, this.first, newArray, 0, this.array.length - this.first);
            if (this.first > 0) {
                System.arraycopy(this.array, 0, newArray, this.array.length - this.first, this.first);
            }

            this.first = 0;
            this.array = newArray;
        }
    }

    public void clear() {
        this.first = this.size = 0;
    }

    public void setSize(int newSize) {
        this.ensureCapacity((long)newSize);
        this.size = newSize;
    }

    public void sort() {
        this.array = this.toIntArray();
        this.first = 0;
        Arrays.sort(this.array, 0, this.size);
    }
}