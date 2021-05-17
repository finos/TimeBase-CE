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

import com.epam.deltix.qsrv.dtb.store.pub.DataReader;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.SymbolRegistry;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.lang.Util;

class PDStreamReader implements TickStreamReader {

    private final PDStream                  stream;
    private final DataReader                reader;
    private final TSRoot                    root;
    private boolean                         hasNext = false;

    private final MessageConsumer<? extends InstrumentMessage> consumer;

    private DisposableListener<PDStreamReader> listener;

    PDStreamReader(PDStream stream, TSRoot root, DataReader reader, MessageConsumer<? extends InstrumentMessage> consumer) {
        this.stream = stream;
        this.reader = reader;
        this.consumer = consumer;
        this.root = root;
    }

    public SymbolRegistry       getSymbols() {
        return root.getSymbolRegistry();
    }

    @Override
    public void         reset(long timestamp) {
        reader.reopen(TimeStamp.getNanoTime(timestamp));
    }

    @Override
    public boolean isRealTime() {
        return consumer.isRealTime();
    }

    @Override
    public boolean realTimeAvailable() {
        return consumer.realTimeAvailable();
    }

    @Override
    public InstrumentMessage getMessage() {
        return consumer.getMessage();
    }

    @Override
    public boolean next() {
        return (hasNext = reader.readNext(consumer));
    }

    @Override
    public boolean isAtEnd() {
        return !hasNext;
    }

    @Override
    public void close() {
        Util.close(reader);
        stream.readerClosed(reader);

        // disposed event notification
        if (listener != null)
            listener.disposed(this);
    }

    public void setAvailabilityListener(Runnable maybeAvailable) {
        reader.setAvailabilityListener(maybeAvailable);
    }

    public long                 getStartTimestamp() {
        return reader.getStartTimestamp();
    }

    public long                 getEndTimestamp() {
        return reader.getEndTimestamp();
    }

    public void                 setFilter(EntityFilter filter) {
        reader.setFilter(filter);
    }

    @Override
    public TickStream getStream() {
        return stream;
    }

    @Override
    public int getCurrentTypeIndex() {
        return consumer.getCurrentTypeIndex();
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        return consumer.getCurrentType();
    }

    public void                 setLimitTimestamp(long timestamp) {
        reader.setLimitTimestamp(timestamp);
    }

    public void                 setDisposableListener(DisposableListener<PDStreamReader> listener) {
        this.listener = listener;
    }
}
