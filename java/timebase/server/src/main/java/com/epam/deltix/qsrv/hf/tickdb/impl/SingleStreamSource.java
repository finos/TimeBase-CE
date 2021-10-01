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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.util.collections.CharSequenceSet;

import java.util.Collection;

class SingleStreamSource extends AbstractStreamSource implements QuickMessageFilter {

    protected final DXTickStream                    stream;

    private final CharSequenceSet                   subscribedEntities = new CharSequenceSet();
    private boolean                                 isSubscribedToAllEntities = false;
    protected long                                  time;

    protected MessageSource <InstrumentMessage>     source;

    SingleStreamSource(PrioritizedMessageSourceMultiplexer<InstrumentMessage> mx, DXTickStream s, SelectionOptions options) {
        super(mx, options);

        this.stream = s;
    }

    protected boolean     checkSource() {

        if (source != null)
            return false;

        if (stream instanceof TransientStreamImpl) {
            if (isSubscribedToAllEntities || !subscribedEntities.isEmpty()) {
                SingleChannelStream fsi = (SingleChannelStream) stream;
                source = fsi.createSource(time, options, this);

                mx.add(source, time);
            }
        }
//        else if (stream instanceof ExternalStreamImpl) {
//            ExternalStreamImpl externalStream = (ExternalStreamImpl) stream;
//
//            if (isSubscribedToAllEntities)
//                source = externalStream.createSource(time, options, (IdentityKey[]) null);
//            else
//                source = externalStream.createSource(time, options, subscribedEntities.toArray());
//
//            mx.add (source, time);
//        }

        addSpecialReaders(stream, time);

        return true;
    }

    /// QuickMessageFilter implementation

    @Override
    public boolean              acceptAllEntities() {
        return isSubscribedToAllEntities;
    }

    @Override
    public boolean              acceptEntity(CharSequence symbol) {
        return isSubscribedToAllEntities || (isSubscribed(symbol));
    }

    private boolean             isSubscribed(CharSequence symbol) {
        return subscribedEntities.containsCharSequence(symbol);

//        if (type == InstrumentType.OPTION)
//            return subscribedEntities.contains(InstrumentType.OPTION, OptionFilter.getRootSymbol(symbol));
    }

    @Override
    public void                 setChangeListener(Runnable r) {

    }

    @Override
    public boolean              isRestricted() {
        return !isSubscribedToAllEntities && subscribedEntities.isEmpty();
    }

    @Override
    public boolean              subscribeToAllEntities(long timestamp) {

        assert Thread.holdsLock(mx);

        time = timestamp;

        isSubscribedToAllEntities = true;
        subscribedEntities.clear();

        return checkSource();
    }

    @Override
    public boolean              clearAllEntities() {

        assert Thread.holdsLock(mx);

        clearInternal();

        isSubscribedToAllEntities = false;
        subscribedEntities.clear();

        return true;
    }

    @Override
    public boolean              addEntities(long timestamp, Collection<IdentityKey> ids) {

        assert Thread.holdsLock(mx);

        time = timestamp;

        isSubscribedToAllEntities = false;
        for (IdentityKey id : ids)
            subscribedEntities.addCharSequence(id.getSymbol());

        //subscribedEntities.addAll(ids);
        checkSource();

        return true;
    }

    @Override
    public boolean              addEntities(long timestamp, IdentityKey[] ids) {
        assert Thread.holdsLock(mx);

        time = timestamp;

        isSubscribedToAllEntities = false;
        for (IdentityKey id : ids)
            subscribedEntities.addCharSequence(id.getSymbol());

        //subscribedEntities.addAll(ids);

        checkSource();

        return true;
    }

    @Override
    public boolean              removeEntities(IdentityKey[] ids) {
        assert Thread.holdsLock(mx);

        isSubscribedToAllEntities = false;
        for (IdentityKey id : ids)
            subscribedEntities.removeCharSequence(id.getSymbol());

        if (subscribedEntities.isEmpty())
            clearInternal();

        return true;
    }

    @Override
    public boolean              reset(long timestamp) {
        clearInternal();
        time = timestamp;

        return checkSource();
    }

    @Override
    public boolean              entityCreated(IdentityKey id) {
        return false;
    }

    @Override
    public boolean              spaceCreated(String space) {
        return false;
    }

    @Override
    public boolean              handle(MessageSource<?> feed, RuntimeException ex) {
        return false;
    }

    private void                clearInternal() {
        removeSpecialReaders();

        if (source != null)
            mx.closeAndRemove(source);
        source = null;
    }

    @Override
    public void                 close() {
        clearInternal();
    }
}