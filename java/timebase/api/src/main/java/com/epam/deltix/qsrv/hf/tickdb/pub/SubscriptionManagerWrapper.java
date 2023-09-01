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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Provides SubscriptionManager capabilities to InstrumentMessageSource
 */
public class SubscriptionManagerWrapper extends InstrumentMessageSourceAdapter implements SubscriptionManager {
    private static final Logger LOG = Logger.getLogger(SubscriptionManagerWrapper.class.getName());
    private static Byte                         VALUE = 0;

    private boolean                             allEntitiesSubscribed = false;
    private final InstrumentToObjectMap<Byte> subscribedEntities =
        new InstrumentToObjectMap<Byte>();

    private boolean                             allTypesSubscribed = false;
    private final Set<String> subscribedTypes = new HashSet<String>();

    private SubscriptionManagerWrapper(InstrumentMessageSource delegate) {
        super(delegate);

        if (delegate instanceof SubscriptionManager)
            LOG.warning("Nested SubscriptionManager: " + delegate.getClass().getName());
    }

    public static InstrumentMessageSource   wrap(InstrumentMessageSource source) {
        if (source instanceof SubscriptionManager)
            return source;

        return new SubscriptionManagerWrapper(source);
    }

    @Override
    public IdentityKey[] getSubscribedEntities() {
        return (IdentityKey[]) subscribedEntities.keySet().toArray(new IdentityKey[subscribedEntities.size()]);
    }

    @Override
    public boolean isAllEntitiesSubscribed() {
        return allEntitiesSubscribed;
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
        return !subscribedTypes.isEmpty();
    }

    @Override
    public void                     subscribeToAllEntities() {
        allEntitiesSubscribed = true;
        subscribedEntities.clear();

        getDelegate().subscribeToAllEntities();
    }

    @Override
    public void                     clearAllEntities() {
        allEntitiesSubscribed = false;
        subscribedEntities.clear();

        getDelegate().clearAllEntities();
    }

    @Override
    public void                     add(IdentityKey[] ids, String[] types) {

        // entities
        if (allEntitiesSubscribed)
            subscribedEntities.clear();

        allEntitiesSubscribed = false;
        for (int i = 0; i < ids.length; i++)
            subscribedEntities.put(ids[i], VALUE);

        // types
        if (allTypesSubscribed)
            subscribedTypes.clear();

        allTypesSubscribed = false;
        subscribedTypes.addAll(Arrays.asList(types));

        getDelegate().add(ids, types);
    }

    @Override
    public void                     remove(IdentityKey[] ids, String[] types) {

        // entities
        if (allEntitiesSubscribed)
            subscribedEntities.clear();
        else
            for (int i = 0; i < ids.length; i++)
                subscribedEntities.remove(ids[i]);
        allEntitiesSubscribed = false;

        // types
        if (allTypesSubscribed)
            subscribedTypes.clear();
        else
            subscribedTypes.removeAll(Arrays.asList(types));
        allTypesSubscribed = false;

        getDelegate().remove(ids, types);
    }

    @Override
    public void                     addEntity(IdentityKey id) {
        if (allEntitiesSubscribed)
            subscribedEntities.clear();

        allEntitiesSubscribed = false;
        subscribedEntities.put(id, VALUE);

        getDelegate().addEntity(id);
    }

    @Override
    public void                     addEntities(IdentityKey[] ids, int offset, int length) {
        if (allEntitiesSubscribed)
            subscribedEntities.clear();

        allEntitiesSubscribed = false;

        for (int i = offset; i < length; i++)
            subscribedEntities.put(ids[i], VALUE);

        getDelegate().addEntities(ids, offset, length);
    }

    @Override
    public void                     removeEntity(IdentityKey id) {
        if (allEntitiesSubscribed)
            subscribedEntities.clear();
        else
            subscribedEntities.remove(id);

        allEntitiesSubscribed = false;
        getDelegate().removeEntity(id);
    }

    @Override
    public void                     removeEntities(IdentityKey[] ids, int offset, int length) {
        if (allEntitiesSubscribed)
            subscribedEntities.clear();
        else
            for (int i = offset; i < length; i++)
                subscribedEntities.remove(ids[i]);

        allEntitiesSubscribed = false;

        getDelegate().removeEntities(ids, offset, length);
    }

    @Override
    public void addStream(TickStream... tickStreams) {
        getDelegate().addStream(tickStreams);
    }

    @Override
    public void removeAllStreams() {
        getDelegate().removeAllStreams();
    }

    @Override
    public void removeStream(TickStream... tickStreams) {
        getDelegate().removeStream(tickStreams);
    }

    @Override
    public void setTimeForNewSubscriptions(long time) {
        getDelegate().setTimeForNewSubscriptions(time);
    }

    @Override
    public void reset(long time) {
        getDelegate().reset(time);
    }

    @Override
    public void                             subscribeToAllTypes() {
        allTypesSubscribed = true;
        subscribedTypes.clear();

        getDelegate().subscribeToAllTypes();
    }

    @Override
    public void                             setTypes(String... names) {
        allTypesSubscribed = false;

        subscribedTypes.clear();
        subscribedTypes.addAll(Arrays.asList(names));

        getDelegate().setTypes(names);
    }

    @Override
    public void                             addTypes(String... names) {
        allTypesSubscribed = false;

        subscribedTypes.addAll(Arrays.asList(names));
        getDelegate().addTypes(names);
    }

    @Override
    public void                             removeTypes(String... names) {
        if (allEntitiesSubscribed)
            subscribedTypes.clear();
        else
            subscribedTypes.removeAll(Arrays.asList(names));

        allTypesSubscribed = false;

        subscribedTypes.removeAll(Arrays.asList(names));
        getDelegate().removeTypes(names);
    }

}