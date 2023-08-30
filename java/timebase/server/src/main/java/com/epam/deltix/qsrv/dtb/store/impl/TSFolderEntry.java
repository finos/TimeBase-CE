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

import com.epam.deltix.gflog.api.*;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.lang.MathUtil;

import java.io.IOException;

/**
 *
 */
abstract class TSFolderEntry
    implements Comparable <TSFolderEntry>, Loggable
{
    static final Log                LOGGER = LogFactory.getLog("deltix.dtb.filesystem");
//    static {
//        LOGGER.setLevel(LogLevel.DEBUG);
//    }
    protected static final boolean  TRACE = false;

    private static final int        DESTROYED = -199;
    
    protected final TSRootFolder    root;

    // Structure version on which this file were created, changes or deleted
    // also stored in index file to recover from any errors

    protected long                  version = 0;

    protected boolean               dropped = false;
    
    /**
     *  Guarded by this
     */
    protected volatile int          useCount = 0;
    
    /**
     *  Parent folder. Guarded by structure lock.
     */
    private TSFolder                parent;
    
    private int                     idxInParent;
    
    /**
     *  Unique entry id (within the parent). Guarded by structure lock.
     */
    private int                     id;
    
    private String                  tmpName;
    
    /**
     *  Start timestamp. Guarded by structure lock.
     */
    private long                    startTimestamp;        
        
    TSFolderEntry () {
        root = (TSRootFolder) this;
        this.parent = null;
        this.idxInParent = -1;
        this.id = -1;
        this.startTimestamp = Long.MIN_VALUE;
    }
    
    TSFolderEntry (        
        TSFolder            parent,
        int                 id, 
        long                startTimestamp
    )
    {
        this.root = parent.root;
        this.parent = parent;
        this.idxInParent = -1;
        this.id = id;
        this.startTimestamp = startTimestamp;
    }

    int                         getIdxInParent () {
        checkNotDestroyed ();
        
        assert root.currentThreadHoldsAnyLock ();
        
        return idxInParent;
    }

    void                        setIdxInParent (int idxInParent) {
        checkNotDestroyed ();
        
        assert root.currentThreadHoldsAnyLock ();
        
        this.idxInParent = idxInParent;
    }
    
    public long                  getVersion () {
        checkNotDestroyed ();
        
        assert root.currentThreadHoldsAnyLock ();
        
        return version;
    }

    public void                 setVersion(long version) {
        this.version = version;
    }

//    public int                  nextVersion () {
//        checkNotDestroyed ();
//
//        assert root.currentThreadHoldsAnyLock ();
//
//        return ++version;
//    }

//    public void                 setVersion (long version) {
//        checkNotDestroyed ();
//
//        assert root.currentThreadHoldsAnyLock ();
//
//        this.version = version;
//    }

    public synchronized boolean isDropped() {
        return dropped;
    }

    void                        drop() {
        getParent().dropChild(this);
    }

    boolean                     isFirst() {
        if (idxInParent == 0 && startTimestamp == Long.MIN_VALUE) {
            TSFolder parent = getParent();
            if (parent == root)
                return true;
            else if (parent != null)
                return parent.isFirst();

            return false;
        }

        return false;
    }
        
    synchronized boolean        incUseCount () {
        checkNotDestroyed ();
        
        boolean     justActivated = false;
        
        if (useCount == 0) {
            activate ();
            justActivated = true;
        }

        useCount++;

        //logTrace();

        if (TRACE) {
            PDSImpl.LOGGER.warn(this + ".incUseCount() = %d. trace: %s").with(useCount).with(new Exception());
        }
        
        return (justActivated);
    }
    
    synchronized boolean        decUseCount () {
        checkNotDestroyed ();
        
        boolean     justDeactivated = false;
        
        assert useCount > 0;
        
        useCount--;
        
        if (useCount == 0) {
            deactivate ();
            justDeactivated = true;
        }

        if (TRACE) {
            PDSImpl.LOGGER.warn(this + ".decUseCount() = %d. trace: %s").with(useCount).with(new Exception());
        }

        return (justDeactivated);
    }
    
    protected void              useLess () {
        assert Thread.holdsLock (this);
        
        checkNotDestroyed ();
        
        if (useCount <= 1)
            throw new IllegalStateException ("useCount=" + useCount);
        
        useCount--;

        //logTrace();

        if (TRACE) {
            PDSImpl.LOGGER.warn(this + ".useLess() = %d. trace: %s").with(useCount).with(new Exception());
        }
    }
    
    protected void              useMore () {
        assert Thread.holdsLock (this);

        checkNotDestroyed ();
        
        if (useCount == 0)
            throw new IllegalStateException ("useCount=" + useCount);
        
        useCount++;

        //logTrace();

        if (TRACE) {
            PDSImpl.LOGGER.warn(this + ".useMore() = %d. trace: %s").with(useCount).with(new Exception());
        }
    }

    private void                logTrace() {
        System.out.print(this + ".useCount = " + useCount);
        //usages.add(useCount + " - " + Arrays.toString(new Exception().getStackTrace()));
    }
    
    void                        activate () {
        if (LOGGER.isEnabled (LogLevel.DEBUG))
            LOGGER.debug().append(this).append(" is activated").commit();
    }
    
    void                        deactivate () {
        if (LOGGER.isEnabled (LogLevel.DEBUG))
            LOGGER.debug().append(this).append(" is de-activated").commit();
    }
    
    final void                  assertNotLockedByCurrentThread () {
        assert !Thread.holdsLock (this) : 
            this + " is already locked by " + Thread.currentThread () +
            ".\nUpdating parent would cause mutual locking by tree nodes.";
    }
    
    @Override
    public final int        compareTo (TSFolderEntry that) {
        return MathUtil.compare (this.startTimestamp, that.startTimestamp);
    }

    public final AbstractPath      getPath () {
        return (root.getFS ().createPath (getPathString ()));
    }
    
    final TSFolder          getParent () {
        checkNotDestroyed ();
        
        assert root.currentThreadHoldsAnyLock ();
        
        return parent;
    }
    
    abstract String         getNormalName ();
    
    final void              setTmpName (String tmpName) {
        assert root.currentThreadHoldsWriteLock ();
        
        this.tmpName = tmpName;
    }
    
    final String            getName () {
        assert root.currentThreadHoldsAnyLock ();
        
        return (tmpName == null ? getNormalName () : tmpName);
    }

    final int               getId () {
        checkNotDestroyed ();
        
        assert root.currentThreadHoldsAnyLock ();
        
        return id;
    }

    final void              renameToNormal () throws IOException {
        assert root.currentThreadHoldsWriteLock ();
        
        String      normalName = getNormalName ();
        
        getPath ().renameTo (normalName);
        
        this.tmpName = null;
    }
    
    /**
     * Must be public because the TimeSlice interface implemented by TSFile 
     * wants it public.
     */
    public final long           getStartTimestamp () {
        checkNotDestroyed ();
        
        return startTimestamp;
    }

    final boolean              setStartTimestamp (long ts) {
        assert ts != Long.MAX_VALUE;

        if (!isFirst()) {
            //assert ts != Long.MIN_VALUE;

            if (startTimestamp != ts) {
                startTimestamp = ts;
                return ts != Long.MIN_VALUE;
            }
        }

        return false;
    }

    TSFolderEntry           getNextSibling () {
        return (getParent ().getChildOrNull (idxInParent + 1));
    }
    
    TSFolderEntry           getPreviousSibling () {
        return (getParent ().getChildOrNull (idxInParent - 1));
    }

    String                  getPathString () {
        return (getParent ().getPathString () + root.getFS().getSeparator() + getName ());
    }
    
    @Override
    public String           toString () {
        if (useCount == DESTROYED)
            return (getClass ().getSimpleName () + " (destroyed, no longer identifiable)");
        else if (root.isWriteLockedByCurrentThread()) {
            return (getPathString () + " @" + startTimestamp);
        } else if (root.tryAcquireSharedLock ()) {
            try {
                return (getPathString () + " @" + startTimestamp);
            } finally {
                root.releaseSharedLock ();
            }
        }
        else {
            return (getClass ().getSimpleName () + "#" + id + " @" + startTimestamp);
        }
    }

    public String           toShortString() {
        return (getClass ().getSimpleName () + "#" + id + " @" + startTimestamp);
    }

    @Override
    public void             appendTo(AppendableEntry entry) {
        if (useCount == DESTROYED) {
            entry.append(getClass().getSimpleName()).append(" (destroyed, no longer identifiable)");
        } else if (root.isWriteLockedByCurrentThread()) {
            String path = getPathString();
            entry.append(path).append(" @").append(startTimestamp); // Do not obtain lock to avoid nasty logger recursion
        } else if (root.tryAcquireSharedLock ()) {
            try {
                String path = getPathString();
                entry.append(path).append(" @").append(startTimestamp);
            } finally {
                root.releaseSharedLock ();
            }
        } else {
            entry.append(getClass().getSimpleName ()).append("#").append(id).append(" @").append(startTimestamp);
        }
    }

    final synchronized boolean  isActive () {
        return (useCount > 0);
    }        

    abstract boolean            exists ();

    final boolean               setParent (TSFolder newParent) {
        parent = newParent;

        return (useCount > 0);
    }

    final synchronized void         destroy () {
        destroy(false);
    }

    synchronized void               destroy (boolean force) {
        if (!force && useCount != 0)
            throw new IllegalStateException (
                this + " is being destroyed while used; useCount = " + useCount
            );
        
        if (LOGGER.isEnabled (LogLevel.DEBUG))
            LOGGER.debug().append(this).append(" is destroyed").commit();

        id = -1;
        useCount = DESTROYED;
        parent = null;
        tmpName = null;
        startTimestamp = Long.MAX_VALUE;
    }
    
    protected final void              checkNotDestroyed () {
        if (useCount == DESTROYED) 
            throw new IllegalStateException (this + " must not be accessed.");
    }

    @Override
    protected void                  finalize () throws Throwable {
        if (useCount > 0)
            LOGGER.warn().append(this).append(" is being finalized while used").commit();

        super.finalize ();
    }
}