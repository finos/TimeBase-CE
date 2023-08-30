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
package com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.PriorityQueue;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Comparator;

/**
 * Modified version of {@link PriorityQueue} that works with {@link PrioritizedSource} instead of {@link MessageSource}.
 */
final class MSPriorityQueue<T> {
    /**
     *  Number of elements in the heap.
     */
    private int                     mSize;

    /**
     *  The heap represented by array. Root has the index of 1.
     *  Left child of node A is at A &lt;&lt; 1. Right child of node
     *  A is at A &lt;&lt; 1 + 1. Parent of node A is at A &gt;&gt; 1.
     */
    private PrioritizedSource<T>[]      heap;
    private final Comparator<T>     comparator;

    private final byte              direction; // 1 for ascending and -1 for descending

    public MSPriorityQueue(int capacity, boolean ascending, Comparator<T> c) {
        init (capacity);
        this.comparator = c;
        this.direction = (byte) (ascending ? 1 : -1);
    }
    
    public void         dump (PrintStream ps) {
        dump (ps, "", 1);
    }
    
    public void         dump (PrintStream ps, String indent, int pos) {
        PrioritizedSource<T>      obj = getInternal (pos);
        
        ps.println (indent + pos + ": " + obj);
        
        int             leftChildPos = pos << 1;
        int             rightChildPos = leftChildPos + 1;

        if (leftChildPos <= mSize) 
            dump (ps, indent + "  ", leftChildPos);

        if (rightChildPos <= mSize)
            dump (ps, indent + "  ", rightChildPos);
    }
    
//    public void         assertValid () {
//        for (int pos = 1; pos <= mSize; pos++) {
//            MessageSource<T>      obj = getInternal (pos);
//
//            int             leftChildPos = pos << 1;
//            int             rightChildPos = leftChildPos + 1;
//
//            if (leftChildPos <= mSize) {
//                MessageSource<T>  child = getInternal (leftChildPos);
//
//                assert (child.getMessage ().getNanoTime () >= obj.getMessage ().getNanoTime ()) :
//                 "(L) child at " + leftChildPos + ": " + child + " < parent: " + obj + "; parent pos: " + pos;
//            }
//
//            if (rightChildPos <= mSize) {
//                MessageSource<T>  child = getInternal (rightChildPos);
//
//                assert (child.getMessage ().getNanoTime () >= obj.getMessage ().getNanoTime ()) :
//                 "(R) child at " + rightChildPos + ": " + child + " < parent: " + obj + "; parent pos: " + 1;
//            }
//        }
//    }

    @SuppressWarnings("unchecked")
    public void         init (int numObjects) {
        heap = new PrioritizedSource [numObjects + 1];
        mSize = 0;
    }
    
    public void         clear () {
        mSize = 0;
    }
    
    public int          size () {
        return (mSize);
    }
        
    public boolean      isEmpty () {
        return (mSize == 0);
    }

    /**
     *  Spec: set heap at <code>pos</code> to <code>obj</code>, 
     *  and percolate the element up if necessary.
     */
    private int        percUp (int pos, PrioritizedSource<T> obj) {
        if (pos > 1) {
            pos = findPosForElementInPercUp(pos, obj);
        }
        heap [pos] = obj;
        return pos;
    }

    private int compareMessagesSources(PrioritizedSource<T> a, PrioritizedSource<T> b) {
        int val = comparator.compare(a.getMessage(), b.getMessage());
        if (val == 0) {
            val = Integer.compare(a.getPriority(), b.getPriority());
        }
        return val;
    }

    /**
     * This code is moved to a separate method from {@link #percUp} to allow JIT not inline this method.
     */
    private int findPosForElementInPercUp(int pos, PrioritizedSource<T> obj) {
        // Cache fields
        PrioritizedSource<T>[] heap = this.heap;
        byte direction = this.direction;

        while (pos > 1) {
            int parentPos = pos >> 1;
            PrioritizedSource<T> parentSource = heap[parentPos];

            if (compareMessagesSources(parentSource, obj) * direction <= 0)
                break;

            /**
             *  Percolate the parent down into the hole
             */
            heap [pos] = heap [parentPos];
            pos = parentPos;
        }
        return pos;
    }
    
    /**
     *  Spec: set heap at <code>pos</code> to <code>id</code>, 
     *  and percolate the element down if necessary.
     */
    private int     percDown (int pos, PrioritizedSource<T> obj) {
        if (pos * 2 <= mSize) {
            pos = findPosForElementInPercDown(pos, obj);
        }
        
        /**
         *  Plug this hole with the last element.
         */
        heap [pos] = obj;         
        
        return pos;
    }

    /**
     * This code is moved to a separate method from {@link #percDown} to allow JIT not inline this method.
     */
    private int findPosForElementInPercDown(int pos, PrioritizedSource<T> obj) {
        // Cache fields
        int mSize = this.mSize;
        PrioritizedSource<T>[] heap = this.heap;
        byte direction = this.direction;

        for (; ; ) {
            int leftChildPos = pos << 1;

            if (leftChildPos > mSize)
                break;
            
            /**
             *  Find smallest child of the parent at pos.
             */
            PrioritizedSource<T> leftSource = heap[leftChildPos];
            int rightChildPos = leftChildPos + 1;

            int smallestChildPos;
            PrioritizedSource<T> smallestSource;
            if (rightChildPos <= mSize) {
                PrioritizedSource<T> rightSource = heap[rightChildPos];

                if (compareMessagesSources(rightSource, leftSource) * direction < 0) {
                    smallestChildPos = rightChildPos;
                    smallestSource = rightSource;
                } else {
                    smallestChildPos = leftChildPos;
                    smallestSource = leftSource;
                }
            } else {
                smallestChildPos = leftChildPos;
                smallestSource = leftSource;
            }

            if (compareMessagesSources(obj, smallestSource) * direction <= 0)
                break;

            heap[pos] = heap[smallestChildPos];
            pos = smallestChildPos;
        }
        return pos;
    }

    public void         offer (PrioritizedSource<T> obj) {
        final int               currentCapacity = heap.length;
        
        mSize++;
            
        if (mSize + 1 >= currentCapacity) {
            extendCapacity(currentCapacity);
        }
        
        /**
         *  Put the element at new leaf location and adjust the heap.
         */
        percUp (mSize, obj);
    }

    @SuppressWarnings("unchecked")
    private void extendCapacity(int currentCapacity) {
        int newCapacity = currentCapacity * 2;
        PrioritizedSource[] newHeap = new PrioritizedSource[newCapacity];
        System.arraycopy(heap, 0, newHeap, 0, mSize);
        heap = newHeap;
    }

    private int          indexOfInternal (PrioritizedSource<T> obj) {
        for (int ii = 1; ii <= mSize; ii++)
            if (obj.equals (heap [ii]))
                return (ii);
        
        return (-1);
    }

    private PrioritizedSource <T> getInternal (int pos) {
        return heap [pos];
    }
    
    public PrioritizedSource<T>   get (int idx) {
        return getInternal (idx + 1);
    }
    
    private void         removeInternal (int pos) {
        /**
         *  We are removing element at pos, so we are going to have a hole there.
         *  Put the last element in the hole and adjust the heap.
         */

//        ByteArrayOutputStream buf = new ByteArrayOutputStream();
//        dump(new PrintStream(buf));

        PrioritizedSource<T>  last = getInternal (mSize);

        heap [mSize] = null;
        mSize--;

        if (percDown(pos, last) == pos)
            percUp(pos, last);
    }
    
    public boolean      remove (PrioritizedSource<T> obj) {
        int         pos = indexOfInternal (obj);
        
        if (pos > 0) {
            removeInternal (pos);
            return (true);
        }
        
        return (false);
    }
    
    public PrioritizedSource<T>       peek () {
        if (mSize == 0)
            return (null);
        
        return (getInternal (1));        
    }

    @Nullable
    public PrioritizedSource<T>       poll () {
        if (mSize == 0)
            return (null);
        
        PrioritizedSource<T>      min = getInternal (1);
        removeInternal (1);
        return (min);
    }
}