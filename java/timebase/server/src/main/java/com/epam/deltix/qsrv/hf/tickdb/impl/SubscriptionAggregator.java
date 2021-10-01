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

import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import net.jcip.annotations.GuardedBy;

import java.util.*;

public abstract class SubscriptionAggregator<T> {

    private final ArrayList<T>              added = new ArrayList<T>();
    private final ArrayList<T>              removed = new ArrayList<T>();
    private final Reference                 all = new Reference();

    @GuardedBy("this")
    private final HashMap<T, Reference>     subscribed = new HashMap<T, Reference>();

    static class Reference {
        final HashSet<TickCursor> owners = new HashSet<TickCursor>();
        boolean notified = false;
        
        boolean add(TickCursor cursor) {
            return owners.add(cursor) && owners.size() == 1;
        }

        boolean remove(TickCursor cursor) {
            if (owners.remove(cursor))
                return owners.isEmpty();
            
            return owners.isEmpty();
        }
    }

    public synchronized void    change(TickCursor cursor,
                                    Collection<T> cAdded,
                                    Collection<T> cRemoved) {
        added.clear();
        removed.clear();

        boolean notify = all.owners.isEmpty();

        if (cAdded != null && !cAdded.isEmpty()) {
            for (T id : cAdded) {
                Reference ref = subscribed.get(id);
                if (ref == null) {
                    ref = new Reference();
                    subscribed.put(id, ref);
                }

                if (ref.add(cursor) || !ref.notified)
                    if (notify) {
                        added.add(id);
                        ref.notified = true;
                    }
            }
        }

        if (cRemoved != null && !cRemoved.isEmpty()) {
            for (T id : cRemoved) {
                Reference ref = subscribed.get(id);

                if (ref == null) continue;

                if (ref.remove(cursor) || !ref.notified)
                    if (notify) {
                        removed.add(id);
                        ref.notified = true;
                    }
            }
        }

        if (notify)
            fireChanges(added, removed, getListeners());
    }

    public synchronized void         addAll(TickCursor cursor) {
        if (all.add(cursor))
            fireAllAdded(getListeners());

        // clear individual subscription owned by this cursor
        Iterator<Map.Entry<T, Reference>> iterator = subscribed.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<T, Reference> entry = iterator.next();

            Reference ref = entry.getValue();
            if (ref.remove(cursor))
                iterator.remove();
        }
    }

    public synchronized void         removeAll(TickCursor cursor) {
        added.clear();
        removed.clear();

        boolean notify = all.remove(cursor);

        Iterator<Map.Entry<T, Reference>> iterator = subscribed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<T, Reference> entry = iterator.next();

            Reference ref = entry.getValue();
            boolean used = !ref.remove(cursor);

            if (notify) {
                if (!used && ref.notified) {
                    removed.add(entry.getKey());
                    ref.notified = false;
                } else if (used && !ref.notified) {
                    added.add(entry.getKey());
                    ref.notified = true;
                }
            }

            if (!used)
                iterator.remove();
        }

        if (notify) {
            if (subscribed.isEmpty())
                fireAllRemoved(getListeners());
            else
                fireChanges(added, removed, getListeners());
        }
    }

    void   fireCurrent(SubscriptionChangeListener ... listeners) {
        if (all.owners.size() > 0) {
            fireAllAdded(listeners);
        } else if (!subscribed.isEmpty()) {
            ArrayList<T> copy = new ArrayList<T>();

            synchronized (this) {
                copy.addAll(subscribed.keySet());
            }

            fireChanges(copy, null, listeners);
        }
    }

    abstract void   fireChanges(Collection<T> added,
                                Collection<T> removed,
                                SubscriptionChangeListener[] listeners);

    abstract void   fireAllAdded(SubscriptionChangeListener[] listeners);

    abstract void   fireAllRemoved(SubscriptionChangeListener[] listeners);

    abstract SubscriptionChangeListener[]   getListeners();
}