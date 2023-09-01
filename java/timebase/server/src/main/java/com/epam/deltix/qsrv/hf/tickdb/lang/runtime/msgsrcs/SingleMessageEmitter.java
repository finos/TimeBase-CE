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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Emits a single empty message.
 */
public class SingleMessageEmitter implements InstrumentMessageSource,
        SymbolAndTypeSubscriptionControllerAdapter {
    public static final String                  VOID_GUID = "void.0";
    public static final String                  VOID_NAME = "VOID";
    public static final RecordClassDescriptor   VOID_TYPE =
        new RecordClassDescriptor (VOID_GUID, VOID_NAME, null, false, null);
    
    private final RawMessage            msg = new RawMessage ();
    private int                         nextCalled = 0;
    private boolean                     closed = false;
    
    public SingleMessageEmitter () {
        msg.setSymbol("");
        msg.setTimeStampMs(TimeConstants.TIMESTAMP_UNKNOWN);
        msg.type = VOID_TYPE;
        msg.setBytes(new byte[0], 0, 0);
    }
    
    public InstrumentMessage getMessage () {
        return (msg);
    }

    public boolean                      isClosed () {
        return (closed);
    }

    public boolean                      isAtEnd () {
        return (nextCalled == 2);
    }

    public void                         reset (long time) {
        nextCalled = 0;
    }

    public boolean                      next () {
        switch (nextCalled) {
            case 0:
                nextCalled = 1;
                return (true);
                
            case 1:
                nextCalled = 2;
                return (false);
                
            default:
                throw new IllegalStateException ("next () on finished cursor");
        }
    }

    public void                         close () {
        closed = true;
    }

    @Override
    public void                         add(IdentityKey[] ids, String[] types) {
        throw new UnsupportedOperationException ();
    }

    @Override
    public void                         remove(IdentityKey[] ids, String[] types) {
        throw new UnsupportedOperationException ();
    }

    public void                         addEntities (IdentityKey[] ids, int offset, int length) {
        throw new UnsupportedOperationException ();
    }

    public void                         addEntity (IdentityKey id) {
        throw new UnsupportedOperationException ();
    }

    public void                         clearAllEntities () {
        throw new UnsupportedOperationException ();
    }

    public void                         removeEntities (IdentityKey[] ids, int offset, int length) {
        throw new UnsupportedOperationException ();
    }

    public void                         removeEntity (IdentityKey id) {
        throw new UnsupportedOperationException ();
    }

    public void                         subscribeToAllEntities () {        
    }

    public void                         addStream (TickStream... tickStreams) {
        throw new UnsupportedOperationException ();
    }

    public void                         removeAllStreams () {
        throw new UnsupportedOperationException ();
    }

    public void                         removeStream (TickStream... tickStreams) {
        throw new UnsupportedOperationException ();
    }

    public void                         addTypes (String... names) {
        throw new UnsupportedOperationException ();
    }

    public void                         removeTypes (String... names) {
        throw new UnsupportedOperationException ();
    }

    public void                         setTypes (String... names) {
        throw new UnsupportedOperationException ();
    }

    public void                         subscribeToAllTypes () {        
    }

    public void                         setTimeForNewSubscriptions (long time) {
    }

    public int                          getCurrentEntityIndex () {
        return (0);
    }

    public TickStream                   getCurrentStream () {
        return (null);
    }

    public int                          getCurrentStreamIndex () {
        return (-1);
    }

    public String                       getCurrentStreamKey () {
        return (null);
    }

    public RecordClassDescriptor        getCurrentType () {
        return (VOID_TYPE);
    }

    public int                          getCurrentTypeIndex () {
        return (0);
    }

    public void                         setAvailabilityListener (Runnable maybeAvailable) {
    }

    @Override
    public boolean                      isRealTime() {
        return false;
    }

    @Override
    public boolean                      realTimeAvailable() {
        return false;
    }
}