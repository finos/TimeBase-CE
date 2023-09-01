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

import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex4;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.SelectLimit;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;
import com.epam.deltix.util.collections.IndexedArrayList;

public class CompositePreparedQuery implements PreparedQuery {

    private static class InstrumentMessageSourceMultiplexer
        extends MessageSourceMultiplexer<InstrumentMessage>
        implements InstrumentMessageSource
    {

        protected static final int REJECT = 0;
        protected static final int ACCEPT = 1;
        protected static final int ABORT = -1;

        private final InstrumentMessageSource[] feeds;
        private final IndexedArrayList<String> streamKeyIndex = new IndexedArrayList<>();
        private final InstrumentIndex4 instrumentIndex = new InstrumentIndex4(64);
        private final CharSequenceToIntegerMap typeToIndex = new CharSequenceToIntegerMap();

        private int lastTypeIndex = 0;
        private int nullTypeIndex = -1;

        private long limit = Long.MIN_VALUE;
        private long offset = Long.MIN_VALUE;

        private InstrumentMessageSourceMultiplexer(boolean ascending, boolean realTimeNotification, boolean live,
                                                   InstrumentMessageSource[] feeds, SelectLimit limit) {

            super(ascending, realTimeNotification);
            this.feeds = feeds;
            if (limit != null) {
                this.limit = limit.getLimit();
                this.offset = limit.getOffset();
            }

            setLive(live);
            reset(feeds);
        }

        private void setSchema(ClassSet<RecordClassDescriptor> schema) {
            RecordClassDescriptor[] descriptors = schema.getContentClasses();
            for (RecordClassDescriptor descriptor : descriptors) {
                addType(descriptor);
            }
        }

        @Override
        public void add(CharSequence[] symbols, String[] types) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].add(symbols, types);
            }
        }

        @Override
        public void remove(CharSequence[] symbols, String[] types) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].remove(symbols, types);
            }
        }

        @Override
        public void subscribeToAllSymbols() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].subscribeToAllSymbols();
            }
        }

        @Override
        public void clearAllSymbols() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].clearAllSymbols();
            }
        }

        @Override
        public void addSymbol(CharSequence symbol) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addSymbol(symbol);
            }
        }

        @Override
        public void addSymbols(CharSequence[] symbols, int offset, int length) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addSymbols(symbols, offset, length);
            }
        }

        @Override
        public void removeSymbol(CharSequence symbol) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeSymbol(symbol);
            }
        }

        @Override
        public void removeSymbols(CharSequence[] symbols, int offset, int length) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeSymbols(symbols, offset, length);
            }
        }

        @Override
        public void subscribeToAllEntities() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].subscribeToAllEntities();
            }
        }

        @Override
        public void clearAllEntities() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].clearAllEntities();
            }
        }

        @Override
        public void addEntity(IdentityKey id) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addEntity(id);
            }
        }

        @Override
        public void addEntities(IdentityKey[] ids, int offset, int length) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addEntities(ids, offset, length);
            }
        }

        @Override
        public void removeEntity(IdentityKey id) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeEntity(id);
            }
        }

        @Override
        public void removeEntities(IdentityKey[] ids, int offset, int length) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeEntities(ids, offset, length);
            }
        }

        @Override
        public int getCurrentStreamIndex() {
            TickStream currentStream = getCurrentStream();
            return currentStream != null ? streamKeyIndex.getIndexOrAdd(currentStream.getKey()) : -1;
        }

        @Override
        public String getCurrentStreamKey() {
            InstrumentMessageSource source = getSource();
            return source != null ? source.getCurrentStreamKey() : null;
        }

        @Override
        public TickStream getCurrentStream() {
            InstrumentMessageSource source = getSource();
            return source != null ? source.getCurrentStream() : null;
        }

        @Override
        public int getCurrentEntityIndex() {
            return currentMessage != null ?
                instrumentIndex.getOrAdd(currentMessage.getSymbol()) :
                -1;
        }

        @Override
        public void addStream(TickStream... tickStreams) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addStream(tickStreams);
            }
        }

        @Override
        public void removeAllStreams() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeAllStreams();
            }
        }

        @Override
        public void removeStream(TickStream... tickStreams) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeStream(tickStreams);
            }
        }

        @Override
        public void setTimeForNewSubscriptions(long time) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribeToAllTypes() {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].subscribeToAllTypes();
            }
        }

        @Override
        public void setTypes(String... names) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].setTypes(names);
            }
        }

        @Override
        public void addTypes(String... names) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].addTypes(names);
            }
        }

        @Override
        public void removeTypes(String... names) {
            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].removeTypes(names);
            }
        }

        @Override
        public int getCurrentTypeIndex() {
            int index = -1;
            InstrumentMessageSource source = getSource();
            if (source != null) {
                index = addType(source.getCurrentType());
            }

            return index;
        }

        private int addType(RecordClassDescriptor type) {
            int index;
            if (type == null) {
                return -1;
            }

            if (type.getName() == null) {
                if (nullTypeIndex < 0) {
                    nullTypeIndex = lastTypeIndex++;
                }
                index = nullTypeIndex;
            } else {
                index = typeToIndex.get(type.getName(), -1);
                if (index < 0) {
                    typeToIndex.put(type.getName(), index = lastTypeIndex++);
                }
            }

            return index;
        }

        @Override
        public RecordClassDescriptor getCurrentType() {
            InstrumentMessageSource source = getSource();
            if (source == null) {
                return null;
            }

            return source.getCurrentType();
        }

        private InstrumentMessageSource getSource() {
            return (InstrumentMessageSource) getCurrentSource();
        }

        @Override
        public synchronized void reset(long time) {
            for (int i = 0; i < feeds.length; ++i) {
                remove(feeds[i]);
            }

            for (int i = 0; i < feeds.length; ++i) {
                feeds[i].reset(time);
            }

            for (int i = 0; i < feeds.length; ++i) {
                add(feeds[i]);
            }
        }

        @Override
        public boolean next() {
            for (;;) {
                boolean next = super.next();
                if (!next) {
                    return false;
                }

                int s = applyLimit();
                switch (s) {
                    case ABORT:  return false;
                    case ACCEPT: return true;
                }
            }
        }

        protected int applyLimit() {
            if (limit == Long.MIN_VALUE) {
                return ACCEPT;
            }

            if (limit <= 0) {
                return ABORT;
            }

            if (offset > 0) {
                offset--;
                return REJECT;
            }
            limit--;

            return ACCEPT;
        }

    }

    private final PreparedQuery[] subQueries;
    private final boolean isReverse;
    private final ClassSet<RecordClassDescriptor> schema;
    private final SelectLimit limit;

    private InstrumentMessageSourceMultiplexer multiplexer;

    public CompositePreparedQuery(PreparedQuery[] subQueries,
                                  boolean isForward,
                                  ClassSet<RecordClassDescriptor> schema,
                                  SelectLimit limit
    ) {
        this.subQueries = subQueries;
        this.isReverse = !isForward;
        this.schema = schema;
        this.limit = limit;
    }

    @Override
    public boolean isReverse() {
        return isReverse;
    }

    @Override
    public InstrumentMessageSource executeQuery(SelectionOptions options, ReadableValue[] params) {
        InstrumentMessageSource[] feeds = new InstrumentMessageSource[subQueries.length];
        for (int i = 0; i < subQueries.length; ++i) {
            feeds[i] = subQueries[i].executeQuery(options, params);
        }

        if (options == null) {
            options = new SelectionOptions();
        }

        multiplexer = new InstrumentMessageSourceMultiplexer(
            !options.reversed, options.realTimeNotification, options.live, feeds, limit
        );
        multiplexer.setSchema(schema);
        return multiplexer;
    }

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        return schema;
    }

    @Override
    public void close() {
        multiplexer.close();
    }
}