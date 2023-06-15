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
package com.epam.deltix.data.stream.pq;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintStream;

/**
 * Binary heap-based priority queue.
 * <p>Optimizations (compared to {@link PriorityQueueExt}):
 *
 * <ul>
 *     <li>We not extract keys from objects when comparing them. Instead we store keys in separate array.</li>
 *
 *     <li>When root removed, we don't update tree structure immediately.
 *     Instead we mark that root is missing and update structure</li>
 * </ul>
 */
@ParametersAreNonnullByDefault
public final class PriorityQueueExt<T> {
    /**
     *  Number of elements in the heap.
     */
    private int                     mSize;

    /**
     *  The heap represented by array. Root has the index of 1.
     *  Left child of node A is at A &lt;&lt; 1. Right child of node
     *  A is at A &lt;&lt; 1 + 1. Parent of node A is at A &gt;&gt; 1.
     */
    private Object[]                heap;
    private long[]                  keys;

    private final byte              direction; // 1 for ascending and -1 for descending
    private boolean                 rootMissing = false; // If true, then the root element should be considered empty.

    public PriorityQueueExt(int capacity, boolean ascending) {
        init (capacity);
        this.direction = (byte) (ascending ? 1 : -1);
    }
    
    public void         dump (PrintStream ps) {
        dump (ps, "", 1);
    }
    
    public void         dump (PrintStream ps, String indent, int pos) {
        T obj = getInternal (pos);
        
        ps.println (indent + pos + ": " + obj);
        
        int             leftChildPos = pos << 1;
        int             rightChildPos = leftChildPos + 1;

        if (leftChildPos <= mSize) 
            dump (ps, indent + "  ", leftChildPos);

        if (rightChildPos <= mSize)
            dump (ps, indent + "  ", rightChildPos);
    }

    private void         init (int numObjects) {
        heap = new Object[numObjects + 1];
        keys = new long[numObjects + 1];
        mSize = 0;
    }
    
    public void         clear () {
        mSize = 0;
        rootMissing = false;
    }
    
    public int          size () {
        return rootMissing ? mSize - 1 : mSize;
    }
        
    public boolean      isEmpty () {
        return mSize == 0 || (rootMissing && mSize == 1);
    }

    /**
     *  Spec: set heap at <code>pos</code> to <code>obj</code>, 
     *  and percolate the element up if necessary.
     */
    private int        percUp(int pos, T obj, long objKey) {
        if (pos > 1) {
            pos = findPosForElementInPercUp(pos, objKey);
        }
        heap [pos] = obj;
        keys [pos] = objKey;
        return pos;
    }

    /**
     * This code is moved to a separate method from {@link #percUp} to allow JIT not inline this method.
     */
    private int findPosForElementInPercUp(int pos, long objKey) {
        // Cache fields
        Object[] heap = this.heap;
        long[] keys = this.keys;
        byte direction = this.direction;

        while (pos > 1) {
            int parentPos = pos >> 1;
            long parentKey = keys[parentPos];


            if (Long.compare(parentKey, objKey) * direction <= 0)
                break;

            /**
             *  Percolate the parent down into the hole
             */
            heap[pos] = heap[parentPos];
            keys[pos] = keys[parentPos];
            pos = parentPos;
        }
        return pos;
    }
    
    /**
     *  Spec: set heap at <code>pos</code> to <code>id</code>, 
     *  and percolate the element down if necessary.
     */
    private int     percDown(int pos, T obj, long objKey) {
        if (pos * 2 <= mSize) {
            pos = findPosForElementInPercDown(pos, objKey);
        }
        
        /**
         *  Plug this hole with the last element.
         */
        heap [pos] = obj;
        keys[pos] = objKey;
        
        return pos;
    }

    /**
     * This code is moved to a separate method from {@link #percDown} to allow JIT not inline this method.
     */
    private int findPosForElementInPercDown(int pos, long objKey) {
        // Cache fields
        int mSize = this.mSize;
        Object[] heap = this.heap;
        long[] keys = this.keys;
        byte direction = this.direction;

        for (; ; ) {
            int leftChildPos = pos << 1;

            if (leftChildPos > mSize)
                break;
            
            /**
             *  Find smallest child of the parent at pos.
             */
            long leftKey = keys[leftChildPos];
            int rightChildPos = leftChildPos + 1;

            int smallestChildPos;
            long smallestChildKey;
            if (rightChildPos <= mSize) {
                // Node has both left and right chilren.
                long rightKey = keys[rightChildPos];

                if (Long.compare(rightKey, leftKey) * direction < 0) {
                    smallestChildPos = rightChildPos;
                    smallestChildKey = rightKey;
                } else {
                    smallestChildPos = leftChildPos;
                    smallestChildKey = leftKey;
                }
            } else {
                // There is no right child
                smallestChildPos = leftChildPos;
                smallestChildKey = leftKey;
            }

            if (Long.compare(objKey, smallestChildKey) * direction <= 0)
                break;

            heap[pos] = heap[smallestChildPos];
            keys[pos] = smallestChildKey;
            pos = smallestChildPos;
        }
        return pos;
    }

    public void         offer (T obj, long objKey) {
        if (rootMissing) {
            putBackRoot(obj, objKey);
        } else {
            offerInternal(obj, objKey);
        }
        //validateStructure();
    }

    /*
    private void validateStructure() {
        for (int i = 2; i < mSize; i++) {
            long key = keys[i];
            int parentPos = i >> 1;
            long parentKey = keys[parentPos];
            if (Long.compare(parentKey, key) * direction > 0) {
                throw new AssertionError("Violated for key=" + i);
            }
        }
    }
    */

    private void         offerInternal (T obj, long objKey) {
        final int               currentCapacity = heap.length;
        
        mSize++;
            
        if (mSize + 1 >= currentCapacity) {
            extendCapacity(currentCapacity);
        }

        /**
         *  Put the element at new leaf location and adjust the heap.
         */
        percUp (mSize, obj, objKey);
    }

    private void putBackRoot(T obj, long objKey) {
        rootMissing = false;
        percDown(1, obj, objKey);
    }

    @SuppressWarnings("unchecked")
    private void extendCapacity(int currentCapacity) {
        int newCapacity = currentCapacity * 2;
        Object[] newHeap = new Object[newCapacity];
        System.arraycopy(heap, 0, newHeap, 0, mSize);
        this.heap = newHeap;
        long[] newKeyArray = new long[newCapacity];
        System.arraycopy(keys, 0, newKeyArray, 0, mSize);
        this.keys = newKeyArray;
    }

    private int          indexOfInternal (T obj) {
        for (int ii = 1; ii <= mSize; ii++)
            if (obj.equals (heap [ii]))
                return (ii);
        
        return (-1);
    }
    
    @SuppressWarnings ("unchecked")
    private T getInternal (int pos) {
        return (T) heap [pos];
    }

    /*
    public MessageSource   get (int idx) {
        return (MessageSource) (getInternal (idx + 1));
    }
    */

    private void         removeInternal (int pos) {
        /**
         *  We are removing element at pos, so we are going to have a hole there.
         *  Put the last element in the hole and adjust the heap.
         */

//        ByteArrayOutputStream buf = new ByteArrayOutputStream();
//        dump(new PrintStream(buf));

        T last = getInternal (mSize);
        long lastKey = keys[mSize];

        heap [mSize] = null;
        mSize--;

        if (percDown(pos, last, lastKey) == pos)
            percUp(pos, last, lastKey);
    }


    /**
     * Similar to {@link #removeInternal} but for root position only (pos=0).
     */
    private void removeRoot() {
        assert mSize > 0;
        // Take tha last tree element
        T last = getInternal(mSize);
        long lastKey = keys[mSize];
        heap[mSize] = null;
        mSize--;

        // Push down that element from root position
        percDown(1, last, lastKey);
    }

    public boolean      remove (T obj) {
        if (rootMissing) {
            removeRoot();
            rootMissing = false;
        }

        int         pos = indexOfInternal (obj);

        if (pos > 0) {
            removeInternal (pos);
            return (true);
        }

        return (false);
    }

    @Nullable
    public T poll () {
        if (isEmpty()) {
            return null;
        }
        if (rootMissing) {
            // Perform delayed root deletion
            removeRoot();
        }
        rootMissing = true;
        return getInternal (1);
    }

    /**
     * @return returns current key of root.
     */
    public long peekKey () {
        if (isEmpty()) {
            throw new IllegalStateException();
        }
        if (rootMissing) {
            // Perform delayed root deletion
            removeRoot();
            rootMissing = false;
        }
        return keys[1];
    }
}