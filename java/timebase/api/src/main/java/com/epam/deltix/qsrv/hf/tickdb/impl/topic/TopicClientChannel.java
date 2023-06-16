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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.UnknownChannelException;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.ConsumerPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Adapter of topics to {@link DirectChannel} interface.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicClientChannel implements DirectChannel {
    private final TopicDB client;
    private final String topicKey;

    public TopicClientChannel(TopicDB client, String topicKey) {
        this.client = client;
        this.topicKey = topicKey;
    }

    @Nonnull
    @Override
    public String getKey() {
        return topicKey;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Nonnull
    @Override
    public RecordClassDescriptor[] getTypes() {
        return client.getTypes(topicKey);
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        return client.createConsumer(topicKey, ConsumerPreferences.from(options), null);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        return client.createPublisher(
                topicKey,
                PublisherPreferences.from(options),
                null
        );
    }

    @Override
    public MessagePoller createPollingConsumer(ChannelPreferences options) throws UnknownChannelException {
        return client.createPollingConsumer(topicKey, ConsumerPreferences.from(options));
    }
}