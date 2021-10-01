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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.google.common.collect.ImmutableList;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

/**
 * @author Alexei Osipov
 */
public class ReaderSubscriptionResult {
    private final TopicType topicType;
    private final ConstantIdentityKey[] mapping;
    private final ImmutableList<RecordClassDescriptor> types;
    private final String subscriberChannel;
    private final int dataStreamId;

    public ReaderSubscriptionResult(TopicType topicType, ConstantIdentityKey[] mapping, ImmutableList<RecordClassDescriptor> types, String subscriberChannel, int dataStreamId) {
        this.topicType = topicType;
        this.mapping = mapping;
        this.types = types;
        this.subscriberChannel = subscriberChannel;
        this.dataStreamId = dataStreamId;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public ImmutableList<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getSubscriberChannel() {
        return subscriberChannel;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public ConstantIdentityKey[] getMapping() {
        return mapping;
    }
}