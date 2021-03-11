package com.epam.deltix.qsrv.dtb.store.dataacc;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.util.lang.Util;

import java.io.PrintStream;

/**
 *  Hand-optimized priority queue of AccessorBlockLink objects.
 *  Analogous to java.util.PriorityQueue &lt;AccessorBlockLink&gt;
 */
final class ABLPQ {
    /**
     *  Number of elements in the heap.
     */
    private int                     size;
    
    /**
     *  The heap represented by array. Root has the index of 1.
     *  Left child of node A is at A &lt;&lt; 1. Right child of node
     *  A is at A &lt;&lt; 1 + 1. Parent of node A is at A &gt;&gt; 1.
     */
    private AccessorBlockLink []    heap;
    private final byte              direction; // 1 for ascending and -1 for descending
        
    public ABLPQ (int capacity, boolean ascending) {
        init (capacity);
        this.direction = ascending ? (byte) 1 : -1;
    }

    public ABLPQ (boolean ascending) {
        this (10, ascending);
    }
    
    public void         dump (PrintStream ps) {
        dump (ps, "", 1);
    }
    
    public void         dump (PrintStream ps, String indent, int pos) {
        AccessorBlockLink      obj = getInternal (pos);
        
        ps.println (indent + pos + ": " + obj);
        
        int             leftChildPos = pos << 1;
        int             rightChildPos = leftChildPos + 1;

        if (leftChildPos <= size) 
            dump (ps, indent + "  ", leftChildPos);

        if (rightChildPos <= size)
            dump (ps, indent + "  ", rightChildPos);
    }
    
    public void         assertValid () {
        for (int pos = 1; pos <= size; pos++) {           
            AccessorBlockLink      obj = getInternal (pos);
            int             leftChildPos = pos << 1;
            int             rightChildPos = leftChildPos + 1;
            
            if (leftChildPos <= size) {
                AccessorBlockLink  child = getInternal (leftChildPos);
            
                if (child.getNextTimestamp () < obj.getNextTimestamp ())
                    throw new RuntimeException (
                        "(L) child at " + leftChildPos + ": " +
                        child + 
                        " < parent: " + obj + "; parent pos: " + pos
                    );
            }            
            
            if (rightChildPos <= size) {
                AccessorBlockLink  child = getInternal (rightChildPos);
                
                if (child.getNextTimestamp () < obj.getNextTimestamp ())
                    throw new RuntimeException (
                        "(R) child at " + rightChildPos + ": " +
                        child + 
                        " < parent: " + obj + "; parent pos: " + pos
                    );
            }                        
        }
    }
    
    public void         init (int numObjects) {
        heap = new AccessorBlockLink [numObjects + 1];
        size = 0;
    }
    
    public void         clear () {
        size = 0;
    }
    
    public int          size () {
        return (size);
    }
        
    public boolean      isEmpty () {
        return (size == 0);
    }
    
    private int         compare (AccessorBlockLink x1, AccessorBlockLink x2) {
        long                t1 = x1.getNextTimestamp ();
        long                t2 = x2.getNextTimestamp ();

        if (t1 > t2)
            return direction;

        if (t2 > t1)
            return -direction;

        // reverse ordering should affect and entities order
        return Long.compare(x1.getEntity (), x2.getEntity ()) * direction;
    }
    
    /**
     *  Spec: set heap at <code>pos</code> to <code>obj</code>, 
     *  and percolate the element up if necessary.
     */
    private void        percUp (int pos, AccessorBlockLink obj) {
        while (pos > 1) {
            int                 parentPos = pos >> 1;
            
            if (compare (heap [parentPos], obj) <= 0)
                break;

            /**
             *  Percolate the parent down into the hole
             */
            heap [pos] = heap [parentPos];
            
            pos = parentPos;
        }

        heap [pos] = obj;
    }
    
    /**
     *  Spec: set heap at <code>pos</code> to <code>id</code>, 
     *  and percolate the element down if necessary.
     */
    private boolean     percDown (int pos, AccessorBlockLink obj) {
        boolean     percolated = false;
        
        for (;;) {
            int         leftChildPos = pos << 1;
            
            if (leftChildPos > size)
                break;
            
            /**
             *  Find smallest child of the parent at pos.
             */
            int                         rightChildPos = leftChildPos + 1;
            int                         smallestChildPos;
            
            if (rightChildPos <= size) {
                if (compare (heap [rightChildPos], heap [leftChildPos]) < 0) 
                    smallestChildPos = rightChildPos;                
                else 
                    smallestChildPos = leftChildPos;                
            }
            else 
                smallestChildPos = leftChildPos;            
            
            if (compare (obj, heap [smallestChildPos]) <= 0)
                break;            
            
            heap [pos] = heap [smallestChildPos];
            pos = smallestChildPos;
            percolated = true;
        }
        
        /**
         *  Plug this hole with the last element.
         */
        heap [pos] = obj; 
        
        return (percolated);
    }

    public void         offer (AccessorBlockLink obj) {

        obj.queued = true;
        final int               currentCapacity = heap.length;
        
        size++;        
            
        if (size + 1 >= currentCapacity) {
            int                 newCapacity = currentCapacity * 2;
            AccessorBlockLink [] newHeap = new AccessorBlockLink [newCapacity];                        
            
            System.arraycopy (heap, 0, newHeap, 0, size);
            
            heap = newHeap;
        }
        
        /**
         *  Put the element at new leaf location and adjust the heap.
         */
        percUp (size, obj);
    }
    
    private int          indexOfInternal (AccessorBlockLink obj) {
        for (int ii = 1; ii <= size; ii++)
            if (obj.equals (heap [ii]))
                return (ii);
        
        return (-1);
    }

    public boolean contains(AccessorBlockLink link) {
        return indexOfInternal (link) != -1;
    }
    
    @SuppressWarnings ("unchecked")
    private AccessorBlockLink getInternal (int pos) {
        return ((AccessorBlockLink) heap [pos]);
    }
    
    public AccessorBlockLink get (int idx) {
        return (getInternal (idx + 1));
    }
    
    private void         removeInternal (int pos) {
        /**
         *  We are removing element at pos, so we are going to have a hole there.
         *  Put the last element in the hole and adjust the heap.
         */
        AccessorBlockLink  last = getInternal (size);
        
        heap [size] = null;
        size--;
        
        percDown (pos, last);
    }
    
    public boolean      remove (AccessorBlockLink obj) {
        int         pos = indexOfInternal (obj);
        
        if (pos <= 0)
            return (false);
        
        removeInternal (pos);
        return (true);
    }
    
    public AccessorBlockLink       peek () {
        if (size == 0)
            return (null);
        
        return (getInternal (1));        
    }
    
    public AccessorBlockLink       poll () {
        if (size == 0)
            return (null);
        
        AccessorBlockLink      min = getInternal (1);        
        removeInternal (1);
        min.queued = false;
        return (min);
    }
}

