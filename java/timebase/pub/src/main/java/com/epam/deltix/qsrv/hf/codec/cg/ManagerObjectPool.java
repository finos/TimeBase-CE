/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.collections.ICapacity;
import com.epam.deltix.containers.BinaryArray;

import java.lang.reflect.InvocationTargetException;


public final class ManagerObjectPool {

    private Object[]    pool;
    private boolean[]   usedStates; //false - not used; true - used;

    private int         size;
    private boolean     isClean;

    public ManagerObjectPool() {
        pool = new Object[10];
        usedStates = new boolean[10];
        size = 0;
        isClean = true;
    }

    public void clean() {
        if (!isClean) {
            for (int i = 0; i < size; i++)
                usedStates[i] = false;
        }
    }

    public Object use(Class<?> clazz, int capacity) {
        isClean = false;
        final int index = binarySearch(pool, capacity);
        if (index >= 0 && index < pool.length) {
            for (int i = index; i < size; i++) {
                if (!usedStates[i]) {
                    usedStates[i] = true;
                    return pool[i];
                }
            }
            return createObject(clazz,capacity);
        }
        else {
            for (int i = Math.abs(index) - 1; i < size; i++) {
                if (!usedStates[i]) {
                    usedStates[i] = true;
                    return pool[i];
                }
            }
            return createObject(clazz,capacity);
        }
    }

    private Object createObject(Class<?> clazz, int capacity) {
        Object object;
        try {
            object = clazz.getConstructor(int.class).newInstance(capacity);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        addObject(object);
        return object;
    }

    private void addObject(Object object) {
        int index = binarySearch(pool, getCapacity(object));
        if (size == pool.length)
            increaseLength();

        if (index >= size - 1) { // the most right element
            pool[index + 1] = object;
            usedStates[index + 1] = true;
        } else if(index < size && index >= 0) {// element in the range [0..size-1]
            System.arraycopy(pool, index + 1, pool, index + 2, size - index - 1);
            System.arraycopy(usedStates , index + 1, usedStates, index + 2, size - index - 1);
            pool[index + 1] = object;
            usedStates[index + 1] = true;
        } else {//not found element
            index = Math.abs(index) - 1;
            if (size - index > 0) {
                System.arraycopy(pool, index, pool, index + 1, size - index);
                System.arraycopy(usedStates, index, usedStates, index + 1, size - index);
            }
            pool[index] = object;
            usedStates[index] = true;
        }
        size++;
    }

    private void increaseLength(){
        Object oldData[] = pool;
        boolean oldStates[] = usedStates;
        int newCapacity = (pool.length * 3) / 2 + 1;
        pool = new Object[newCapacity];
        usedStates = new boolean[newCapacity];
        System.arraycopy (oldData, 0, pool, 0, size);
        System.arraycopy (oldStates, 0, usedStates, 0, size);
    }


    public int binarySearch(Object[] a, int key) {
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;

            int midVal = getCapacity(a[mid]);
            int cmp = Integer.compare(midVal, key);
            int cmpBorder = Integer.compare(midVal, getCapacity(a[low]));
            if (cmpBorder == 0 && cmp == 0 ) {
                if (!usedStates[low])
                    return low;
                else {
                    if (!usedStates[mid])
                        high = mid;
                    else if (usedStates[low])
                        low = mid + 1;
                }
            }
            else if (cmpBorder > 0 && cmp == 0)
                high = mid ;
            else if (cmpBorder < 0 && cmp == 0)
                low = mid ;
            else
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    private int     getCapacity(Object obj) {
        if (obj instanceof BinaryArray)
            return ((BinaryArray)obj).getCapacity();

        return ((ICapacity) obj).capacity();
    }
}