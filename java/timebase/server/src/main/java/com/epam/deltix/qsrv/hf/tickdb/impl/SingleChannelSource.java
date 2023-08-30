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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex;
import com.epam.deltix.qsrv.hf.blocks.InstrumentSet;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SymbolAndTypeSubscriptionControllerAdapter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TimeController;
import com.epam.deltix.util.collections.CharSequenceSet;
import com.epam.deltix.util.lang.Util;

public class SingleChannelSource implements
        InstrumentMessageSource,
        QuickMessageFilter,
        SubscriptionManager,
        TickStreamRelated,
        TimeController,
        SymbolAndTypeSubscriptionControllerAdapter {

    private final DXTickStream          stream;
    private TickStreamReader            reader;
    private volatile boolean            closed = false;

    private final InstrumentIndex       instrumentIndex = new InstrumentIndex ();

    private final CharSequenceSet       subscribedEntities = new CharSequenceSet();
    private boolean                     allEntitiesSubscribed = true;

    private final Set<String>           subscribedTypes = new HashSet<>();
    private boolean                     allTypesSubscribed = true;
    private Runnable                    changeListener;

    public <T extends SingleChannelStream & DXTickStream> SingleChannelSource(T stream, SelectionOptions options, long time, IdentityKey[] ids, String[] types) {
        this.stream = stream;

        synchronized (subscribedEntities) {
            allEntitiesSubscribed = ids == null;
            if (ids != null) {
                for (IdentityKey id : ids)
                    subscribedEntities.addCharSequence(id.getSymbol());
            }
        }

        allTypesSubscribed = types == null;
        if (types != null)
            Collections.addAll(subscribedTypes, types);

        this.reader = (TickStreamReader) stream.createSource(time, options, this);
    }

    @Override
    public boolean          acceptAllEntities() {
        synchronized (subscribedEntities) {
            return allEntitiesSubscribed;
        }
    }

    @Override
    public TickStream getStream() {
        return stream;
    }

    @Override
    public boolean          acceptEntity(CharSequence symbol) {
        synchronized (subscribedEntities) {
            return allEntitiesSubscribed || subscribedEntities.containsCharSequence(symbol);
        }
    }

    @Override
    public boolean isRestricted() {
        synchronized (subscribedEntities) {
            return !allEntitiesSubscribed && subscribedEntities.isEmpty();
        }
    }

    // Delegating MessageSource methods

    @Override
    public InstrumentMessage        getMessage() {
        return reader.getMessage();
    }

    @Override
    public boolean                  next() {
        return reader.next();
    }

    @Override
    public boolean                  isAtEnd() {
        return reader.isAtEnd();
    }

    @Override
    public void                     close() {
        Util.close(reader);
        closed = true;
    }

    @Override
    public boolean                  isClosed() {
        return closed;
    }

    @Override
    public void setAvailabilityListener(Runnable maybeAvailable) {
        reader.setAvailabilityListener(maybeAvailable);
    }

    @Override
    public int getCurrentStreamIndex() {
        return 0;
    }

    @Override
    public String getCurrentStreamKey() {
        return stream.getKey();
    }

    @Override
    public TickStream getCurrentStream() {
        return stream;
    }

    @Override
    public int                  getCurrentEntityIndex () {
        return (instrumentIndex.getOrAdd (reader.getMessage()));
    }

    @Override
    public boolean          isRealTime() {
        return reader.isRealTime();
    }

    @Override
    public boolean          realTimeAvailable() {
        return reader.realTimeAvailable();
    }

    @Override
    public void             add(IdentityKey[] entities, String[] types) {
        addEntities(entities, 0, entities.length);
    }

    @Override
    public void remove(IdentityKey[] entities, String[] types) {
        removeEntities(entities, 0, entities.length);
    }

    @Override
    public void         subscribeToAllEntities() {
        synchronized (subscribedEntities) {
            allEntitiesSubscribed = true;
            subscribedEntities.clear();
        }

        fireChanged();
    }

    @Override
    public void         clearAllEntities() {
        synchronized (subscribedEntities) {
            allEntitiesSubscribed = false;
            subscribedEntities.clear ();
        }

        fireChanged();
    }

    @Override
    public void         addEntity(IdentityKey id) {
        synchronized (subscribedEntities) {
            if (allEntitiesSubscribed)
                subscribedEntities.clear ();

            subscribedEntities.addCharSequence(id.getSymbol());

            allEntitiesSubscribed = false;
        }

        fireChanged();
    }

    @Override
    public void         addEntities(IdentityKey[] ids, int offset, int length) {
        synchronized (subscribedEntities) {
            if (allEntitiesSubscribed)
                subscribedEntities.clear ();

            for (int ii = 0; ii < length; ii++)
                subscribedEntities.addCharSequence(ids[offset + ii].getSymbol());

            allEntitiesSubscribed = false;
        }

        fireChanged();
    }

    @Override
    public void         removeEntities(IdentityKey[] ids, int offset, int length) {
        synchronized (subscribedEntities) {
            if (allEntitiesSubscribed) {
                subscribedEntities.clear ();
                allEntitiesSubscribed = false;
            } else {
                for (int ii = 0; ii < length; ii++)
                    subscribedEntities.removeCharSequence(ids[offset + ii].getSymbol());
            }
        }

        fireChanged();
    }

    @Override
    public void         removeEntity(IdentityKey id) {
        synchronized (subscribedEntities) {
            if (allEntitiesSubscribed) {
                subscribedEntities.clear ();
                allEntitiesSubscribed = false;
            } else {
                subscribedEntities.remove(id);
            }
        }

        fireChanged();
    }

    @Override
    public void addStream(TickStream... tickStreams) {

    }

    @Override
    public void removeAllStreams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeStream(TickStream... tickStreams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeForNewSubscriptions(long timestamp) {
        //reader.setTimeForNewSubscriptions(timestamp);
    }

    @Override
    public void             reset(long time) {
        reader.reset(time);
    }

    @Override
    public void subscribeToAllTypes() {
        allTypesSubscribed = true;
    }

    @Override
    public void setTypes(String... names) {
        allTypesSubscribed = false;
        subscribedTypes.clear();

        Collections.addAll(subscribedTypes, names);
    }

    @Override
    public void addTypes(String... names) {
        if (allTypesSubscribed) {
            subscribedTypes.clear();
            allTypesSubscribed = false;
        }

        Collections.addAll(subscribedTypes, names);
    }

    @Override
    public void removeTypes(String... names) {
        if (allTypesSubscribed) {
            subscribedTypes.clear();
            allTypesSubscribed = false;
        }

        for (String type : names)
            subscribedTypes.remove(type);
    }

    @Override
    public IdentityKey[] getSubscribedEntities() {
        synchronized (subscribedEntities) {
            return subscribedEntities.toArray(new IdentityKey[subscribedEntities.size()]);
        }
    }

    @Override
    public boolean isAllEntitiesSubscribed() {
        synchronized (subscribedEntities) {
            return allEntitiesSubscribed;
        }
    }

    @Override
    public String[] getSubscribedTypes() {
        return subscribedTypes.toArray(new String[subscribedTypes.size()]);
    }

    @Override
    public boolean isAllTypesSubscribed() {
        return allTypesSubscribed;
    }

    @Override
    public boolean hasSubscribedTypes() {
        return allTypesSubscribed || !subscribedTypes.isEmpty();
    }

    @Override
    public int                          getCurrentTypeIndex() {
        return reader.getCurrentTypeIndex();
    }

    @Override
    public RecordClassDescriptor        getCurrentType() {
        return reader.getCurrentType();
    }

    @Override
    public void     setChangeListener(Runnable r) {
        this.changeListener = r;
    }

    void            fireChanged() {
        if (changeListener != null)
            changeListener.run();
    }
}