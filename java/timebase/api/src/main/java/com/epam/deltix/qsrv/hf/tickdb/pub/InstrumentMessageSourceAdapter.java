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

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;

/**
 * Utility classes that delegates InstrumentMessageSource interface
 */
public class InstrumentMessageSourceAdapter extends MessageSourceAdapter<InstrumentMessage> implements InstrumentMessageSource {

    public InstrumentMessageSourceAdapter(InstrumentMessageSource delegate) {
        super(delegate);
    }

    @Override
    protected InstrumentMessageSource getDelegate() {
        return (InstrumentMessageSource) super.getDelegate();
    }

    @Override
    public InstrumentMessage getMessage() {
        return getDelegate().getMessage();
    }

    @Override
    public boolean isClosed() {
        return getDelegate().isClosed();
    }

    @Override
    public boolean next() {
        return getDelegate().next();
    }

    @Override
    public boolean isAtEnd() {
        return getDelegate().isAtEnd();
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    public void subscribeToAllEntities() {
        getDelegate().subscribeToAllEntities();
    }

    @Override
    public void clearAllEntities() {
        getDelegate().clearAllEntities();
    }

    @Override
    public void addEntity(IdentityKey id) {
        getDelegate().addEntity(id);
    }

    @Override
    public void addEntities(IdentityKey[] ids, int offset, int length) {
        getDelegate().addEntities(ids, offset, length);
    }

    @Override
    public void removeEntity(IdentityKey id) {
        getDelegate().removeEntity(id);
    }

    @Override
    public void removeEntities(IdentityKey[] ids, int offset, int length) {
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
    public void subscribeToAllTypes() {
        getDelegate().subscribeToAllTypes();
    }

    @Override
    public void setTypes(String... names) {
        getDelegate().setTypes(names);
    }

    @Override
    public void addTypes(String... names) {
        getDelegate().addTypes(names);
    }

    @Override
    public void removeTypes(String... names) {
        getDelegate().removeTypes(names);
    }

    @Override
    public void add(IdentityKey[] ids, String[] types) {
        getDelegate().add(ids, types);
    }

    @Override
    public void remove(IdentityKey[] ids, String[] types) {
        getDelegate().remove(ids, types);
    }

    @Override
    public void add(CharSequence[] symbols, String[] types) {
        getDelegate().add(symbols, types);
    }

    @Override
    public void remove(CharSequence[] symbols, String[] types) {
        getDelegate().remove(symbols, types);
    }

    @Override
    public void subscribeToAllSymbols() {
        getDelegate().subscribeToAllSymbols();
    }

    @Override
    public void clearAllSymbols() {
        getDelegate().clearAllSymbols();
    }

    @Override
    public void addSymbol(CharSequence symbol) {
        getDelegate().addSymbol(symbol);
    }

    @Override
    public void addSymbols(CharSequence[] symbols, int offset, int length) {
        getDelegate().addSymbols(symbols, offset, length);
    }

    @Override
    public void removeSymbol(CharSequence symbol) {
        getDelegate().removeSymbol(symbol);
    }

    @Override
    public void removeSymbols(CharSequence[] symbols, int offset, int length) {
        getDelegate().removeSymbols(symbols, offset, length);
    }

    @Override
    public void addSymbols(CharSequence[] symbols) {
        getDelegate().addSymbols(symbols);
    }

    @Override
    public void removeSymbols(CharSequence[] symbols) {
        getDelegate().removeSymbols(symbols);
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
    public int getCurrentStreamIndex() {
        return getDelegate().getCurrentStreamIndex();
    }

    @Override
    public String getCurrentStreamKey() {
        return getDelegate().getCurrentStreamKey();
    }

    @Override
    public TickStream getCurrentStream() {
        return getDelegate().getCurrentStream();
    }

    @Override
    public int getCurrentEntityIndex() {
        return getDelegate().getCurrentEntityIndex();
    }

    @Override
    public int getCurrentTypeIndex() {
        return getDelegate().getCurrentTypeIndex();
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        return getDelegate().getCurrentType();
    }

    @Override
    public void setAvailabilityListener(Runnable maybeAvailable) {
        getDelegate().setAvailabilityListener(maybeAvailable);
    }

    @Override
    public boolean isRealTime() {
        return getDelegate().isRealTime();
    }

    @Override
    public boolean realTimeAvailable() {
        return getDelegate().realTimeAvailable();
    }
}