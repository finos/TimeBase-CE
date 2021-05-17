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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request that stores topic attributes as key-value map to simply compatibility.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateCustomTopicRequest extends CreateTopicRequest {
    private final TopicType topicType;
    private final Map<CreateCustomTopicRequest.Field, String> attributes;

    public CreateCustomTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable Collection<? extends IdentityKey> initialEntitySet,
                                    @Nullable String targetStream, TopicType topicType, @Nonnull Map<CreateCustomTopicRequest.Field, ?> attributes) {
        super(topicKey, types, initialEntitySet, targetStream);
        this.topicType = topicType;
        this.attributes = new HashMap<>(attributes.size());
        for (Map.Entry<CreateCustomTopicRequest.Field, ?> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                this.attributes.put(entry.getKey(), value.toString());
            }
        }
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public Map<CreateCustomTopicRequest.Field, String> getAttributes() {
        return attributes;
    }

    public enum Field {
        PUBLISHER_ADDRESS
    }
}
