package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.binaryheap;

import java.io.PrintStream;

/**
 *  Hand-optimized priority queue of MessageSource objects.
 *  Analogous to java.util.PriorityQueue &lt;MessageSource &lt;T&gt;&gt;
 */
public final class LongPriorityQueue {
    /**
     *  Number of elements in the heap.
     */
    private int                     mSize;

    /**
     *  The heap represented by array. Root has the index of 1.
     *  Left child of node A is at A &lt;&lt; 1. Right child of node
     *  A is at A &lt;&lt; 1 + 1. Parent of node A is at A &gt;&gt; 1.
     */
    private long[]      heap;

    private final byte              direction; // 1 for ascending and -1 for descending

    public LongPriorityQueue(int capacity, boolean ascending) {
        init (capacity);
        this.direction = (byte) (ascending ? 1 : -1);
    }
    
    public void         dump (PrintStream ps) {
        dump (ps, "", 1);
    }
    
    public void         dump (PrintStream ps, String indent, int pos) {
        long      obj = getInternal (pos);
        
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
        heap = new long[numObjects + 1];
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
    private int        percUp (int pos, long obj) {
        if (pos > 1) {
            pos = findPosForElementInPercUp(pos, obj);
        }
        heap [pos] = obj;
        return pos;
    }

    /**
     * This code is moved to a separate method from {@link #percUp} to allow JIT not inline this method.
     */
    private int findPosForElementInPercUp(int pos, long obj) {
        long message = obj;

        // Cache fields
        long[] heap = this.heap;
        byte direction = this.direction;

        while (pos > 1) {
            int parentPos = pos >> 1;
            long parent = heap[parentPos];

            if (Long.compare(parent, message) * direction <= 0)
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
    private int     percDown (int pos, long obj) {
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
    private int findPosForElementInPercDown(int pos, long obj) {
        long msg = obj;

        // Cache fields
        int mSize = this.mSize;
        long[] heap = this.heap;
        byte direction = this.direction;

        for (; ; ) {
            int leftChildPos = pos << 1;

            if (leftChildPos > mSize)
                break;
            
            /**
             *  Find smallest child of the parent at pos.
             */
            long left = heap[leftChildPos];
            int rightChildPos = leftChildPos + 1;

            int smallestChildPos;
            long smallest;
            if (rightChildPos <= mSize) {
                long right = heap[rightChildPos];

                if (Long.compare(right, left) * direction < 0) {
                    smallestChildPos = rightChildPos;
                    smallest = right;
                } else {
                    smallestChildPos = leftChildPos;
                    smallest = left;
                }
            } else {
                smallestChildPos = leftChildPos;
                smallest = left;
            }

            if (Long.compare(msg, smallest) * direction <= 0)
                break;

            heap[pos] = heap[smallestChildPos];
            pos = smallestChildPos;
        }
        return pos;
    }

    public void         offer (long obj) {
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
        long[] newHeap = new long[newCapacity];
        System.arraycopy(heap, 0, newHeap, 0, mSize);
        heap = newHeap;
    }

    private int          indexOfInternal (long obj) {
        for (int ii = 1; ii <= mSize; ii++)
            if (obj == heap [ii])
                return (ii);
        
        return (-1);
    }
    
    @SuppressWarnings ("unchecked")
    private long getInternal (int pos) {
        return heap [pos];
    }
    
    public long   get (int idx) {
        return  (getInternal (idx + 1));
    }
    
    private void         removeInternal (int pos) {
        /**
         *  We are removing element at pos, so we are going to have a hole there.
         *  Put the last element in the hole and adjust the heap.
         */

//        ByteArrayOutputStream buf = new ByteArrayOutputStream();
//        dump(new PrintStream(buf));

        long  last = getInternal (mSize);

        heap [mSize] = Long.MIN_VALUE; // TODO: Remove. This is not necessary.
        mSize--;

        if (percDown(pos, last) == pos)
            percUp(pos, last);
    }
    
    public boolean      remove (long obj) {
        int         pos = indexOfInternal (obj);
        
        if (pos > 0) {
            removeInternal (pos);
            return (true);
        }
        
        return (false);
    }
    
    public long       peek () {
        if (mSize == 0)
            return Long.MIN_VALUE;
        
        return (getInternal (1));        
    }
    
    public long       poll () {
        if (mSize == 0)
            return Long.MIN_VALUE;
        
        long      min = getInternal (1);
        removeInternal (1);
        return (min);
    }
}

