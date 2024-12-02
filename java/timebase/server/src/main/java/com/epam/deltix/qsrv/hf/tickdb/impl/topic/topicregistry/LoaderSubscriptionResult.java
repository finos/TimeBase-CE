/*
 * Copyright 2024 EPAM Systems, Inc
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

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class LoaderSubscriptionResult {
    private final TopicType topicType;
    private final List<RecordClassDescriptor> types;
    private final String publisherChannel;
    private final int dataStreamId;

    public LoaderSubscriptionResult(TopicType topicType, List<RecordClassDescriptor> types,
                                    String publisherChannel, int dataStreamId) {
        this.topicType = topicType;
        this.types = types;
        this.publisherChannel = publisherChannel;
        this.dataStreamId = dataStreamId;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getPublisherChannel() {
        return publisherChannel;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }
}