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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.TSFFormat;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.pub.AbstractSingleEntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.util.collections.generated.*;

import java.io.*;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;


/**
 *  Implementation of a time slice folder. The content of the activated folder 
 *  is guarded by the root structure lock. The activation process itself is
 *  guarded by "this".
 */
abstract class TSFolder extends TSFolderEntry {

    //public static final int         FORMAT_VERSION = 2;

    private static class EntityIndexEntry {
        final int                   entity;
        TSFolderEntry               first;
        TSFolderEntry               last;

        EntityIndexEntry (int entity, TSFolderEntry first, TSFolderEntry last) {
            this.entity = entity;
            this.first = first;
            this.last = last;
        }
    }
    
    /**
     *  Whether this folder has unsaved data. Guarded by the structure lock.
     */
    private volatile boolean                    isDirty;
    
    /**
     *  Whether data in folder changed between store temp index and finalize it. Guarded by the structure lock.
     */
    private volatile boolean                     isDataChanged; // TODO: simplify

    /**
     *  Next file id. Guarded by structure lock.
     */
    private int                                 nextChildId;
    
    /**
     *  Child files or folders. Guarded by structure lock
     */
    private ObjectArrayList <TSFolderEntry>     children = null;

    /**
     *  First and last children with data for each entity. Guarded by this.
     *
     *  Basically for each entity (instrument) this list stores the first file and the last data file
     */
    private ObjectArrayList <EntityIndexEntry>  entityIndex = null;

    // run-time cache of entities for each file - only for leaf folders
    private HashMap<TSFolderEntry, IntegerArrayList> cache;

    TSFolder () {
    }
    
    TSFolder (
        TSFolder                        parent, 
        int                             id, 
        long                            timestamp
    )
    {
        super (parent, id, timestamp);
    }

    protected void                  findActiveChildren (Collection <TSFolderEntry> out) {

        if (!isActive () || children == null)
            return;

        out.add (this);

        ObjectArrayList<TSFolderEntry> list = children;

        for (int i = 0, childrenSize = list.size(); i < childrenSize; i++) {
            TSFolderEntry e = list.get(i);
            if (e instanceof TSFile)
                out.add(e);
            else
                ((TSFolder) e).findActiveChildren(out);
        }
    }

    void                     buildCache() {

        synchronized (this) {
            if (cache == null)
                cache = new HashMap<>();
            else
                return;
        }

        for (TSFolderEntry child : children) {
            if (child instanceof TSFile) {
                TreeOps.use(child);
                try {
                    IntegerArrayList value = ((TSFile) child).getEntities();
                    synchronized (this) {
                        cache.put(child, value);
                    }
                } catch (IOException e) {
                    LOGGER.error().append(e).commit();
                } finally {
                    TreeOps.unuse(child);
                }
            }
        }

//        for (int i = 0; i < entityIndex.size(); i++) {
//            EntityIndexEntry entry = entityIndex.get(i);
//
//            IntegerArrayList list = cache.get(entry.first);
//            assert list.contains(entry.entity);
//
//            list = cache.get(entry.last);
//            assert list.contains(entry.entity);
//        }
    }

    TSFolderEntry                           getNext(TSFolderEntry entry, int entity) {
        TSFolderEntry next = entry.getNextSibling();
        while (next != null && cache != null) {
            IntegerArrayList entries = cache.get(next);
            if (entries != null && entries.contains(entity))
                return next;
            else
                next = next.getNextSibling();
        }

        return next;
    }

    TSFolderEntry                           getPrevious(TSFolderEntry entry, int entity) {
        TSFolderEntry previous = entry.getPreviousSibling();
        while (previous != null && cache != null) {
            IntegerArrayList entries = cache.get(previous);
            if (entries != null && entries.contains(entity))
                return previous;
            else
                previous = previous.getPreviousSibling();
        }

        return previous;
    }

    void        dropChild (TSFolderEntry child) {
        // guarded by this

        boolean isEmpty;
        IntegerHashSet removed = new IntegerHashSet();

        synchronized (this) {
            int index = child.getIdxInParent();
            assert index < children.size();

            TSFolderEntry next = child.getNextSibling();

            removeFromIndex(child, removed); // replace references

            children.remove(index);
            for (int i = index > 0 ? index - 1 : index, size = children.size(); i < size; i++)
                children.get(i).setIdxInParent(i);

            child.setIdxInParent(index - 1); // for references

            if (index == 0) { // first child dropped
                if (next != null)
                    TreeOps.setStartTimestamp(this, next.getStartTimestamp());
                else
                    setStartTimestamp(Long.MIN_VALUE); /// ???
            }

            setDirty();
            isEmpty = children.isEmpty();
        }

        final IntegerEnumeration e = removed.keys();
        while (e.hasMoreElements ())
            TreeOps.propagateDataDeletion(this, e.nextIntElement());

        if (isEmpty && getParent() != null)
            getParent().dropChild(this);
    }

    void                            format() {
        assert root.currentThreadHoldsWriteLock ();

        isDirty = false;

        for (int i = 0; children != null && i < children.size(); i++) {
            TSFolderEntry e = children.get(i);

            if (e instanceof TSFolder) {
                ((TSFolder) e).format();
            } else if (e instanceof TSFile) {
                TSFile file = (TSFile) e;
                root.getCache().removeFromWriteQueue(file);
                file.deactivate();
            }
            e.destroy(true);
        }

        children = null;

        // deactivate
        while (isActive())
            decUseCount();
    }
    
    @Override
    boolean                         exists () {
        return (true);
    }
    
    /**
     *  Return the first entry in this folder.
     * 
     *  @return     The entry. The returned entry may be inactive.
     * 
     *  @throws IOException 
     */
    TSFolderEntry                   getFirstEntry () throws IOException {
        ensureLoaded ();
        return (children.isEmpty () ? null : children.get (0));      
    }           
            
    /**
     *  Return the first entry in this folder.
     * 
     *  @return     The entry. The returned entry may be inactive.
     * 
     *  @throws IOException 
     */
    TSFolderEntry                   getLastEntry () throws IOException {        
        ensureLoaded ();
                
        int     sz = children.size ();
        
        return (sz == 0 ? null : children.get (sz - 1));      
    }           
            
    /**
     *  Find an entry by timestamp.
     * 
     *  @param timestamp
     *  @param create
     * 
     *  @return     The entry, if found. The returned entry may be inactive.
     * 
     *  @throws IOException 
     */
    TSFolderEntry                   findEntry (
        long                            timestamp, 
        boolean                         create
    ) 
        throws IOException 
    {        
        ensureLoaded ();
                
        if (children.isEmpty ()) {
            if (this != root)
                throw new IllegalStateException ("Empty intermediate folder");
            
            if (!create)
                return (null);            
            //
            //  Create the first TSF ever
            //
            TSFile      tsf = new TSFile (this, root.getSequence(), nextId (), Long.MIN_VALUE, true);

            tsf.setIdxInParent (0);
            children.add (tsf);

            return (tsf);
        }
        
        int         pos = findChild (timestamp);

        if (pos < 0) {
            pos = -pos - 2;     // snap to the left
            
            if (pos < 0) 
                throw new IllegalArgumentException (
                    "Looking for out-of-range timestamp " + timestamp + 
                    " in " + this
                );
        } 

        return (getChild (pos));           
    }

    /**
     *  Find an entry by timestamp.
     *
     *  @return     The entry, if found. The returned entry may be inactive.
     * 
     *  @throws IOException 
     */
    TSFolderEntry                   findEntryById (int id) throws IOException {
        ensureLoaded ();
                
        int                 numChildren = children.size ();

        for (int ii = 0; ii < numChildren; ii++) {
            TSFolderEntry   child = children.getObjectNoRangeCheck (ii);
            
            if (child.getId () == id)
                return (child);
        }
        
        return (null);           
    }           

    TSFolderEntry               getChildOrNull (int idx) {
        if (idx < 0 || idx >= children.size ())
            return (null);
        
        return (getChild (idx));
    }
    
    TSFolderEntry               getChild (int idx) {
        TSFolderEntry   result = children.get (idx);
        
        if (result.getIdxInParent () != idx)
            throw new IllegalStateException (
                result + " at idx " + idx + " in " + this + " claims idx = " +
                    result.getIdxInParent ()
            );
        
        return result;
    }
    
    @Override
    synchronized void                        deactivate () {
        if (isDirty)
            throw new IllegalStateException ("Deactivating dirty folder " + this);
        
        if (children != null) {
            if (DTSDebug.DESTROY_CHECK)
                destroyChildren ();

            children.clear();
            children = null;
        }

        if (cache != null)
            cache.clear();
        cache = null;
        
        entityIndex = null;
        
        super.deactivate ();
    }
        
    TSFolderEntry                   getFirstChildWithDataFor (int entity) 
        throws IOException 
    {        
        EntityIndexEntry    ee = findEntityIndexEntry (entity);
        
        return (ee == null ? null : ee.first);
    }

    TSFolderEntry                   getLastChildWithDataFor (int entity) 
        throws IOException 
    {        
        EntityIndexEntry    ee = findEntityIndexEntry (entity);
        
        return (ee == null ? null : ee.last);
    }
        
    TSFolderEntry                   getFirstChildWithDataFor (EntityFilter filter) 
        throws IOException 
    {
        if (filter == null)
            return (getFirstEntry ());
        
        if (filter instanceof AbstractSingleEntityFilter)
            return (getFirstChildWithDataFor (((AbstractSingleEntityFilter) filter).getSingleEntity ()));
        
        ensureLoaded ();

        TSFolderEntry ret = null;

        synchronized (this) {
            int numEntries = entityIndex.size();

            for (int ii = 0; ii < numEntries; ii++) {
                EntityIndexEntry ee = entityIndex.getObjectNoRangeCheck(ii);

                if (!filter.accept(ee.entity))
                    continue;

                if (ret == null || earlier(ee.first, ret))
                    ret = ee.first;
            }
        }
        
        return (ret);
    }

    boolean                   isAccepted (TSFolderEntry entry, EntityFilter filter)
            throws IOException
    {
        if (filter == null)
            return true;

        ensureLoaded ();

        synchronized (this) {

            if (filter instanceof AbstractSingleEntityFilter) {
                int entity = ((AbstractSingleEntityFilter) filter).getSingleEntity();
                EntityIndexEntry ee = entityIndex.getObjectNoRangeCheck(entity);

                return earlier(ee.first, entry);
            }

            int numEntries = entityIndex.size();

            for (int ii = 0; ii < numEntries; ii++) {
                EntityIndexEntry ee = entityIndex.getObjectNoRangeCheck(ii);

                if (!filter.accept(ee.entity))
                    continue;

                if (ee.first.getIdxInParent() <= entry.getIdxInParent() && ee.last.getIdxInParent() >= entry.getIdxInParent())
                    return true;
            }
        }

        return false;
    }

    TSFolderEntry                   getLastChildWithDataFor (EntityFilter filter) 
        throws IOException 
    {    
        if (filter == null)
            return (getLastEntry ());
        
        if (filter instanceof AbstractSingleEntityFilter)
            return (getLastChildWithDataFor (((AbstractSingleEntityFilter) filter).getSingleEntity ()));
        
        ensureLoaded ();
        
        TSFolderEntry           ret = null;

        synchronized (this) {
            int numEntries = entityIndex.size();

            for (int ii = 0; ii < numEntries; ii++) {
                EntityIndexEntry ee = entityIndex.getObjectNoRangeCheck(ii);

                if (!filter.accept(ee.entity))
                    continue;

                if (ret == null || later(ee.last, ret))
                    ret = ee.last;
            }
        }
        
        return (ret);
    }
        
    int                             getNumChildren () {
        assert root.currentThreadHoldsAnyLock ();
        
        return (children.size ());
    }
    
    boolean                         hasRoomFor (int addlEntries) {
        assert root.currentThreadHoldsWriteLock ();
        
        return (children.size () + addlEntries <= root.getMaxFolderSize ());               
    }
    
    final void                      setChildren (TSFolderEntry ... newChildren) {
        assert root.currentThreadHoldsWriteLock ();
        assert children.isEmpty ();
        
        int                 length = newChildren.length;
        
        for (int ii = 0; ii < length; ii++) {
            TSFolderEntry   child = newChildren [ii];
            
            child.setIdxInParent (ii);
            children.add (child);    
        }
    }
    
    final TSFile                    createFileAfter (TSFile after, long ts) {
        TSFile      newFile = new TSFile (this, root.getSequence(), nextId (), ts, true);
        
        insertNewChildAfter (after, newFile);
        
        return (newFile);
    }
    
    final TSIntermediateFolder      createChildFolder (long ts) {
        TSIntermediateFolder    f = new TSIntermediateFolder (this, root.getSequence(), nextId (), ts);
        TreeOps.use (f);
        f.initNew ();
        
        return (f);
    }
    
    final TSIntermediateFolder      createFolderAfter (TSFolder after, long ts) {
        TSIntermediateFolder      newFolder = createChildFolder (ts);
        
        insertNewChildAfter (after, newFolder);
        
        return (newFolder);
    }
    
    final void                      insertNewChildAfter (
        TSFolderEntry                   after, 
        TSFolderEntry                   newEntry
    )
    {
        assert root.currentThreadHoldsWriteLock ();
        
        if (after.getParent () != this)
            throw new IllegalArgumentException (
                after + " does not have " + this + " as its parent"
            );
        
        if (newEntry.getParent () != this)
            throw new IllegalArgumentException (
                newEntry + " does not have " + this + " as its parent"
            );
        
        insertAndLinkEntry (after.getIdxInParent () + 1, newEntry);                
    }  

    @Override
    String                      getNormalName () {
        return (TSNames.buildFolderName (getId ()));
    }    
    
    private static boolean      earlier (TSFolderEntry a, TSFolderEntry b) {
        return (a.getIdxInParent () < b.getIdxInParent ());
    }
    
    private static boolean      later (TSFolderEntry a, TSFolderEntry b) {
        return (a.getIdxInParent () > b.getIdxInParent ());
    }

    synchronized boolean                     dataAdded (TSFolderEntry child, int entity)
        throws IOException 
    {
        ensureLoaded ();
        
        assert children.contains (child) : child.getPathString();
        
        int             pos = findEntity (entity);
        boolean         wasNewEntity = pos < 0;
        
        if (wasNewEntity) {
            entityIndex.add (-pos - 1, new EntityIndexEntry (entity, child, child));
            setDirty();
        }
        else {
            EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (pos);
            
            if (earlier (child, ee.first)) {
                ee.first = child;
                setDirty();
            }
            else if (later (child, ee.last)) {
                ee.last = child;
                setDirty();
            }
        }

        if (cache != null) {
            IntegerArrayList list = cache.computeIfAbsent(child, k -> new IntegerArrayList());
            list.add(entity);
        }
        
        return (wasNewEntity);
    }

    /*
        Return true, if entity completely removed from index.
     */
    boolean                     dataRemoved (TSFolderEntry child, int entity) {

        boolean checkFirst = false;
        boolean checkLast = false;

        EntityIndexEntry ee;
        int pos;

        synchronized (this) {
            pos = findEntity(entity);

            assert pos >= 0;

            if (cache != null) {
                IntegerArrayList list = cache.get(child);
                if (list != null) {
                    int index = list.indexOf(entity);
                    if (index != -1)
                        list.remove(index);
                }
            }

            ee = entityIndex.getObjectNoRangeCheck(pos);

            if (child.equals(ee.first) && child.equals(ee.last)) {
                ee.first = ee.last = null;
            } else if (child.equals(ee.first)) {
                checkFirst = true;
            } else if (child.equals(ee.last)) {
                checkLast = true;
            }

            if (ee.first == null && ee.last == null) {
                entityIndex.remove(pos);
                setDirty();
                return true;
            } 
        }

        if (checkFirst) {
            TSFolderEntry first = TreeOps.getNextSibling(child, entity);

            if (first == null) {
                synchronized (this) {
                    // check position again, collection may change out-of lock
                    pos = findEntity(entity);
                    entityIndex.remove(pos);
                    setDirty();
                    return true;
                }
            }
            else {
                synchronized (this) {
                    ee.first = first;
                    setDirty();
                }
            }
        }

        if (checkLast) {
            TSFolderEntry last = TreeOps.getPreviousSibling(child, entity); // assuming that we have this data in cache

            if (last == null) {
                synchronized (this) {
                    // check position again, collection may change out-of lock
                    pos = findEntity(entity);
                    entityIndex.remove(pos);
                    setDirty();
                    return true;
                }
            } else {
                synchronized (this) {
                    ee.last = last;
                    setDirty();
                }
            }
        }

        return false;
    }

    void                removeFromIndex(TSFolderEntry child, IntegerHashSet removed) {
        assert Thread.holdsLock(this);

        // guarded by this

        for (int ii = 0, numEntries = entityIndex.size(); ii < numEntries; ii++) {
            EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (ii);

            if (child.equals(ee.first) && child.equals(ee.last))
                ee.first = ee.last = null;
            else  if (child.equals(ee.first))
                ee.first = getNext(child, ee.entity);
            else if (child.equals(ee.last))
                ee.last = getPrevious(child, ee.entity);

            // check for possible errors in index
            if (ee.first == null || ee.last == null)
                ee.first = ee.last = null;
        }

        // clean-up empty index
        for (int ii = entityIndex.size() - 1; ii >= 0; ii--) {
            EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (ii);
            if (ee.first == null && ee.last == null) {
                entityIndex.remove(ii);
                removed.add(ee.entity);
            }
        }

        if (cache != null)
            cache.remove(child);

        setDirty();
    }
            
    void                        dataMovedLeftToRight (
        TSFolderEntry               fromLeft, 
        TSFolderEntry               toRight, 
        int                         entity
    ) 
        throws IOException 
    {   
        ensureLoaded ();
        
        assert children.contains (fromLeft);
        assert children.contains (toRight);
        assert earlier (fromLeft, toRight);
        
        int                 pos = findEntity (entity);
        
        if (pos < 0) 
            throw new IllegalArgumentException (entity + " not found in " + this);
        
        EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (pos);

        if (ee.first == fromLeft) {
            ee.first = toRight;
            setDirty();
        }
        
        if (ee.last == fromLeft) {
            ee.last = toRight;
            setDirty();
        }
    }

//    void                            moveTo(TSFolder newParent) throws IOException {
//
//        assert root.isWriteLockedByCurrentThread();
//
//        assert newParent.children == null || newParent.children.isEmpty();
//
//        newParent.children = children;
//        newParent.entityIndex = entityIndex;
//
//        AbstractPath                        toPath = newParent.getPath();
//
//        for (TSFolderEntry child : children) {
//            child.setParent(this);
//
//            if (child.exists ())
//                child.getPath().moveTo(toPath.append(child.getName()));
//        }
//
//        if (children.size() > 0)
//            setStartTimestamp(children.get(0).getStartTimestamp());
//
//        isDataChanged = isDirty = true;
//    }

    void                            moveChildrenHere (
        TSFolder                        oldParent, 
        int                             pivotIdx,
        boolean                         fromLeftOfPivot
    ) 
        throws IOException
    {
        assert root.currentThreadHoldsWriteLock ();
        
        ObjectArrayList <TSFolderEntry>     oldChildList = oldParent.children;
        AbstractPath                        oldParentPath = oldParent.getPath ();
        AbstractPath                        thisPath = getPath ();
        
        TSFolderEntry       firstChild;
        TSFolderEntry       lastChild;
        int                 firstIdx;
        int                 limitIdx;
        
        if (fromLeftOfPivot) {
            firstIdx = 0;
            firstChild = oldChildList.get (0);
            limitIdx = pivotIdx;
            lastChild = oldChildList.get (pivotIdx - 1);
        }
        else {  
            firstIdx = pivotIdx;
            firstChild = oldChildList.get (pivotIdx);
            limitIdx = oldChildList.size ();
            lastChild = oldChildList.get (limitIdx - 1);            
        }

        this.nextChildId = lastChild.getId() + 1;
              
        assert getStartTimestamp () == firstChild.getStartTimestamp ();                
        assert children.isEmpty ();
        
        //
        //  Deal with entity indexes
        //
        ObjectArrayList <EntityIndexEntry>  oldEntIdx = oldParent.entityIndex;
        TSFolder                            myParent = getParent ();
        boolean                             shiftDown = myParent == oldParent;
            
        for (int ii = 0; ii < oldEntIdx.size (); ) {
            EntityIndexEntry    oldEntry = oldEntIdx.getObjectNoRangeCheck (ii);
            //
            //  If no intersection with this folder, skip.
            //
            if (fromLeftOfPivot ? 
                    later (oldEntry.first, lastChild) :
                    earlier (oldEntry.last, firstChild))
            {
                ii++;
                continue;
            }
            //
            //  Intersection is found
            //
            int                 entity = oldEntry.entity;
            
            boolean             entrySpansFolders = 
                fromLeftOfPivot ? 
                    later (oldEntry.last, lastChild) :
                    earlier (oldEntry.first, firstChild);
                  
            if (!shiftDown) {
                if (entrySpansFolders) {
                    boolean     check = myParent.dataAdded (this, entity);
                                                            
                    if (check)
                        throw new IllegalStateException (
                            myParent + " thinks entity " + entity +
                            " is new, but we just split it from existing child " + 
                            oldParent
                        );
                }
                else
                    myParent.dataMovedLeftToRight (oldParent, this, entity);
            }
            
            TSFolderEntry       efirst = oldEntry.first;
            TSFolderEntry       elast = oldEntry.last;
            
            if (entrySpansFolders) {
                if (fromLeftOfPivot) {
                    oldEntry.first = 
                        shiftDown ?
                            this :
                            oldChildList.get (pivotIdx);
                    
                    elast = lastChild;
                }
                else {
                    efirst = firstChild;
                    
                    oldEntry.last = 
                        shiftDown ?
                            this :
                            oldChildList.get (pivotIdx - 1);                                        
                }
            }
            else if (shiftDown) {
                oldEntry.first = oldEntry.last = this;
            } 
            else {
                oldEntIdx.remove (ii);      // Move it over
                entityIndex.add (oldEntry);
                continue;
            }       
            
            entityIndex.add (new EntityIndexEntry (entity, efirst, elast));
            ii++;
        }
        //
        //  Now safe to move children
        //
        children.ensureCapacity (limitIdx - firstIdx);
        
        for (int ii = firstIdx; ii < limitIdx; ii++) {
            TSFolderEntry       child = oldChildList.get (ii);
            
            if (child.setParent (this)) {
                //
                //  If we are here, the child was active. Adjust use counts.
                //
                if (oldParent.decUseCount ())
                    throw new IllegalStateException (
                        oldParent + " got deactivated in the middle of moving children"
                    );
                
                incUseCount ();
            }
            
            if (child.exists ()) {
                String          name = child.getName ();
                
                oldParentPath.append (name).moveTo (thisPath.append (name));
            }
            
            child.setIdxInParent (children.size ());
            children.add (child);
        }
                
        oldChildList.removeRange (firstIdx, limitIdx);
        
        if (fromLeftOfPivot) {
            int         newSize = oldChildList.size ();
            
            for (int ii = 0; ii < newSize; ii++)
                oldChildList.getObjectNoRangeCheck (ii).setIdxInParent (ii);
        }

        setDirty();
        oldParent.setDirty();
    }

    //
    //  INTERNALS
    //
    private int                     nextId () {
        assert root.currentThreadHoldsWriteLock ();
        isDirty = true;
        isDataChanged = true;

        int         numChildren = children.size ();
        
        nextid: for (;;) {
            int     id = nextChildId++;

            if (nextChildId >= Short.MAX_VALUE)
                nextChildId = 0;
            
            for (int ii = 0; ii < numChildren; ii++)
                if (children.getObjectNoRangeCheck (ii).getId () == id)
                    continue nextid;
            
            return (id);
        }
    }
    
    private void                    destroyChildren () {
        if (children == null)
            return;
        
        int         numChildren = children.size ();
        
        for (int ii = 0; ii < numChildren; ii++)
            children.getObjectNoRangeCheck(ii).destroy();
    }
    
    private EntityIndexEntry        findEntityIndexEntry (int entity)
        throws IOException 
    {
        ensureLoaded();

        synchronized (this) {
            int pos = findEntity(entity);

            if (pos < 0)
                return (null);

            return (entityIndex.getObjectNoRangeCheck(pos));
        }
    }
    
    private int                     findEntity (int entity) {
        int         low = 0;
        int         high = entityIndex.size () - 1;

        while (low <= high) {
            int     mid = (low + high) >>> 1;
            int     midVal = entityIndex.getObjectNoRangeCheck (mid).entity;
            
            if (midVal < entity)
                low = mid + 1;
            else if (midVal > entity)
                high = mid - 1;
            else
                return mid;
        }

        return -(low + 1);
    }
    
    private int                     findChild (long ts) {
        int         low = 0;
        int         high = children.size () - 1;

        while (low <= high) {
            int     mid = (low + high) >>> 1;
            long    midVal = children.getObjectNoRangeCheck (mid).getStartTimestamp ();
            
            if (midVal < ts)
                low = mid + 1;
            else if (midVal > ts)
                high = mid - 1;
            else
                return mid;
        }

        return -(low + 1);
    }   

    private void                    insertAndLinkEntry (int idx, TSFolderEntry e) {
        e.setIdxInParent (idx);
        children.add (idx, e);
           
        int     n = children.size ();
        
        for (int ii = idx + 1; ii < n; ii++) 
            children.getObjectNoRangeCheck (ii).setIdxInParent (ii);                
    }      

    synchronized void               initNew () {
        if (!isActive ())
            throw new AssertionError (this + " is not active");
        
        if (children != null)
            throw new AssertionError (this + " is not new, usages: " + useCount);
        
        children = new ObjectArrayList <> ();
        entityIndex = new ObjectArrayList<> ();

        isDirty = true;
        isDataChanged = true;
    }

//    synchronized void              moveTo(TSFolder folder) {
//        children = folder.children;
//        entityIndex = folder.entityIndex;
//        isDirty = true;
//        isDataChanged = true;
//
//        getPath().
//    }

    private synchronized void       ensureLoaded () throws IOException {
        if (!isActive ())
            throw new AssertionError (this + " is not active");
        
        if (children != null)
            return;

        //new Exception(this + ".ensureLoaded() :").printStackTrace(System.out);

        AbstractPath        path = getPath ();
        
        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append("LOADING FOLDER: ").append(getPathString()).commit();
        
        isDirty = false;
        
        AbstractPath        fp = path.append (TSNames.INDEX_NAME);
        
        int                 numChildren;
        
        try (DataInputStream dis = 
                new DataInputStream (BufferedStreamUtil.wrapWithBuffered(fp.openInput (0)))
            )
        {   
            int             formatVersion = dis.readUnsignedShort ();
            
            if (formatVersion > TSFFormat.INDEX_FORMAT_VERSION)
                throw new IOException (
                    fp + ": Unrecognized format version " + formatVersion
                );
            
            setVersion(formatVersion >= 2 ? dis.readLong () : dis.readInt());
            nextChildId = dis.readUnsignedShort ();
            
            numChildren = dis.readUnsignedShort ();
                        
            children = new ObjectArrayList <> (numChildren);
            
            IntegerToObjectHashMap <TSFolderEntry>     idToChild = 
                new IntegerToObjectHashMap <> (numChildren);
            
            long            prevTS = Long.MIN_VALUE;
            
            int index = 0;
            
            for (int ii = 0; ii < numChildren; ii++) {
                boolean         isFile = dis.readBoolean ();
                int             eid = dis.readUnsignedShort ();
                long            ts = dis.readLong ();
                long            v = dis.readLong ();
                
                if (index == 0) {
                    if (ts != getStartTimestamp ())
                        throw new IOException (
                            "Bad first child (id=" + eid + ") ts in " + path + ": " +
                            ts + " (expected " + getStartTimestamp ()
                        );
                } else if (isFile && ts == Long.MIN_VALUE) {
                    // just skip empty files
                    LOGGER.warn().append("Skip file: ").append(TSNames.buildFileName(eid)).append(" in path: ").append(path).commit();
                    continue;
                }
                else if (ts < prevTS)
                        throw new IOException (
                            "Bad child ts order in " + path + ": " +
                            prevTS + " --> " + ts
                        );
                
                prevTS = ts;
                
                TSFolderEntry   e;
                
                if (isFile)
                    e = new TSFile (this, root.getSequence(), eid, ts, false);
                else
                    e = new TSIntermediateFolder (this, root.getSequence(), eid, ts);
                
                children.add (e);
                e.setIdxInParent (index);
                idToChild.put (eid, e);

                // set limit time
                if (isFile && index > 0)
                    ((TSFile)children.get(index - 1)).limitTimestamp = ts;

                index++;
            }
                        
            int             numEntities = dis.readInt ();
            
            entityIndex = new ObjectArrayList <> (numEntities);

            int             prevEntity = -1;
            
            for (int ii = 0; ii < numEntities; ii++) {
                int                 entity = dis.readInt ();
                
                if (entity <= prevEntity)
                    throw new IOException (
                        "Bad entity order in " + path + ": " +
                        prevEntity + " --> " + entity
                    );
                
                prevEntity = entity;
                
                int                 firstId = dis.readUnsignedShort ();
                TSFolderEntry       first = idToChild.get (firstId, null);
                
                if (first == null)
                    throw new IOException (
                        "Bad entity index in " + fp + ": child is " +
                        firstId + " was not found"
                    );
                
                int                 lastId = dis.readUnsignedShort ();                
                TSFolderEntry       last = idToChild.get (lastId, null);
                
                if (last == null)
                    throw new IOException (
                        "Bad entity index in " + fp + ": child is " +
                        lastId + " was not found"
                    );
                
                EntityIndexEntry    ee = 
                    new EntityIndexEntry (entity, first, last);
                
                entityIndex.add (ee);
            }
        }         
        
        assert checkConsistent ();

        //checkTailExist ();
    }

    synchronized boolean           storeDirtyData () throws IOException {
        if (storeIndexFile()) {
            finalizeIndexFile();
            return true;
        }

        return false;
    }

    synchronized final void        setDirty() {
        isDirty = isDataChanged = true;
    }

    synchronized boolean           storeIndexFile() throws IOException {
        if (!isActive ())
            throw new AssertionError (this + " is not active");
        
        if (!isDirty)
            return false;
        
        assert checkConsistent ();
        
        AbstractPath    tmp = TreeOps.makeTempPath (getPath (), TSNames.INDEX_NAME);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug().append("Storing ").append(tmp.getPathString()).commit();
        int             numChildren = children.size ();
        int             numEntities = entityIndex.size ();
        final int       fileSize = 0;
        
        try (DataOutputStream dos =
                new DataOutputStream (
                    new BufferedOutputStream (tmp.openOutput (fileSize))
                )
             )
        {
            dos.writeShort (TSFFormat.INDEX_FORMAT_VERSION);
            dos.writeLong (getVersion());
            dos.writeShort (nextChildId);
            dos.writeShort (numChildren);
            
            for (int ii = 0; ii < numChildren; ii++) {
                TSFolderEntry   e = children.getObjectNoRangeCheck (ii);
                
                dos.writeBoolean (e instanceof TSFile);
                dos.writeShort (e.getId ());
                dos.writeLong (e.getStartTimestamp ());
                dos.writeLong (e.getVersion ());
            }
            
            dos.writeInt (numEntities);
            
            for (int ii = 0; ii < numEntities; ii++) {
                EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (ii);
                
                dos.writeInt (ee.entity);
                dos.writeShort (ee.first.getId ());
                dos.writeShort (ee.last.getId ());                
            }            
        }

        isDataChanged = false;

        return true;
    }   
    
    synchronized void            finalizeIndexFile() throws IOException {
        AbstractPath path = TreeOps.makeTempPath(getPath(), TSNames.INDEX_NAME);

        if (path.exists()) {
            if (!isDataChanged)
                isDirty = false;

            TreeOps.finalize(path);
        }
    }

    synchronized boolean             checkConsistent () {
        if (!isActive ())
            throw new AssertionError (this + " is not active");
        
        if (children == null)
            throw new AssertionError (this + " is not loaded");
        
        int         numChildren = children.size ();
        int         numEntities = entityIndex.size ();
        
        if (numChildren == 0) {
            if (numEntities != 0)
                throw new AssertionError (
                    this + " has no children, but non-empty entity index"
                );
            
//            if (this != root)
//                throw new AssertionError ("Empty intermediate folder");
            
            return (true);
        }
        
        BitSet          idsSeen = new BitSet ();

        for (int ii = 0; ii < numChildren; ii++) {
            TSFolderEntry child = children.getObjectNoRangeCheck (ii);
            
            if (child.getIdxInParent () != ii)
                throw new AssertionError (
                    "child " + child + " #" + ii + 
                    " is mistaken about its position: claims " + 
                    child.getIdxInParent ()
                );
            
            int             cid = child.getId ();
            
            if (idsSeen.get (cid))
                throw new AssertionError (
                    "child " + child + " #" + ii + 
                    " has duplicate id=" + cid
                );
            
            idsSeen.set (cid);
            
            long            cts = child.getStartTimestamp ();
            
            if (ii == 0) {
                if (cts != getStartTimestamp ())
                    throw new AssertionError (
                        "First child " + child + " of " + this + 
                        " has ts=" + cts + ", different from this ts=" + 
                        getStartTimestamp ()
                    );                                
            } else {
                TSFolderEntry   prev = children.getObjectNoRangeCheck (ii - 1);
                long            pts = prev.getStartTimestamp ();
                
                // after truncation files can be empty, so startTimestamp = Long.MIN_VALUE

                if (cts != Long.MIN_VALUE && cts < pts)
                    throw new AssertionError (
                        "child " + child + " #" + ii + 
                        " has ts=" + cts + " out of order with previous ts=" + pts
                    );
            }
        }
        
        idsSeen = null;
        
        int         prevEntity = -1;
        
        for (int ii = 0; ii < numEntities; ii++) {
            EntityIndexEntry    ee = entityIndex.getObjectNoRangeCheck (ii);
            int                 entity = ee.entity;
            
            if (entity <= prevEntity)
                throw new AssertionError (
                    "Entity " + entity + " #" + ii + " was expected to be > " +
                    prevEntity
                );
            
            prevEntity = entity;
            
            if (ee.first == null || ee.last == null)
                throw new AssertionError (
                    "Null child pointers in index entry for entity " + 
                    entity + " #" + ii 
                );
            
            int                 x1 = children.indexOf (ee.first);
            int                 x2 = children.indexOf (ee.last);
            
            if (x1 < 0)
                throw new AssertionError (
                    "In index entry for entity " + 
                    entity + " #" + ii + " left child " + ee.first + 
                    " was not found in this folder."
                );
            
            if (x2 < 0)
                throw new AssertionError (
                    "In index entry for entity " + 
                    entity + " #" + ii + " right child " + ee.first + 
                    " was not found in this folder."
                );
            
            if (x1 > x2)
                throw new AssertionError (
                    "Index entry for entity " + 
                    entity + " #" + ii + " lists children in wrong order: " +
                    ee.first + " .. " + ee.last + " (their indexes are " + 
                    x1 + " .. " + x2 + ")"
                );
        }

        return (true);
    }


//    private void checkTailExist () {
//        if ( ! children.isEmpty()) {
//            TSFolderEntry missing = children.get(children.size() - 1);
//            if ( ! missing.getPath().exists()) {
//                LOGGER.level(Level.WARN).append("The last data file in the folder is missing: ").append(missing.getPathString()).commit();
//                children.remove(children.size() - 1);
//                isDirty = true;
//                isDataChanged = true;
//                TSFolderEntry prev = missing.getPreviousSibling();
//                if (prev != null || prev.getParent() != missing.getParent()) {
//                    int         numEntities = entityIndex.size ();
//                    for (int ii = 0; ii < numEntities; ii++) {
//                        EntityIndexEntry ee = entityIndex.getObjectNoRangeCheck(ii);
//                        if (ee.first == missing)
//                            ee.first = prev;
//                        if (ee.last == missing)
//                            ee.last = prev;
//
//                    }
//
//                } else {
//                    throw new AssertionError("Missing data file " + missing + " (Run Repair Shop)");
//                }
//            }
//        }
//    }
}