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

import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.concurrent.*;

import java.io.*;

/**
 *  Adapts RawReaderBase to MessageSource &lt;InstrumentMessage&gt; by decoding.
 */
class TickReader implements TickStreamReader {
    final QueueMessageReader                rawReader;
    protected Filter                        filter;
    protected final RecordDecoder           decoder;
    protected long                          startTime;
    protected boolean                       isAtEnd = false;
    protected InstrumentMessage             curMsg = null;
    protected RecordClassDescriptor         currentType;

    private InstrumentMessage               liveMessage;
    private boolean                         isRealtime = false;
    private boolean                         realTimeNotification = false;

    TickReader (
            Filter                              filter,
            RecordDecoder                       decoder,
            QueueMessageReader                  rawReader,
            long                                startTime,
            SelectionOptions                    options
    )
    {
        this.filter = filter;
        this.rawReader = rawReader;
        this.decoder = decoder;
        this.startTime = startTime;
        this.realTimeNotification = options.realTimeNotification;

        liveMessage = realTimeNotification ? TickStreamImpl.createRealTimeStartMessage(options.raw, startTime) : null;
    }

    public void                 setAvailabilityListener (Runnable lnr) {
        rawReader.setAvailabilityListener (lnr);
    }

    public DXTickStream getStream () {
        return (rawReader.getStream ());
    }

    public InstrumentMessage    getMessage () {
        return (curMsg);
    }

    public boolean              isAtEnd () {
        return (isAtEnd);
    }

    @Override
    public boolean              isRealTime() {
        return  isRealtime;
    }

    @Override
    public boolean              realTimeAvailable() {
        return rawReader.isLive();
    }

    @SuppressWarnings ("unchecked")
    public boolean              next () {
        try {
            for (;;) {
                if (!rawReader.read ()) {
                        isAtEnd = true;
                        return (false);
                    }

                    long            ts = rawReader.getTimestamp ();

                    if (ts < startTime) {
                        if (DebugFlags.DEBUG_MSG_DISCARD)
                            DebugFlags.discard (
                                "Discarding message " + rawReader + " because timestamp " + ts +
                                    " is earlier than required " + startTime
                            );

                    continue;
                }

                curMsg = (InstrumentMessage) decoder.decode (filter, rawReader.getInput ());
                currentType = decoder.getCurrentType();

                if (curMsg != null) {
                    curMsg.setNanoTime(rawReader.getNanoTime());
                    return (true);
                }
            }
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } catch (UnavailableResourceException r) {
            if (realTimeNotification && !isRealtime && (rawReader.isTransient() || rawReader.available() == 0)) {
                isRealtime = true;
                liveMessage.setNanoTime(curMsg != null ? curMsg.getNanoTime() : Long.MIN_VALUE);
                curMsg = liveMessage;
                currentType = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                return true;
            }

            throw r;
        }
    }
   
    public void                     reset(long time) {
        startTime = time;
    }
  
    public int                      getCurrentTypeIndex () {
        return decoder.getCurrentTypeIndex();
    }

    public RecordClassDescriptor    getCurrentType () {
        return currentType;
    }

//    public RecordClassDescriptor [] getMessageTypes() {
//        return decoder.getMessageTypes();
//    }

    public void                     close () {
        Util.close (rawReader);
    }

    @Override
    public String                   toString () {
        return ("TickReader (" + rawReader + ")");
    }
}