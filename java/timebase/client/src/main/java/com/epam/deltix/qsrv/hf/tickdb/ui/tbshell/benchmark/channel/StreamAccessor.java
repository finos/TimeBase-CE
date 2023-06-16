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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;

/**
 * @author Alexei Osipov
 */
public class StreamAccessor implements ChannelAccessor {

    private final ChannelPerformance channelPerformance;
    private final StreamScope scope;

    public StreamAccessor(StreamScope scope, ChannelPerformance channelPerformance) {
        this.scope = scope;
        this.channelPerformance = channelPerformance;
    }

    @Override
    public void createChannel(RemoteTickDB tickDB, String channelKey, RecordClassDescriptor[] rcd) {
        StreamOptions options = new StreamOptions();
        options.scope = this.scope;
        if (rcd.length != 1) {
            options.setPolymorphic(rcd);
        } else {
            options.setFixedType(rcd[0]);
        }
        tickDB.createStream(channelKey, options);
    }

    @Override
    public void deleteChannel(RemoteTickDB tickDB, String channelKey) {
        tickDB.getStream(channelKey).delete();
    }

    @Override
    public MessageChannel<InstrumentMessage> createLoader(RemoteTickDB tickDB, String channelKey) {
        LoadingOptions options = new LoadingOptions();
        options.channelPerformance = this.channelPerformance;
        return tickDB.getStream(channelKey).createLoader(options);
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(RemoteTickDB tickDB, String channelKey) {
        SelectionOptions options = new SelectionOptions();
        options.live = true;
        TickCursor cursor = tickDB.getStream(channelKey).createCursor(options);
        cursor.subscribeToAllTypes();
        cursor.subscribeToAllEntities();
        cursor.reset(Long.MIN_VALUE);
        return cursor;
    }
}