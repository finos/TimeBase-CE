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
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;

/**
 * @author Alexei Osipov
 */
public class TopicAccessor implements ChannelAccessor {
    @Override
    public void createChannel(RemoteTickDB tickDB, String channelKey, RecordClassDescriptor[] rcd) {
        tickDB.getTopicDB().createTopic(channelKey, rcd, null);
    }

    @Override
    public void deleteChannel(RemoteTickDB tickDB, String channelKey) {
        tickDB.getTopicDB().deleteTopic(channelKey);
    }

    @Override
    public MessageChannel<InstrumentMessage> createLoader(RemoteTickDB tickDB, String channelKey) {
        return tickDB.getTopicDB().createPublisher(channelKey, null, new BusySpinIdleStrategy());
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(RemoteTickDB tickDB, String channelKey) {
        return tickDB.getTopicDB().createConsumer(channelKey, null, new BusySpinIdleStrategy());
    }
}
