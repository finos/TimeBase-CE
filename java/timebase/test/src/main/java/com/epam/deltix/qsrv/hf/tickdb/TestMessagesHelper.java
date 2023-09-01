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

package com.epam.deltix.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.*;
import com.epam.deltix.util.annotations.TimestampMs;

import java.util.function.Consumer;

public class TestMessagesHelper {

    private final Generator generator;
    private final RecordClassDescriptor rcd;
    private final RecordClassDescriptor simpleRcd;
    private final RecordClassDescriptor allSimpleNumericsRcd;
    private final RecordClassDescriptor allNumericsRcd;

    public TestMessagesHelper(Generator generator) {
        this.generator = generator;
        Introspector introspector = Introspector.createEmptyMessageIntrospector();
        try {
            rcd = introspector.introspectRecordClass(AllTypesMessage.class);
            simpleRcd = introspector.introspectRecordClass(AllSimpleTypesMessage.class);
            allSimpleNumericsRcd = introspector.introspectRecordClass(AllSimpleNumericsMessage.class);
            allNumericsRcd = introspector.introspectRecordClass(AllNumericsMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

//    public DXTickStream createAllNumericsStream(DXTickDB db, String key) {
//
//    }

    public DXTickStream createStream(DXTickDB db, String key) {
        deleteIfExists(db, key);
        StreamOptions options = new StreamOptions(StreamScope.DURABLE, key, key, StreamOptions.MAX_DISTRIBUTION);
        options.setPolymorphic(rcd, simpleRcd);
        return db.createStream(key, options);
    }

    public DXTickStream createSimpleStream(DXTickDB db, String key) {
        deleteIfExists(db, key);
        StreamOptions options = new StreamOptions(StreamScope.DURABLE, key, key, StreamOptions.MAX_DISTRIBUTION);
        options.setFixedType(simpleRcd);
        return db.createStream(key, options);
    }

    public DXTickStream createVarcharListStream(DXTickDB db, String key) {
        deleteIfExists(db, key);
        StreamOptions options = new StreamOptions(StreamScope.DURABLE, key, key, StreamOptions.MAX_DISTRIBUTION);
        return db.createStream(key, options);
    }

    public void loadMessages(DXTickStream stream, int n) {
        try (TickLoader loader = stream.createLoader()) {
            AllTypesMessage message = new AllTypesMessage();
            for (int i = 0; i < n; i++) {
                generator.fillAllTypesMessage(message);
                loader.send(message);
            }
        }
    }

    public AllTypesMessage nextMessage() {
        return generator.nextMessage();
    }

    public void loadMessages(DXTickStream stream, @TimestampMs long startTime, long step, int n) {
        long timestamp = startTime;
        try (TickLoader loader = stream.createLoader()) {
            AllTypesMessage message = new AllTypesMessage();
            for (int i = 0; i < n; i++, timestamp += step) {
                generator.fillAllTypesMessage(message);
                message.setTimeStampMs(timestamp);
                loader.send(message);
            }
        }
    }

    public void loadMessages(DXTickStream stream, @TimestampMs long startTime, @TimestampMs long endTime, long step) {
        try (TickLoader loader = stream.createLoader()) {
            AllTypesMessage message = new AllTypesMessage();
            for (long timestamp = startTime; timestamp <= endTime; timestamp += step) {
                generator.fillAllTypesMessage(message);
                message.setTimeStampMs(timestamp);
                loader.send(message);
            }
        }
    }

    public void loadMessages(DXTickStream stream, @TimestampMs long startTime, @TimestampMs long endTime, long step, String ... symbols) {
        try (TickLoader loader = stream.createLoader()) {
            AllTypesMessage message = new AllTypesMessage();
            int i = 0;
            for (long timestamp = startTime; timestamp <= endTime; timestamp += step,
                    i = i + 1 == symbols.length ? 0: i + 1) {
                generator.fillAllTypesMessage(message);
                message.setTimeStampMs(timestamp);
                message.setSymbol(symbols[i]);
                loader.send(message);
            }
        }
    }

    public void loadMessages(DXTickStream stream, @TimestampMs long startTime, @TimestampMs long endTime, long step, long dataInterval, long gapInterval, String ... symbols) {
        try (TickLoader loader = stream.createLoader()) {
            AllTypesMessage message = new AllTypesMessage();
            int i = 0;
            long endData = startTime + dataInterval;
            for (long timestamp = startTime; timestamp <= endTime; timestamp += step,
                    i = i + 1 == symbols.length ? 0 : i + 1) {
                if (timestamp <= endData) {
                    generator.fillAllTypesMessage(message);
                    message.setTimeStampMs(timestamp);
                    message.setSymbol(symbols[i]);
                    loader.send(message);
                } else {
                    timestamp = endData + gapInterval;
                    endData = timestamp + dataInterval;
                }
            }
        }
    }

    public void loadSimpleMessages(DXTickStream stream, int n) {
        try (TickLoader loader = stream.createLoader()) {
            for (int i = 0; i < n; i++) {
                loader.send(generator.nextSimpleMessage());
            }
        }
    }

    public void readMessages(DXTickDB db, String key, Consumer<AllTypesMessage> consumer, boolean compiledCodecs) {
        DXTickStream stream = db.getStream(key);
        if (stream == null) {
            throw new RuntimeException("Unknown stream " + key);
        }
        SelectionOptions selectionOptions = new SelectionOptions();
        if (!compiledCodecs) {
            selectionOptions.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        }
        try (TickCursor cursor = db.select(Long.MIN_VALUE, selectionOptions, stream)) {
            while (cursor.next()) {
                consumer.accept((AllTypesMessage) cursor.getMessage());
            }
        }
    }

    public void readRawMessages(DXTickDB db, String key, Consumer<RawMessage> consumer) {
        DXTickStream stream = db.getStream(key);
        if (stream == null) {
            throw new RuntimeException("Unknown stream " + key);
        }
        SelectionOptions selectionOptions = new SelectionOptions(true, false);
        try (TickCursor cursor = db.select(Long.MIN_VALUE, selectionOptions, stream)) {
            while (cursor.next()) {
                consumer.accept((RawMessage) cursor.getMessage());
            }
        }
    }

    public void loadMessages(DXTickStream stream1, DXTickStream stream2, int n) {
        try (TickLoader loader1 = stream1.createLoader();
             TickLoader loader2 = stream2.createLoader()) {
            for (int i = 0; i < n; i++) {
                AllTypesMessage message = generator.nextMessage();
                loader1.send(message);
                loader2.send(message);
            }
        }
    }

    public void loadSimpleMessages(DXTickStream stream1, DXTickStream stream2, int n) {
        try (TickLoader loader1 = stream1.createLoader();
             TickLoader loader2 = stream2.createLoader()) {
            for (int i = 0; i < n; i++) {
                AllSimpleTypesMessage message = generator.nextSimpleMessage();
                loader1.send(message);
                loader2.send(message);
            }
        }
    }

    private static void deleteIfExists(DXTickDB db, String key) {
        DXTickStream stream = db.getStream(key);
        if (stream != null) {
            stream.delete();
        }
    }
}
