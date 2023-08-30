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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Implements the InstrumentMessageSource interface by delegation.
 */
public class DelegatingInstrumentMessageSource
    implements InstrumentMessageSource
{
    protected InstrumentMessageSource   source;

    public DelegatingInstrumentMessageSource (InstrumentMessageSource source) {
        this.source = source;
    }

    @Override
    public String toString () {
        return getClass ().getSimpleName () + " (" + source.toString () + ")";
    }

    public void removeEntity (IdentityKey id) {
        source.removeEntity (id);
    }

    public void removeEntities (
        IdentityKey []           ids,
        int                             offset,
        int                             length
    )
    {
        source.removeEntities (ids, offset, length);
    }

    public void clearAllEntities () {
        source.clearAllEntities ();
    }

    public void addEntity (IdentityKey id) {
        source.addEntity (id);
    }

    public void addEntities (
        IdentityKey []           ids,
        int                             offset,
        int                             length
    )
    {
        source.addEntities (ids, offset, length);
    }

    public void subscribeToAllEntities () {
        if (!adjustSymbolsSubscription()) {
            source.subscribeToAllEntities ();
        }
    }

    private boolean adjustSymbolsSubscription() {
        String[] symbolsToAdjust = symbolsToAdjust();
        if (symbolsToAdjust == null) {
            return false;
        } else {
            addSymbols(symbolsToAdjust);
            return true;
        }
    }

    protected String[] symbolsToAdjust() {
        return null;
    }

    public void close () {
        source.close ();
    }

    public boolean next () {
        return source.next ();
    }

    public boolean isAtEnd () {
        return source.isAtEnd ();
    }

    public InstrumentMessage getMessage () {
        return source.getMessage ();
    }

    public void removeStream (TickStream... tickStreams) {
        source.removeStream (tickStreams);
    }

    public void removeAllStreams () {
        source.removeAllStreams ();
    }

    public void addStream (TickStream... tickStreams) {
        source.addStream (tickStreams);
    }

    public int getCurrentStreamIndex () {
        return source.getCurrentStreamIndex ();
    }

    public boolean isClosed () {
        return source.isClosed ();
    }   

    public int getCurrentTypeIndex () {
        return source.getCurrentTypeIndex ();
    }

    public RecordClassDescriptor getCurrentType () {
        return source.getCurrentType ();
    }

    public String getCurrentStreamKey () {
        return source.getCurrentStreamKey ();
    }

    public TickStream getCurrentStream () {
        return source.getCurrentStream ();
    }

    public int getCurrentEntityIndex () {
        return source.getCurrentEntityIndex ();
    }

    public void subscribeToAllTypes () {
        source.subscribeToAllTypes ();
    }

    public void removeTypes (String ... types) {
        source.removeTypes (types);
    }

    public void addTypes (String ... types) {
        source.addTypes (types);
    }

    @Override
    public void setTypes(String... names) {
        source.setTypes(names);
    }

    @Override
    public void add(IdentityKey[] ids, String[] types) {
        source.add(ids, types);
    }

    @Override
    public void remove(IdentityKey[] ids, String[] types) {
        source.remove(ids, types);
    }

    @Override
    public void add(CharSequence[] symbols, String[] types) {
        source.add(symbols, types);
    }

    @Override
    public void remove(CharSequence[] symbols, String[] types) {
        source.remove(symbols, types);
    }

    @Override
    public void subscribeToAllSymbols() {
        if (!adjustSymbolsSubscription()) {
            source.subscribeToAllSymbols();
        }
    }

    @Override
    public void clearAllSymbols() {
        source.clearAllSymbols();
    }

    @Override
    public void addSymbol(CharSequence symbol) {
        source.addSymbol(symbol);
    }

    @Override
    public void addSymbols(CharSequence[] symbols, int offset, int length) {
        source.addSymbols(symbols, offset, length);
    }

    @Override
    public void removeSymbol(CharSequence symbol) {
        source.removeSymbol(symbol);
    }

    @Override
    public void removeSymbols(CharSequence[] symbols, int offset, int length) {
        source.removeSymbols(symbols, offset, length);
    }

    @Override
    public void addSymbols(CharSequence[] symbols) {
        source.addSymbols(symbols);
    }

    @Override
    public void removeSymbols(CharSequence[] symbols) {
        source.removeSymbols(symbols);
    }

    protected long adjustResetPoint (long time) {
        return (time);
    }
    
    public void reset (long time) {
        source.reset (adjustResetPoint (time));
    }

    public void setTimeForNewSubscriptions (long time) {
        source.setTimeForNewSubscriptions (adjustResetPoint (time));
    }

    public void setAvailabilityListener (Runnable lnr) {
        source.setAvailabilityListener (lnr);
    }

    @Override
    public boolean isRealTime() {
        return source.isRealTime();
    }

    @Override
    public boolean realTimeAvailable() {
        return source.realTimeAvailable();
    }
}