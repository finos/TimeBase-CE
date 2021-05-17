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

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class AddTopicSubscriberResponse {
    private final TopicTransferType transferType;
    private final List<ConstantIdentityKey> mapping;
    private final List<RecordClassDescriptor> types;
    private final String channel;
    private final String aeronDir;
    private final int dataStreamId;

    public AddTopicSubscriberResponse(TopicTransferType transferType, List<ConstantIdentityKey> mapping, List<RecordClassDescriptor> types, String channel, @Nullable String aeronDir, int dataStreamId) {
        this.transferType = transferType;
        this.mapping = mapping;
        this.types = types;
        this.channel = channel;
        this.aeronDir = aeronDir;
        this.dataStreamId = dataStreamId;
    }

    public TopicTransferType getTransferType() {
        return transferType;
    }

    public List<ConstantIdentityKey> getMapping() {
        return mapping;
    }

    public ConstantIdentityKey[] getMappingArray() {
        return getMapping().toArray(new ConstantIdentityKey[0]);
    }

    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getChannel() {
        return channel;
    }

    @Nullable
    public String getAeronDir() {
        return aeronDir;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }
}
