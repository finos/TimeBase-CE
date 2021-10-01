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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockEventListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.util.lang.Util;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ServerStreamImpl implements DXTickStream, LockHandler {

    public static long sequence = 0;

    private final ArrayList<LockEventListener>      lockEventListeners =
        new ArrayList <LockEventListener> ();

    private final ArrayList<StreamStateListener>      propertyChangeListeners =
            new ArrayList <StreamStateListener> ();

    private volatile StreamStateListener[]  snChangeListeners = { };
    private final Set<SubscriptionChangeListener>   subscriptionListeners =
        new HashSet<SubscriptionChangeListener>();

    // internal index to increase indexing performance for tick cursors
    private final long           index;

    ServerStreamImpl() {
        synchronized (ServerStreamImpl.class) {
            index = sequence++;
        }
    }

    long                  getIndex() {
        return index;
    }

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private InstrumentsSubscription      entitiesSubscription = new InstrumentsSubscription() {

        @Override
        void            fireChanges(Collection<IdentityKey> added,
                                    Collection<IdentityKey> removed,
                                    SubscriptionChangeListener[] listeners) {
            
            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];

                if (removed != null && removed.size() > 0)
                    listener.entitiesRemoved(removed);
                if (added != null && added.size() > 0)
                    listener.entitiesAdded(added);
            }
        }

        @Override
        void            fireAllAdded(SubscriptionChangeListener[] listeners) {
            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];
                listener.allEntitiesAdded();
            }
        }

        @Override
        void            fireAllRemoved(SubscriptionChangeListener[] listeners) {
            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];
                listener.allEntitiesRemoved();
            }
        }

        @Override
        SubscriptionChangeListener[]        getListeners() {
            return getSubscriptionListeners();
        }
    };

    private SubscriptionAggregator<String>                  typesSubscription =
            new SubscriptionAggregator<String>()
    {
        @Override
        void fireChanges(Collection<String> added, Collection<String> removed,
                         SubscriptionChangeListener[] listeners) {

            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];

                if (removed != null && removed.size() > 0)
                    listener.typesRemoved(removed);

                if (added != null && added.size() > 0)
                    listener.typesAdded(added);
            }
        }

        @Override
        void fireAllAdded(SubscriptionChangeListener[] listeners) {

            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];
                listener.allTypesAdded();
            }
        }

        @Override
        void fireAllRemoved(SubscriptionChangeListener[] listeners) {

            for (int i = 0; i < listeners.length; i++) {
                SubscriptionChangeListener listener = listeners[i];
                listener.allTypesRemoved();
            }
        }

        @Override
        SubscriptionChangeListener[] getListeners() {
            return getSubscriptionListeners();
        }
    };

    void                        warmUp () {
    }

    void                        coolDown () {
    }

    void                        trimToSize () throws IOException {        
    }

    abstract void               cursorCreated (TickCursor cur);
    
    abstract void               cursorClosed (TickCursor cursor);

    void                        onDBOpen () {
    }

    public void                 abortBackgroundProcess () {
        throw new UnsupportedOperationException ();
    }

    public void                 execute (TransformationTask task) {
        throw new UnsupportedOperationException ();
    }

    public BackgroundProcessInfo getBackgroundProcess () {
        return (null);
    }

    public abstract long                             getSizeOnDisk ();

    public void                 purge (long time) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void purge(long time, String space) {
        throw new UnsupportedOperationException();
    }

    public void                 setTargetNumFiles (int value) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void                 addEventListener(LockEventListener listener) {
        synchronized (lockEventListeners) {
            lockEventListeners.add(listener);
        }
    }

    @Override
    public void                 removeEventListener(LockEventListener listener) {
        synchronized (lockEventListeners) {
            lockEventListeners.remove(listener);
        }
    }

    LockEventListener[]         getEventListeners() {
        LockEventListener[] list;

        synchronized (lockEventListeners) {
            list = lockEventListeners.toArray(new LockEventListener[lockEventListeners.size()]);
        }

        return list;
    }

    boolean             hasLock(StreamLockImpl lock) {
        throw new UnsupportedOperationException ();
    }

    void                removeLock(StreamLockImpl lock) {
        throw new UnsupportedOperationException ();
    }

    /// Subscription implementation

//    private void                    updateSubscriptionSnapshot () {
//        int size = subscriptionListeners.size ();
//        snListenersSnapshot = subscriptionListeners.toArray (new SubscriptionChangeListener[size]);
//    }
    
    private SubscriptionChangeListener[] getSubscriptionListeners() {

        SubscriptionChangeListener[] listeners;
        synchronized (subscriptionListeners) {
            listeners = subscriptionListeners.toArray (new SubscriptionChangeListener[subscriptionListeners.size ()]);
        }

        return listeners;
    }

    public final void           addSubscriptionListener (SubscriptionChangeListener lnr) {
        if (lnr == null)
            return;
        
        synchronized (subscriptionListeners) {
            subscriptionListeners.add (lnr);
            //updateSubscriptionSnapshot();
        }

        entitiesSubscription.fireCurrent(lnr);
        typesSubscription.fireCurrent(lnr);
    }

    public final void           removeSubscriptionListener (SubscriptionChangeListener lnr) {
        synchronized (subscriptionListeners) {
            subscriptionListeners.remove (lnr);
            //updateSubscriptionSnapshot();
        }
    }

    public final void           addStateListener(StreamStateListener lnr) {
        if (lnr == null)
            return;

        synchronized (propertyChangeListeners) {
            propertyChangeListeners.add (lnr);
            snChangeListeners = propertyChangeListeners.toArray(snChangeListeners);
        }
    }

    public final void           removeStateListener (StreamStateListener lnr) {
        synchronized (propertyChangeListeners) {
            propertyChangeListeners.remove (lnr);
            snChangeListeners = propertyChangeListeners.toArray(snChangeListeners);
        }
    }

    void         firePropertyChanged(int property) {
        StreamStateListener[] changeListeners = snChangeListeners;

        for (int i = 0, len = changeListeners.length; i < len; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.changed(this, property);
        }
    }

    void         fireWriterCreated(IdentityKey[] ids) {
        StreamStateListener[] changeListeners = snChangeListeners;

        for (int i = 0, len = changeListeners.length; i < len; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.writerCreated(this, ids);
        }
    }

    void         fireWriterClosed(IdentityKey[] ids) {
        StreamStateListener[] changeListeners = snChangeListeners;

        for (int i = 0, len = changeListeners.length; i < len; i++) {
            StreamStateListener listener = changeListeners[i];
            if (listener != null)
                listener.writerClosed(this, ids);
        }
    }

    @Override
    public String                         describe() {
        try {
            StringWriter writer = new StringWriter();
            GrammarUtil.describe("", this, writer);
            return writer.getBuffer().toString();
        } catch (IOException e) {
            throw Util.asRuntimeException(e);
        }
    }

    void                allEntitiesAdded(TickCursor cursor) {
        entitiesSubscription.addAll(cursor);
    }

    void                allEntitiesRemoved(TickCursor cursor) {
        entitiesSubscription.removeAll(cursor);
    }

    void                entitiesChanged(TickCursor cursor,
                                                Collection<IdentityKey> added,
                                                Collection<IdentityKey> removed) {
        
        entitiesSubscription.change(cursor, added, removed);
    }

    void                allTypesAdded(TickCursor cursor) {
        typesSubscription.addAll(cursor);
    }

    void                allTypesRemoved(TickCursor cursor) {
        typesSubscription.removeAll(cursor);
    }

    void                typesChanged(TickCursor cursor,
                                             Collection<String> added,
                                             Collection<String> removed) {

        typesSubscription.change(cursor, added, removed);
    }

    /**
     * Lock for stream metadata access. <p>
     * This lock is designed to prevent access to partially constructed streams.
     */
    @CheckReturnValue
    ReadWriteLock getLock() {
        return readWriteLock;
    }

    /**
     * Ensures that stream is properly constructed.
     * May block.
     * Do not call inside of synchronization block on "TickDBImpl.streams".
     */
    void blockTillReadable() {
        Lock lock = readWriteLock.readLock();
        lock.lock();
        lock.unlock();
    }

    //    void                fireTimeRangeChanged() {
//        timeRangeChangeNotifier.submit();
//    }
//
//    private final QuickExecutor.QuickTask timeRangeChangeNotifier = new QuickExecutor.QuickTask(getDBImpl().getQuickExecutor()) {
//        @Override
//        public void run() throws InterruptedException {
//            firePropertyChanged(TickStreamProperties.TIME_RANGE);
//        }
//    };
}