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
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.qsrv.hf.stream.AbstractMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

import java.io.IOException;

public class StreamVersionsReader extends AbstractMessageReader
        implements
        MessageSource<InstrumentMessage>,
        TickStreamRelated,
        IntermittentlyAvailableResource {

    private volatile Runnable           availabilityListener = null;
    StreamVersionsContainer             container;

    private QuickExecutor.QuickTask     notifier;
    private InstrumentMessage           message = null;
    private long                        time;
    volatile boolean                    waiting = false;
    final boolean                       live;
    private RawReader                   reader;

    public StreamVersionsReader(StreamVersionsContainer container,
                                RawReader reader,
                                long time,
                                SelectionOptions options)
    {
        this.container = container;
        this.reader = reader;
        this.live = options.live;
        this.time = time;

        this.notifier = new QuickExecutor.QuickTask (container.getStream().getQuickExecutor()) {
                @Override
                public void run () {
                    final Runnable          lnr = availabilityListener;

                    if (lnr == null)
                        synchronized (this) {
                            this.notifyAll ();
                        }
                    else
                        lnr.run ();
                }
        };

        this.types = container.getTypes();

        TypeLoader loader = options.getTypeLoader();

        if (!options.raw) {
            final int       numTypes = this.types.length;

            decoders = new FixedExternalDecoder[numTypes];
            messages = new InstrumentMessage [numTypes];

            for (int i = 0; i < numTypes; i++) {
                FixedExternalDecoder d =
                        decoders[i] = CodecFactory.COMPILED.createFixedExternalDecoder(loader, types[i]);
                InstrumentMessage msg =
                        messages[i] = (InstrumentMessage) types[i].newInstanceNoX(loader);
                d.setStaticFields (msg);
            }
        } else {
            rawMsg = new RawMessage();
            rawMsg.setSymbol(symbol);
        }
    }

    public final void                       submitNotifier () {
        waiting = false;
        notifier.submit ();
    }

    @Override
    public InstrumentMessage                getMessage() {
        return message;
    }
    
    @Override
    public synchronized boolean              next() {
        try {
            boolean hasNext;

            while (hasNext = reader.read()) {
                message = decode(reader.getInput());
                if (message.getTimeStampMs() >= time)
                    break;
            }
            return hasNext;

        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } catch (UnavailableResourceException x) {
            waiting = true;
            throw x;
        }
    }

//    void                 readMessage(int ind) {
//        byte[] bytes = container.getData(ind);
//
//        buffer.setBytes(bytes, 0, bytes.length);
//
//        try {
//            message = decode(buffer);
//        } catch (IOException e) {
//            throw new com.epam.deltix.util.io.UncheckedIOException(e);
//        }
//    }

    @Override
    public boolean          isAtEnd() {
        return false;
    }

    @Override
    public void             close() {
        reader.close();
        container.onClose(this);
    }

    @Override
    public void             setAvailabilityListener(Runnable maybeAvailable) {
        availabilityListener = maybeAvailable;
    }

    @Override
    public TickStream           getStream() {
        return container.getStream();
    }
}
