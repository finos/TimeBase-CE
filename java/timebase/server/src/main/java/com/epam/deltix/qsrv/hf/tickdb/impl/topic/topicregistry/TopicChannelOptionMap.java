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

import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicChannelOptionMap {
    private final Map<TopicChannelOption, String> valueMap;

    public TopicChannelOptionMap() {
        this.valueMap = new HashMap<>();
    }

    public TopicChannelOptionMap(Map<TopicChannelOption, String> backingMap) {
        this.valueMap = backingMap;
    }

    public void put(TopicChannelOption option, @Nullable String value) {
        if (value != null) {
            valueMap.put(option, value);
        } else {
            valueMap.remove(option);
        }
    }

    public void put(TopicChannelOption option, @Nullable Integer value) {
        if (value != null) {
            valueMap.put(option, value.toString());
        } else {
            valueMap.remove(option);
        }
    }

    public Map<TopicChannelOption, String> getValueMap() {
        return valueMap;
    }

    public boolean hasValue(TopicChannelOption option) {
        return valueMap.get(option) != null;
    }
}