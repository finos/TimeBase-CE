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

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateMulticastTopicRequest extends CreateTopicRequest {
    private final String endpointHost;
    private final Integer endpointPort;

    private final String networkInterface;
    private final Integer ttl;


    public CreateMulticastTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable Collection<? extends IdentityKey> initialEntitySet,
                                       @Nullable String targetStream,
                                       @Nullable String endpointHost, @Nullable Integer endpointPort,
                                       @Nullable String networkInterface, @Nullable Integer ttl) {
        super(topicKey, types, initialEntitySet, targetStream);
        this.endpointHost = endpointHost;
        this.endpointPort = endpointPort;
        this.networkInterface = networkInterface;
        this.ttl = ttl;
    }

    @Nullable
    public String getEndpointHost() {
        return endpointHost;
    }

    @Nullable
    public Integer getEndpointPort() {
        return endpointPort;
    }

    @Nullable
    public String getNetworkInterface() {
        return networkInterface;
    }

    @Nullable
    public Integer getTtl() {
        return ttl;
    }
}