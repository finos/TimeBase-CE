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
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SymbolAndTypeSubscriptionControllerAdapter;

/**
 * @author Alexei Osipov
 */
public class StubTimeStreamCursor implements TickCursor, SymbolAndTypeSubscriptionControllerAdapter {

    private final RawMessage message = new RawMessage();

    private final StubTimeStream stream;

    public StubTimeStreamCursor(StubTimeStream stream) {
        this.stream = stream;

        message.setSymbol(StubTimeStream.STUB_IDENTITY.getSymbol());
        message.setTimeStampMs(Long.MIN_VALUE);
        message.type = StubTimeStream.STUB_RCD;
        message.data = new byte[0];
    }

    @Override
    public int getCurrentTypeIndex() {
        return 0;
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        return StubTimeStream.STUB_RCD;
    }

    @Override
    public void setTimeForNewSubscriptions(long time) {

    }

    @Override
    public void reset(long time) {
        if (time < TimeCodec.BASE) {
            // Value is too low to be encoded by time code. So start from base time.
            time = TimeCodec.BASE;
        }
        message.setTimeStampMs(time);
    }

    @Override
    public void subscribeToAllTypes() {

    }

    @Override
    public void setTypes(String... names) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addTypes(String... names) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeTypes(String... names) {
        throw new UnsupportedOperationException ();
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
    public int getCurrentEntityIndex() {
        return 0;
    }

    @Override
    public void subscribeToAllEntities() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void clearAllEntities() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addEntity(IdentityKey id) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addEntities(IdentityKey[] ids, int offset, int length) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeEntity(IdentityKey id) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeEntities(IdentityKey[] ids, int offset, int length) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public boolean isRealTime() {
        return false;
    }

    @Override
    public boolean realTimeAvailable() {
        return false;
    }

    @Override
    public void add(IdentityKey[] entities, String[] types) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void remove(IdentityKey[] entities, String[] types) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void addStream(TickStream... tickStreams) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeAllStreams() {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void removeStream(TickStream... tickStreams) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public RawMessage getMessage() {
        return message;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean next() {
        long prevTimeStamp = message.getTimeStampMs();
        if (prevTimeStamp == Long.MAX_VALUE) {
            return false;
        }
        message.setTimeStampMs(prevTimeStamp + 1);
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return message.getTimeStampMs() == Long.MAX_VALUE;
    }

    @Override
    public void setAvailabilityListener(Runnable maybeAvailable) {

    }

    @Override
    public void close() {

    }
}
