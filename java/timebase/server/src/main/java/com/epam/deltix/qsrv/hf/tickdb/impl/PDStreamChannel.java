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

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.dtb.store.pub.DataWriter;
import com.epam.deltix.qsrv.dtb.store.pub.IllegalMessageAppend;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.PredicateCompiler;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CompilerUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.MessagePredicate;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.OutOfSequenceMessageException;
import com.epam.deltix.qsrv.hf.tickdb.pub.WriterClosedException;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeKeeper;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.Flushable;
import java.io.IOException;

/**
 *
 */
class PDStreamChannel implements MessageChannel<InstrumentMessage>, Flushable {

    private final TSRoot                                root;

    private DataWriter                                  writer;
    private PDStream                                    stream;

    private final MessageProducer                       producer;
    private boolean                                     truncate;
    private LoadingOptions.WriteMode                    mode;
    private final RegistryCache                         cache;
    private boolean                                     opened = false;
    private final MutableBoolean                        exists = new MutableBoolean(false);
    private MessagePredicate                            filter;

    PDStreamChannel(PDStream stream, TSRoot root, DataWriter writer,
                    MessageProducer<? extends InstrumentMessage> producer,
                    LoadingOptions options) {

        this.root = root;
        this.writer = writer;
        this.stream = stream;
        this.mode = options.writeMode;
        this.producer = producer;
        this.truncate = mode != LoadingOptions.WriteMode.APPEND &&
                mode != LoadingOptions.WriteMode.INSERT;

        this.cache = new RegistryCache(root.getSymbolRegistry());

        if (!StringUtils.isEmpty(options.filterExpression)) {
            PredicateCompiler pc =
                    stream.isPolymorphic () ?
                            new PredicateCompiler (stream.getPolymorphicDescriptors ()) :
                            new PredicateCompiler (stream.getFixedType ());

            this.filter = pc.compile(CompilerUtil.parseExpression(options.filterExpression));
        }

        stream.writerCreated(writer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void            send(InstrumentMessage msg) {
        if (writer == null) // closed state
            throw new WriterClosedException(this + " is closed");

        if (!stream.accumulateIfRequired (msg))
            return;

        boolean undefined = msg.getTimeStampMs() == TimeStamp.TIMESTAMP_UNKNOWN;

        long nstime = undefined ? TimeKeeper.currentTimeNanos : msg.getNanoTime();

        assert nstime != Long.MAX_VALUE; // temporary

        if (!opened) {
            writer.open(nstime, null);
            opened = true;
            stream.firePropertyChanged(TickStreamProperties.TIME_RANGE);
        }

        int index = cache.encode(msg, exists);
        int type = producer.beginWrite(msg);

        if (truncate) {
            // truncation should be done for the first message

            if (!undefined && !exists.booleanValue()) {
                stream.truncateInternal(root, nstime, msg);
                writer.truncate(nstime, index);
            }
        }

        if (!exists.booleanValue())
            stream.firePropertyChanged(TickStreamProperties.ENTITIES);

        try {
            if (undefined)
                writer.insertMessage(index, nstime, type, producer);
            else if (mode == LoadingOptions.WriteMode.INSERT)
                writer.insertMessage(index, nstime, type, producer);
            else
                writer.appendMessage(index, nstime, type, producer, false);
        } catch (IllegalMessageAppend e) {
            throw new OutOfSequenceMessageException(msg, nstime, stream.getKey(), e.getLastWrittenNanos());
        }
    }

    @Override
    public synchronized void            close() {
        Util.close(writer);
        stream.writerClosed(writer);
        writer = null;

        if (opened) {
            stream.firePropertyChanged(TickStreamProperties.TIME_RANGE);
            stream.firePropertyChanged(TickStreamProperties.ENTITIES);
        }
        opened = false;
    }

    @Override
    public synchronized void flush() throws IOException {
        // we need just close writer, because it will be reopened on next send()
        Util.close(writer);
        opened = false;
    }
}