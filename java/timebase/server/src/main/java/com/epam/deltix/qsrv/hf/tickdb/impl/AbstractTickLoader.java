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

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.mon.InstrumentChannelStatSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Date;

abstract class AbstractTickLoader extends InstrumentChannelStatSet
        implements TickLoader<InstrumentMessage>, TBLoader, LoadingErrorListener, Closeable
{

    protected final TickStreamImpl                        stream;
    protected final LoadingOptions                        options;

    protected final ArrayList<LoadingErrorListener>       channelListeners =
            new ArrayList <LoadingErrorListener> ();
    protected volatile LoadingErrorListener []            channelListenerSnapshot = { };

    //
    //  Monitoring data
    //
    private volatile long                           closeTime = Long.MIN_VALUE;
    private final long                              monId;
    private final long                              openTime;

    AbstractTickLoader (TickStreamImpl stream, LoadingOptions options) {
        super (stream.getDBImpl ());

        if (options == null)
            options = new LoadingOptions ();

        this.stream = stream;
        this.options = options;

        openTime = TimeKeeper.currentTime;

        monId = stream.db == null ? -1 : stream.getDBImpl ().registerLoader (this);

        // pre-compile encoders/decoders
        if (!options.raw) {
            boolean             preferInterpreted = options.channelQOS == ChannelQualityOfService.MIN_INIT_TIME;

            RecordClassDescriptor[] rcds = DXTickStream.getClassDescriptors(stream);

            for (RecordClassDescriptor rcd : rcds) {
                CodecFactory factory = stream.getCodecFactory(preferInterpreted);
                try {
                    FixedBoundEncoder encoder = factory.createFixedBoundEncoder(options.getTypeLoader(), rcd);
                    assert encoder.hashCode() != 0; // to make compiler happy
                } catch (Exception e) {
                    // failed to preload decoder
                    continue;
                }

                try {
                    FixedExternalDecoder decoder = factory.createFixedExternalDecoder(options.getTypeLoader(), rcd);
                    assert decoder.hashCode() != 0; // to make compiler happy
                } catch (Exception e) {
                    // failed to preload encoder
                }
            }
        }

    }

    public WritableTickStream getTargetStream () {
        return (stream);
    }
    //
    //  TickLoader IMPLEMENTATION
    //
    public LoadingOptions       getOptions () {
        return (options);
    }

    public String               getTargetStreamKey () {
        return (stream.getKey ());
    }

    public Date getCloseDate () {
        return (closeTime == Long.MIN_VALUE ? null : new Date (closeTime));
    }

    public long                 getCloseTime () {
        return (closeTime);
    }

    public Date                 getOpenDate () {
        return (new Date (openTime));
    }

    public long                 getOpenTime () {
        return (openTime);
    }

    public long                 getId () {
        return (monId);
    }

    public abstract void        send (InstrumentMessage msg);

    @Override
    public synchronized void    removeUnique(InstrumentMessage msg) {
        if (stream.accumulator != null)
            stream.accumulator.remove(msg);
    }

    public void                 onError (LoadingError e) {
        LoadingOptions.ErrorAction action;

        synchronized (this) {
            action = options.getErrorAction (e.getClass());
        }

        if (action == LoadingOptions.ErrorAction.NotifyAndAbort) {
            for (LoadingErrorListener listener : channelListenerSnapshot)
                listener.onError (e);
            throw e;
        }

        if (action == LoadingOptions.ErrorAction.NotifyAndContinue) {
            for (LoadingErrorListener listener : channelListenerSnapshot)
                listener.onError (e);
        }
    }

    private void                updateListenerSnapshot () {
        int         n = channelListeners.size ();

        channelListenerSnapshot =
                channelListeners.toArray (new LoadingErrorListener [n]);
    }

    @Override
    public void                 addEventListener(LoadingErrorListener listener) {
        synchronized (channelListeners) {
            channelListeners.add (listener);
            updateListenerSnapshot ();
        }
    }

    @Override
    public void                 removeEventListener(LoadingErrorListener listener) {
        synchronized (channelListeners) {
            channelListeners.remove (listener);
            updateListenerSnapshot();
        }
    }

    @Override
    public void                 addSubscriptionListener(SubscriptionChangeListener listener) {
        stream.addSubscriptionListener(listener);
    }

    @Override
    public void                 removeSubscriptionListener(SubscriptionChangeListener listener) {
        stream.removeSubscriptionListener(listener);
    }

    public void                 close () {

        if (closeTime == Long.MIN_VALUE) {
            closeTime = TimeKeeper.currentTime;

            if (stream.db != null)
                stream.getDBImpl().unregisterLoader(this);
        }
    }

    @Override
    public String               toString () {
        return super.toString () + " ==> " + stream.getId ();
    }

    private LoadingErrorListener getLoadingErrorListener (
            Class <? extends LoadingMessageException> exceptionClass
    )
    {
        final LoadingOptions.ErrorAction action = options.getErrorAction (exceptionClass);

        return (
                action == LoadingOptions.ErrorAction.NotifyAndContinue || action == LoadingOptions.ErrorAction.NotifyAndAbort ?
                        this :
                        null
        );
    }
}