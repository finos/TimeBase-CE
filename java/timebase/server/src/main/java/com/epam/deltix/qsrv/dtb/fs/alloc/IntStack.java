package com.epam.deltix.qsrv.dtb.fs.alloc;

/**
 * Stack of int values (minimized implementation)
 */
final class IntStack {
    private final int[] stack;
    private int size; // points to the next

    IntStack(int maxCapacity) {
        this.stack = new int[maxCapacity];
    }

    void push(int value) {
        stack[size++] = value;
    }

    int pop() {
        return stack[--size];
    }

    int size() {
        return size;
    }

    boolean remove(int value) {
        for (int i = 0; i < size; i++) {
            if (stack[i] == value) {
                while (++i < size)
                    stack[i - 1] = stack[i];
                size--;
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(stack[i]);
        }
        sb.append(']');

        return sb.toString();
    }
}
