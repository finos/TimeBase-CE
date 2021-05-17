/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
