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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.google.common.collect.ImmutableList;
import com.epam.deltix.qsrv.hf.blocks.InstrumentKeyToIntegerHashMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public interface TopicRegistryEventListener {
    void topicCreated(String topicKey, @Nullable String channel, ImmutableList<RecordClassDescriptor> types, InstrumentKeyToIntegerHashMap entities, TopicType topicType, Map<TopicChannelOption, String> channelOptions, @Nullable String copyToStreamKey);

    void topicDeleted(String topicKey);
}