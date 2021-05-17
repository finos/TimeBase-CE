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
package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;


import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public abstract class MessageMapper<K, V> extends Mapper<LongWritable, InstrumentMessage, K, V> {

    public abstract void process(MessageSource<InstrumentMessage> source, Context context)
            throws IOException, InterruptedException;

    @Override
    public final void run(Context context) throws IOException, InterruptedException {
        setup(context);
        try {
            process(new Source(context), context);
        } finally {
            cleanup(context);
        }
    }

    @Override
    protected final void map(LongWritable key, InstrumentMessage value, Context context) throws IOException, InterruptedException {
         throw new UnsupportedOperationException("Map is not implemented");
    }

    private class Source implements MessageSource<InstrumentMessage> {

        private final Context       context;
        private boolean             hasNext;

        public Source(Context context) {
            this.context = context;
        }

        @Override
        public InstrumentMessage getMessage() {
            try {
                return context.getCurrentValue();
            } catch (IOException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }

        @Override
        public boolean next() {
            try {
                return hasNext = context.nextKeyValue();
            } catch (IOException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }

        @Override
        public boolean isAtEnd() {
            return hasNext;
        }

        @Override
        public void close() {

        }
    }
}
