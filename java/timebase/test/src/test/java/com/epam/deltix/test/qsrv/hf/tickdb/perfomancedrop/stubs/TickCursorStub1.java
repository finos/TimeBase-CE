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
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop.stubs;

import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.blocks.InstrumentToObjectMap;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.CursorIsClosedException;

import java.util.Set;

/**
 * Stub class for performance testing. Replacement for {@link deltix.qsrv.hf.tickdb.impl.TickCursorImpl}.
 */
public class TickCursorStub1 /*implements
    TickCursor*/
{

    public final MessageSourceMultiplexer <InstrumentMessage> mx;

    private InstrumentToObjectMap<Byte>         subscribedEntities =
            new InstrumentToObjectMap<Byte>();

    private boolean                             isSubscribedToAllEntities = true;
    private Set <String>                        subscribedTypeNames = null;

    //
    //  Current message data, guarded by the virtual query thread
    //
    private InstrumentMessage currentMessage;
    private RecordClassDescriptor               currentType;
    private TickStream                          currentStream;
    private TypedMessageSource currentSource = null;


    private boolean                             inRealtime = false;
    private boolean                             realTimeNotifications = false;

    //
    //  Monitoring objects
    //
    private volatile boolean                    closed = false;


    public TickCursorStub1(boolean live) {
        //this.options = options;
        this.realTimeNotifications = false;

        mx = new MessageSourceMultiplexer<>(true, false);

        mx.setLive (live);

        if (live) // live mode should use allowLateOutOfOrder
            mx.setAllowLateOutOfOrder(true);
        else
            mx.setAllowLateOutOfOrder (false);

    }


    private boolean                      isSubscribed(IdentityKey iid) {
        // no allocations
        if (subscribedEntities.containsKey(iid))
            return true;

        return false;
    }

    public boolean              next () {
        boolean                 ret;
        RuntimeException        x = null;
        Runnable                lnr;
        //nextWatch.start();
        synchronized (mx) {

            for (;;) {
                //x1Watch.start();
                try {
                    boolean hasNext;

                    try {
                        hasNext = mx.syncNext();
                    } catch (RuntimeException xx) {
                        x = xx;
                        ret = false;    // make compiler happy
                        break;
                    }

                    if (!hasNext) {
                        ret = false;
                        break;
                    }
                } finally {
                    //x1Watch.stop();
                }


                //x2Watch.start();
                currentMessage = mx.syncGetMessage ();
                //x2Watch.stop();

                //x3Watch.start();
                try {
                    // current message is indicator of real-time mode
                    if (realTimeNotifications && mx.isRealTimeStarted()) {
                        currentType = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                        ret = inRealtime = true;
                        break;
                    }

                    if (!isSubscribedToAllEntities && !isSubscribed(currentMessage)) {
                        if (DebugFlags.DEBUG_MSG_DISCARD) {
                            DebugFlags.discard(
                                    "TB DEBUG: Discarding message " +
                                            currentMessage + " because we are not subscribed to its entity"
                            );
                        }

                        if (closed)
                            throw new CursorIsClosedException();

                        continue;
                    }
                } finally {
                    //x3Watch.stop();
                }


                //x4Watch.start();
                //x4a1Watch.start();

                final TypedMessageSource  source =
                    (TypedMessageSource) mx.syncGetCurrentSource ();

                //x4a1Watch.stop();

                //x4a2Watch.start();

                //x4a2Watch.start();
                currentStream = null;//((TickStreamRelated) source).getStream ();
                currentSource = source;
                //x4a2Watch.stop();

                //x4a3Watch.start();
                if (currentMessage.getClass () == RawMessage.class)
                    currentType = ((RawMessage) currentMessage).type;
                else
                    currentType = source.getCurrentType ();
                //x4a3Watch.stop();

                //x4Watch.stop();

                //x5Watch.start();
                try {
                    if (subscribedTypeNames != null &&
                            !subscribedTypeNames.contains(currentType.getName())) {
                        if (DebugFlags.DEBUG_MSG_DISCARD) {
                            DebugFlags.discard(
                                    "TB DEBUG: Discarding message " +
                                            currentMessage + " because we are not subscribed to its type"
                            );
                        }

                        if (closed)
                            throw new CursorIsClosedException();

                        continue;
                    }
                } finally {
                    //x5Watch.stop();
                }
                //x6Watch.start();
                //stats.register (currentMessage);

                ret = true;
                //x6Watch.stop();
                break;
            }
            //
            //  Surprisingly, even mx.next () can call the av lnr (on truncation)
            //
            //lnr = lnrTriggered ();
        }


        //x7Watch.start();
/*        if (lnr != null)
            lnr.run ();*/
        //x7Watch.stop();

        //nextWatch.stop();

        if (x != null)
            throw x;
        
        return (ret);
    }








    public InstrumentMessage        getMessage () {
        return (currentMessage);
    }



    @Override
    public String                           toString () {
        StringBuilder   sb = new StringBuilder ();

        sb.append ("TickCursorImpl(").append(this.hashCode()).append(") <== [");


        sb.append ("]");

        return (sb.toString ());
    }




    public void                 reset (long time) {
    }

    public boolean                  isRealTime() {
        return inRealtime;
    }

    public boolean                  realTimeAvailable() {
        return realTimeNotifications;
    }

    public void subscribeToAllEntities() {

    }

    public void close() {

    }
}